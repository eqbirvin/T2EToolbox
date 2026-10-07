package com.t2e.toolbox.eqbirvin.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.InvocationTargetException

private const val TAG = "DisplayFixRepository"

enum class ShizukuStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    NO_PERMISSION,
    READY
}

data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)
data class CurrentDisplayState(val size: String, val density: String)

class DisplayFixRepository(
    private val context: Context,
    private val settingsRepo: SettingsRepository
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.NOT_RUNNING)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        repoScope.launch {
            refreshShizukuStatus()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        repoScope.launch {
            refreshShizukuStatus()
        }
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        repoScope.launch {
            refreshShizukuStatus()
        }
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to add binder received listener sticky", e)
        }

        try {
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to add binder dead listener", e)
        }

        try {
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to add request permission result listener", e)
        }

        repoScope.launch {
            refreshShizukuStatus()
        }
    }

    private fun computeShizukuStatus(): ShizukuStatus {
        val isInstalled = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    "moe.shizuku.privileged.api",
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        if (!isInstalled) return ShizukuStatus.NOT_INSTALLED

        val isRunning = try {
            Shizuku.pingBinder()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Shizuku pingBinder failed", e)
            false
        }

        if (!isRunning) return ShizukuStatus.NOT_RUNNING

        val hasPermission = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Shizuku checkSelfPermission failed", e)
            false
        }

        if (!hasPermission) return ShizukuStatus.NO_PERMISSION

        return ShizukuStatus.READY
    }

    suspend fun refreshShizukuStatus() = withContext(Dispatchers.IO) {
        val status = computeShizukuStatus()
        _shizukuStatus.value = status
    }

    fun isShizukuReady(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: IllegalStateException) {
            Log.e(TAG, "isShizukuReady failed with IllegalStateException", e)
            false
        }
    }

    suspend fun requestShizukuPermission(requestCode: Int = 0) = withContext(Dispatchers.IO) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while requesting Shizuku permission", e)
        }
    }

    suspend fun waitForShizuku(timeoutMs: Long = 90000): Boolean = withContext(Dispatchers.IO) {
        if (isShizukuReady()) {
            refreshShizukuStatus()
            return@withContext true
        }
        val deferred = CompletableDeferred<Boolean>()
        val listener = object : Shizuku.OnBinderReceivedListener {
            override fun onBinderReceived() {
                deferred.complete(true)
            }
        }
        try {
            Shizuku.addBinderReceivedListenerSticky(listener)
            withTimeoutOrNull(timeoutMs) {
                deferred.await()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to register temporary binder received listener during wait", e)
        } finally {
            try {
                Shizuku.removeBinderReceivedListener(listener)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to remove temporary binder received listener during wait", e)
            }
        }
        refreshShizukuStatus()
        return@withContext isShizukuReady()
    }

    private suspend fun runShell(command: String): ShellResult = ShizukuShell.runShell(command)

    private fun parseOverride(output: ShellResult): String? {
        if (output.exitCode != 0) {
            throw IllegalStateException("Command failed with exit code ${output.exitCode}: ${output.stderr}")
        }
        val lines = output.stdout.lines()
        val overrideLine = lines.find { it.startsWith("Override") }
        return overrideLine?.substringAfter(":")?.trim()
    }

    private fun parseCurrentOrPhysical(output: ShellResult): String {
        val override = try {
            parseOverride(output)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to parse override", e)
            null
        }
        if (override != null) return override
        val lines = output.stdout.lines()
        val physicalLine = lines.find { it.startsWith("Physical") }
        return physicalLine?.substringAfter(":")?.trim() ?: "Unknown"
    }

    suspend fun getCurrentDisplayState(): CurrentDisplayState? = withContext(Dispatchers.IO) {
        if (!isShizukuReady()) return@withContext null
        val sizeOut = runShell("wm size")
        val densityOut = runShell("wm density")
        if (sizeOut.exitCode != 0 || densityOut.exitCode != 0) {
            Log.e(TAG, "Failed to query current display state: size code=${sizeOut.exitCode}, density code=${densityOut.exitCode}")
            return@withContext null
        }
        CurrentDisplayState(parseCurrentOrPhysical(sizeOut), parseCurrentOrPhysical(densityOut))
    }

    suspend fun enableFix(trigger: String): Boolean = withContext(Dispatchers.IO) {
        if (!isShizukuReady()) {
            settingsRepo.saveLastRunRecord(LastRunRecord(System.currentTimeMillis(), trigger, false, "Shizuku not ready or permission denied"))
            return@withContext false
        }
        try {
            if (settingsRepo.displaySnapshot.value == null) {
                val currentSize = runShell("wm size")
                val currentDensity = runShell("wm density")
                val sizeOverride = parseOverride(currentSize)
                val densityOverride = parseOverride(currentDensity)
                settingsRepo.saveDisplaySnapshot(DisplaySnapshot(sizeOverride, densityOverride))
            }

            val sizeResult = runShell("wm size 864x960")
            val densityResult = runShell("wm density 231")
            val success = sizeResult.exitCode == 0 && densityResult.exitCode == 0
            val error = if (!success) "Size: ${sizeResult.stderr} | Density: ${densityResult.stderr}" else null

            settingsRepo.saveLastRunRecord(LastRunRecord(System.currentTimeMillis(), trigger, success, error))
            success
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Illegal state while enabling display fix", e)
            settingsRepo.saveLastRunRecord(LastRunRecord(System.currentTimeMillis(), trigger, false, "Parse/State error: ${e.message.orEmpty()}"))
            false
        }
    }

    suspend fun disableFix(trigger: String): Boolean = withContext(Dispatchers.IO) {
        if (!isShizukuReady()) {
            settingsRepo.saveLastRunRecord(LastRunRecord(System.currentTimeMillis(), trigger, false, "Shizuku not ready or permission denied"))
            return@withContext false
        }

        val snapshot = settingsRepo.displaySnapshot.value
        if (snapshot == null) return@withContext true

        val sizeCmd = if (snapshot.sizeOverride != null) "wm size ${ShizukuShell.shellQuote(snapshot.sizeOverride)}" else "wm size reset"
        val densityCmd = if (snapshot.densityOverride != null) "wm density ${ShizukuShell.shellQuote(snapshot.densityOverride)}" else "wm density reset"

        val sizeResult = runShell(sizeCmd)
        val densityResult = runShell(densityCmd)
        val success = sizeResult.exitCode == 0 && densityResult.exitCode == 0
        if (success) {
            settingsRepo.saveDisplaySnapshot(null)
        }
        val error = if (!success) "Size: ${sizeResult.stderr} | Density: ${densityResult.stderr}" else null
        settingsRepo.saveLastRunRecord(LastRunRecord(System.currentTimeMillis(), trigger, success, error))
        success
    }
}

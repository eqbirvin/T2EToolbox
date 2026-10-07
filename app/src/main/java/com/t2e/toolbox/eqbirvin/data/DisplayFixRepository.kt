package com.t2e.toolbox.eqbirvin.data

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)
data class CurrentDisplayState(val size: String, val density: String)

class DisplayFixRepository(private val settingsRepo: SettingsRepository) {

    fun isShizukuReady(): Boolean {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    suspend fun waitForShizuku(timeoutMs: Long = 90000): Boolean = withContext(Dispatchers.IO) {
        if (Shizuku.pingBinder()) return@withContext true
        var bound = false
        val listener = Shizuku.OnBinderReceivedListener { bound = true }
        Shizuku.addBinderReceivedListener(listener)
        val start = System.currentTimeMillis()
        while (!bound && System.currentTimeMillis() - start < timeoutMs) {
            delay(1000)
        }
        Shizuku.removeBinderReceivedListener(listener)
        return@withContext bound || Shizuku.pingBinder()
    }

    private suspend fun runShell(command: String): ShellResult = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
            process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()
            ShellResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            ShellResult(-1, "", e.message ?: "Unknown error")
        } finally {
            process?.destroy()
        }
    }

    private fun parseOverride(output: ShellResult): String? {
        if (output.exitCode != 0) throw Exception("Failed to get value: ${output.stderr}")
        val lines = output.stdout.lines()
        val overrideLine = lines.find { it.startsWith("Override") }
        return overrideLine?.substringAfter(":")?.trim()
    }

    private fun parseCurrentOrPhysical(output: ShellResult): String {
        val override = parseOverride(output)
        if (override != null) return override
        val lines = output.stdout.lines()
        val physicalLine = lines.find { it.startsWith("Physical") }
        return physicalLine?.substringAfter(":")?.trim() ?: "Unknown"
    }

    suspend fun getCurrentDisplayState(): CurrentDisplayState? {
        if (!isShizukuReady()) return null
        return try {
            val sizeOut = runShell("wm size")
            val densityOut = runShell("wm density")
            CurrentDisplayState(parseCurrentOrPhysical(sizeOut), parseCurrentOrPhysical(densityOut))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun enableFix(trigger: String): Boolean {
        if (!isShizukuReady()) {
            settingsRepo.saveLastRunRecord(LastRunRecord(System.currentTimeMillis(), trigger, false, "Shizuku not ready or permission denied"))
            return false
        }
        return try {
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
        } catch (e: Exception) {
            settingsRepo.saveLastRunRecord(LastRunRecord(System.currentTimeMillis(), trigger, false, "Parse error: ${e.message}"))
            false
        }
    }

    suspend fun disableFix(trigger: String): Boolean {
        if (!isShizukuReady()) {
            settingsRepo.saveLastRunRecord(LastRunRecord(System.currentTimeMillis(), trigger, false, "Shizuku not ready or permission denied"))
            return false
        }
        
        val snapshot = settingsRepo.displaySnapshot.value
        // If there's no snapshot, we shouldn't attempt to disable and risk un-restorable state.
        if (snapshot == null) return true 

        val sizeCmd = if (snapshot.sizeOverride != null) "wm size ${snapshot.sizeOverride}" else "wm size reset"
        val densityCmd = if (snapshot.densityOverride != null) "wm density ${snapshot.densityOverride}" else "wm density reset"

        val sizeResult = runShell(sizeCmd)
        val densityResult = runShell(densityCmd)
        val success = sizeResult.exitCode == 0 && densityResult.exitCode == 0
        if (success) {
            settingsRepo.saveDisplaySnapshot(null)
        }
        val error = if (!success) "Size: ${sizeResult.stderr} | Density: ${densityResult.stderr}" else null
        settingsRepo.saveLastRunRecord(LastRunRecord(System.currentTimeMillis(), trigger, success, error))
        return success
    }
}

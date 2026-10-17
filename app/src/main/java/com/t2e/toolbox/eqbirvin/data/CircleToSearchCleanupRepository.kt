package com.t2e.toolbox.eqbirvin.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "CtsCleanupRepo"

data class CtsStorageInfo(
    val fileCount: Int = 0,
    val totalBytes: Long = 0L
) {
    val formattedSize: String
        get() {
            if (totalBytes <= 0) return "0 B"
            val kb = totalBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.2f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$totalBytes B"
            }
        }
}

data class CtsCleanupResult(
    val success: Boolean,
    val deletedCount: Int,
    val freedBytes: Long,
    val errorMessage: String? = null
)

class CircleToSearchCleanupRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cleanupMutex = Mutex()
    private var scheduledCleanupJob: Job? = null

    private val _storageInfo = MutableStateFlow(CtsStorageInfo())
    val storageInfo: StateFlow<CtsStorageInfo> = _storageInfo.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    companion object {
        const val CTS_SETTINGS_ACTIVE = "agui_circle_search_active"
        const val CTS_SETTINGS_FINISHING = "agui_circle_search_finishing"
        const val CTS_FOLDER_PATH = "/sdcard/Pictures/AguiCircleSearch"
        const val CTS_FILE_PATTERN = "agui_circle_search"
    }

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            handleSettingsChange()
        }
    }

    private var isObserverRegistered = false

    fun startObserving() {
        if (isObserverRegistered) return
        try {
            val finishingUri = Settings.Global.getUriFor(CTS_SETTINGS_FINISHING)
            if (finishingUri != null) {
                context.contentResolver.registerContentObserver(finishingUri, false, contentObserver)
                isObserverRegistered = true
                Log.d(TAG, "Registered ContentObserver for $CTS_SETTINGS_FINISHING")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering CTS ContentObserver", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException registering CTS ContentObserver", e)
        }
        refreshStorageInfo()
    }

    fun stopObserving() {
        if (!isObserverRegistered) return
        try {
            context.contentResolver.unregisterContentObserver(contentObserver)
            isObserverRegistered = false
            Log.d(TAG, "Unregistered ContentObserver for CTS")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException unregistering CTS ContentObserver", e)
        }
    }

    private fun handleSettingsChange() {
        scope.launch {
            if (!settingsRepository.isCtsAutoCleanupEnabled.value) return@launch
            try {
                val finishingVal = Settings.Global.getInt(context.contentResolver, CTS_SETTINGS_FINISHING, 0)
                Log.d(TAG, "Observed CTS finishing setting value: $finishingVal")
                if (finishingVal == 1) {
                    scheduleAutoCleanup("Settings.Global finishing")
                }
            } catch (e: Settings.SettingNotFoundException) {
                Log.e(TAG, "CTS finishing setting not found", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException reading CTS finishing setting", e)
            }
        }
    }

    fun onLensActivityDetected() {
        if (!settingsRepository.isCtsAutoCleanupEnabled.value) return
        scheduleAutoCleanup("Lens activity window transition")
    }

    fun scheduleAutoCleanup(triggerSource: String) {
        scope.launch {
            val delaySec = settingsRepository.ctsCleanupDelaySeconds.value
            cleanupMutex.withLock {
                scheduledCleanupJob?.cancel()
                scheduledCleanupJob = scope.launch {
                    Log.d(TAG, "Scheduled CTS cleanup in ${delaySec}s (trigger: $triggerSource)")
                    delay(delaySec * 1000L)
                    val result = executeCleanup()
                    Log.d(TAG, "Auto-cleanup finished: deleted=${result.deletedCount}, freed=${result.freedBytes} bytes")
                }
            }
        }
    }

    fun refreshStorageInfo() {
        scope.launch {
            val info = scanStorage()
            _storageInfo.value = info
        }
    }

    suspend fun scanStorage(): CtsStorageInfo = withContext(Dispatchers.IO) {
        // Try direct file access first if readable
        var count = 0
        var totalBytes = 0L

        try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val ctsDir = File(picturesDir, "AguiCircleSearch")
            if (ctsDir.exists() && ctsDir.isDirectory) {
                val files = ctsDir.listFiles()
                if (files != null && files.isNotEmpty()) {
                    for (file in files) {
                        if (file.name.contains(CTS_FILE_PATTERN) || file.name.endsWith(".png")) {
                            count++
                            totalBytes += file.length()
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading CTS directory directly", e)
        }

        // If direct file access found nothing or Shizuku is ready, query via Shizuku shell for ground truth
        if (ShizukuShell.isReady()) {
            try {
                val cmd = "ls -la $CTS_FOLDER_PATH"
                val res = ShizukuShell.runShell(cmd)
                if (res.exitCode == 0 && res.stdout.isNotBlank()) {
                    var shizukuCount = 0
                    var shizukuBytes = 0L
                    for (line in res.stdout.lines()) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("total")) continue
                        if (trimmed.contains(CTS_FILE_PATTERN) || trimmed.endsWith(".png")) {
                            val parts = trimmed.split("\\s+".toRegex())
                            if (parts.size >= 5) {
                                val size = parts[4].toLongOrNull() ?: 0L
                                shizukuBytes += size
                                shizukuCount++
                            }
                        }
                    }
                    if (shizukuCount > 0) {
                        count = shizukuCount
                        totalBytes = shizukuBytes
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying CTS files via Shizuku", e)
            }
        }

        CtsStorageInfo(count, totalBytes)
    }

    suspend fun cleanNow(): CtsCleanupResult = withContext(Dispatchers.IO) {
        _isCleaning.value = true
        try {
            val result = executeCleanup()
            result
        } finally {
            _isCleaning.value = false
        }
    }

    private suspend fun executeCleanup(): CtsCleanupResult = withContext(Dispatchers.IO) {
        val initialInfo = scanStorage()
        if (initialInfo.fileCount == 0 && initialInfo.totalBytes == 0L) {
            refreshStorageInfo()
            return@withContext CtsCleanupResult(success = true, deletedCount = 0, freedBytes = 0L)
        }

        var deletedCount = 0
        var freedBytes = 0L

        // Attempt 1: File API
        try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val ctsDir = File(picturesDir, "AguiCircleSearch")
            if (ctsDir.exists() && ctsDir.isDirectory) {
                val files = ctsDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.name.contains(CTS_FILE_PATTERN) || file.name.endsWith(".png")) {
                            val len = file.length()
                            if (file.delete()) {
                                deletedCount++
                                freedBytes += len
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException deleting CTS files via File API", e)
        }

        // Attempt 2: Privileged Shizuku deletion if files remain
        if (ShizukuShell.isReady()) {
            try {
                // Delete files matching pattern including trashed files
                val deleteCmd = "rm -f $CTS_FOLDER_PATH/*$CTS_FILE_PATTERN* $CTS_FOLDER_PATH/.*$CTS_FILE_PATTERN* $CTS_FOLDER_PATH/*.png $CTS_FOLDER_PATH/.*.png"
                ShizukuShell.runShell(deleteCmd)

                // Clean MediaStore database entries to remove orphaned gallery thumbnails
                val mediaStoreCmd = "content delete --uri content://media/external/images/media --where \"_data LIKE '%AguiCircleSearch%'\""
                ShizukuShell.runShell(mediaStoreCmd)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing CTS deletion via Shizuku", e)
            }
        }

        val afterInfo = scanStorage()
        val actualDeleted = (initialInfo.fileCount - afterInfo.fileCount).coerceAtLeast(deletedCount)
        val actualFreed = (initialInfo.totalBytes - afterInfo.totalBytes).coerceAtLeast(freedBytes)

        _storageInfo.value = afterInfo
        settingsRepository.setLastCtsCleaned(System.currentTimeMillis(), actualDeleted)

        CtsCleanupResult(
            success = true,
            deletedCount = actualDeleted,
            freedBytes = actualFreed
        )
    }
}

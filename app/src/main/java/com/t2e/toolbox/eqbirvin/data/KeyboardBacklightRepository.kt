package com.t2e.toolbox.eqbirvin.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "KeyboardBacklightRepo"

class KeyboardBacklightRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val SERVICE = "agui_functional_service"
        const val KEY_TIMEOUT = "keyboard_brightness_timeout"
        const val KEY_BRIGHTNESS = "keyboard_led_brightness"
        
        /** When keeping on with the screen, set vendor timeout to 24 hours so it never times out. */
        const val KEEP_ON_TIMEOUT_MS = 24 * 60 * 60 * 1000
        
        const val CLOSE_LIGHT_CMD = "am broadcast -a agui.action.CLOSE_KEYBOARD_LIGHT"
        const val WAKE_LIGHT_CMD = "input keyevent 0"

        fun parseParcelString(rawOutput: String): String? {
            return try {
                val linesWithQuotes = rawOutput.lines().filter { "'" in it }
                if (linesWithQuotes.isEmpty()) return null
                val joined = linesWithQuotes.joinToString("") { line ->
                    line.substringAfter("'").substringBefore("'")
                }
                joined.replace(".", "").replace(" ", "").trim().takeIf { it.isNotEmpty() }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing parcel string", e)
                null
            }
        }
    }

    suspend fun readCurrentTimeoutSeconds(): Int? = withContext(Dispatchers.IO) {
        if (!ShizukuShell.isReady()) return@withContext null
        val result = ShizukuShell.runShell("service call $SERVICE 1 s16 $KEY_TIMEOUT")
        if (result.isSuccess) {
            val parsed = parseParcelString(result.stdout)
            parsed?.toIntOrNull()?.let { it / 1000 }
        } else {
            null
        }
    }

    suspend fun readCurrentBrightness(): Int? = withContext(Dispatchers.IO) {
        if (!ShizukuShell.isReady()) return@withContext null
        val result = ShizukuShell.runShell("service call $SERVICE 1 s16 $KEY_BRIGHTNESS")
        if (result.isSuccess) {
            val parsed = parseParcelString(result.stdout)
            parsed?.toIntOrNull()
        } else {
            null
        }
    }

    suspend fun applyTimeout(timeoutSeconds: Int, keepOnWithScreen: Boolean): ShellResult = withContext(Dispatchers.IO) {
        if (!ShizukuShell.isReady()) {
            return@withContext ShellResult(-1, "", "Shizuku is not running or permission not granted")
        }
        val timeoutMs = if (keepOnWithScreen) {
            KEEP_ON_TIMEOUT_MS
        } else {
            timeoutSeconds.coerceIn(5, 300) * 1000
        }
        val cmd = "service call $SERVICE 2 s16 $KEY_TIMEOUT s16 $timeoutMs"
        val result = ShizukuShell.runShell(cmd)
        if (result.isSuccess) {
            Log.d(TAG, "Successfully set keyboard backlight timeout to ${timeoutMs}ms (keepOnWithScreen=$keepOnWithScreen)")
        } else {
            Log.e(TAG, "Failed to set keyboard backlight timeout: ${result.stderr}")
        }
        result
    }

    suspend fun applyBrightness(level: Int): ShellResult = withContext(Dispatchers.IO) {
        if (!ShizukuShell.isReady()) {
            return@withContext ShellResult(-1, "", "Shizuku is not running or permission not granted")
        }
        val clampedLevel = level.coerceIn(0, 100)
        val cmd = "service call $SERVICE 2 s16 $KEY_BRIGHTNESS s16 $clampedLevel"
        val result = ShizukuShell.runShell(cmd)
        if (result.isSuccess) {
            Log.d(TAG, "Successfully set keyboard backlight brightness to $clampedLevel%")
        } else {
            Log.e(TAG, "Failed to set keyboard backlight brightness: ${result.stderr}")
        }
        result
    }

    suspend fun applyAllSettings(): ShellResult = withContext(Dispatchers.IO) {
        if (!ShizukuShell.isReady()) {
            return@withContext ShellResult(-1, "", "Shizuku is not running or permission not granted")
        }
        val timeoutSec = settingsRepository.keyboardBacklightTimeoutSeconds.value
        val keepOn = settingsRepository.keyboardBacklightKeepOnWithScreen.value
        val brightness = settingsRepository.keyboardBacklightBrightness.value

        val timeoutRes = applyTimeout(timeoutSec, keepOn)
        if (!timeoutRes.isSuccess) return@withContext timeoutRes

        val brightnessRes = applyBrightness(brightness)
        brightnessRes
    }

    fun onScreenOn() {
        if (!settingsRepository.isKeyboardBacklightCustomEnabled.value) return
        if (!settingsRepository.keyboardBacklightKeepOnWithScreen.value) return
        scope.launch {
            if (!ShizukuShell.isReady()) return@launch
            // Wake up keyboard light without typing characters
            ShizukuShell.runShell(WAKE_LIGHT_CMD)
            // Ensure timeout is maintained
            applyTimeout(settingsRepository.keyboardBacklightTimeoutSeconds.value, true)
            // Ensure brightness is applied
            applyBrightness(settingsRepository.keyboardBacklightBrightness.value)
        }
    }

    fun onScreenOff() {
        if (!settingsRepository.isKeyboardBacklightCustomEnabled.value) return
        if (!settingsRepository.keyboardBacklightKeepOnWithScreen.value) return
        scope.launch {
            if (!ShizukuShell.isReady()) return@launch
            // Instantly extinguish keyboard backlight when screen goes off
            ShizukuShell.runShell(CLOSE_LIGHT_CMD)
        }
    }
}

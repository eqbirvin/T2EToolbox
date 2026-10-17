package com.t2e.toolbox.eqbirvin.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "SettingsRepository"

@Serializable
data class DisplaySnapshot(
    val sizeOverride: String?,
    val densityOverride: String?
)

@Serializable
data class LastRunRecord(
    val timestamp: Long,
    val trigger: String, // "Manual" or "Boot"
    val success: Boolean,
    val errorMsg: String?
)

@Serializable
enum class PullDownMethod {
    ACCESSIBILITY,
    SHIZUKU
}

@Serializable
data class AccessibilitySettingsSnapshot(
    val previousEnabledServices: String?,
    val previousAccessibilityEnabled: String?
)

@Serializable
enum class VolumeCycleOption {
    ALL_MODES,
    SOUND_AND_VIBRATE,
    SOUND_AND_SILENT,
    VIBRATE_AND_SILENT
}

@Serializable
enum class SilentModeBehavior(val label: String) {
    MUTE("Silent (Mute only)"),
    DND("Do Not Disturb (DND)")
}

@Serializable
enum class FlashlightIndicatorColor(val colorArgb: Long, val label: String) {
    AMBER(0xFFFFB300, "Amber Glow"),
    YELLOW(0xFFFFEB3B, "Bright Yellow"),
    WHITE(0xFFFFFFFF, "Crisp White")
}

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("t2e_settings", Context.MODE_PRIVATE)

    // Display Fix
    private val _isDisplayFixEnabled = MutableStateFlow(prefs.getBoolean("display_fix_enabled", false))
    val isDisplayFixEnabled: StateFlow<Boolean> = _isDisplayFixEnabled.asStateFlow()

    private val _lastRunRecord = MutableStateFlow<LastRunRecord?>(
        prefs.getString("last_run_record", null)?.let {
            try {
                Json.decodeFromString<LastRunRecord>(it)
            } catch (e: SerializationException) {
                Log.e(TAG, "Failed to decode last_run_record JSON", e)
                null
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Illegal argument while decoding last_run_record JSON", e)
                null
            }
        }
    )
    val lastRunRecord: StateFlow<LastRunRecord?> = _lastRunRecord.asStateFlow()

    private val _displaySnapshot = MutableStateFlow<DisplaySnapshot?>(
        prefs.getString("display_snapshot", null)?.let {
            try {
                Json.decodeFromString<DisplaySnapshot>(it)
            } catch (e: SerializationException) {
                Log.e(TAG, "Failed to decode display_snapshot JSON", e)
                null
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Illegal argument while decoding display_snapshot JSON", e)
                null
            }
        }
    )
    val displaySnapshot: StateFlow<DisplaySnapshot?> = _displaySnapshot.asStateFlow()

    // Unified Pull-Down
    private val _isPullDownEnabled = MutableStateFlow(prefs.getBoolean("pulldown_enabled", false))
    val isPullDownEnabled: StateFlow<Boolean> = _isPullDownEnabled.asStateFlow()

    private val _isPullDownOnLockscreen = MutableStateFlow(prefs.getBoolean("pulldown_on_lockscreen", true))
    val isPullDownOnLockscreen: StateFlow<Boolean> = _isPullDownOnLockscreen.asStateFlow()

    private val _isPullDownDebugTint = MutableStateFlow(prefs.getBoolean("pulldown_debug_tint", false))
    val isPullDownDebugTint: StateFlow<Boolean> = _isPullDownDebugTint.asStateFlow()

    private val _isSmartShadeDetectionEnabled = MutableStateFlow(prefs.getBoolean("pulldown_smart_shade_detection", true))
    val isSmartShadeDetectionEnabled: StateFlow<Boolean> = _isSmartShadeDetectionEnabled.asStateFlow()

    private val _isPullDownHapticFeedbackEnabled = MutableStateFlow(prefs.getBoolean("pulldown_haptic_feedback", true))
    val isPullDownHapticFeedbackEnabled: StateFlow<Boolean> = _isPullDownHapticFeedbackEnabled.asStateFlow()

    private val _pullDownMethod = MutableStateFlow(
        prefs.getString("pulldown_method", null)?.let {
            try {
                PullDownMethod.valueOf(it)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Illegal argument while decoding pulldown_method", e)
                PullDownMethod.ACCESSIBILITY
            }
        } ?: PullDownMethod.ACCESSIBILITY
    )
    val pullDownMethod: StateFlow<PullDownMethod> = _pullDownMethod.asStateFlow()

    private val _accessibilitySnapshot = MutableStateFlow<AccessibilitySettingsSnapshot?>(
        prefs.getString("pulldown_accessibility_snapshot", null)?.let {
            try {
                Json.decodeFromString<AccessibilitySettingsSnapshot>(it)
            } catch (e: SerializationException) {
                Log.e(TAG, "Failed to decode pulldown_accessibility_snapshot JSON", e)
                null
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Illegal argument while decoding pulldown_accessibility_snapshot JSON", e)
                null
            }
        }
    )
    val accessibilitySnapshot: StateFlow<AccessibilitySettingsSnapshot?> = _accessibilitySnapshot.asStateFlow()

    // Volume Mode Switcher
    private val _isVolumeModeShortcutEnabled = MutableStateFlow(prefs.getBoolean("volume_mode_shortcut_enabled", false))
    val isVolumeModeShortcutEnabled: StateFlow<Boolean> = _isVolumeModeShortcutEnabled.asStateFlow()

    private val _isVolumeModeToastEnabled = MutableStateFlow(prefs.getBoolean("volume_mode_toast_enabled", true))
    val isVolumeModeToastEnabled: StateFlow<Boolean> = _isVolumeModeToastEnabled.asStateFlow()

    private val _isVolumeModeHapticEnabled = MutableStateFlow(prefs.getBoolean("volume_mode_haptic_enabled", true))
    val isVolumeModeHapticEnabled: StateFlow<Boolean> = _isVolumeModeHapticEnabled.asStateFlow()

    private val _volumeModeCycleOption = MutableStateFlow(
        prefs.getString("volume_mode_cycle_option", null)?.let {
            try {
                VolumeCycleOption.valueOf(it)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Illegal argument while decoding volume_mode_cycle_option", e)
                VolumeCycleOption.ALL_MODES
            }
        } ?: VolumeCycleOption.ALL_MODES
    )
    val volumeModeCycleOption: StateFlow<VolumeCycleOption> = _volumeModeCycleOption.asStateFlow()

    private val _volumeModeSilentBehavior = MutableStateFlow(
        prefs.getString("volume_mode_silent_behavior", null)?.let {
            try {
                SilentModeBehavior.valueOf(it)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Illegal argument while decoding volume_mode_silent_behavior", e)
                SilentModeBehavior.MUTE
            }
        } ?: SilentModeBehavior.MUTE
    )
    val volumeModeSilentBehavior: StateFlow<SilentModeBehavior> = _volumeModeSilentBehavior.asStateFlow()

    // Flashlight Status Bar Indicator
    private val _isFlashlightIndicatorEnabled = MutableStateFlow(prefs.getBoolean("flashlight_indicator_enabled", true))
    val isFlashlightIndicatorEnabled: StateFlow<Boolean> = _isFlashlightIndicatorEnabled.asStateFlow()

    private val _isFlashlightIndicatorTapToDismiss = MutableStateFlow(prefs.getBoolean("flashlight_indicator_tap_dismiss", false))
    val isFlashlightIndicatorTapToDismiss: StateFlow<Boolean> = _isFlashlightIndicatorTapToDismiss.asStateFlow()

    private val _flashlightIndicatorColor = MutableStateFlow(
        prefs.getString("flashlight_indicator_color", null)?.let {
            try {
                FlashlightIndicatorColor.valueOf(it)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Illegal argument while decoding flashlight_indicator_color", e)
                FlashlightIndicatorColor.AMBER
            }
        } ?: FlashlightIndicatorColor.AMBER
    )
    val flashlightIndicatorColor: StateFlow<FlashlightIndicatorColor> = _flashlightIndicatorColor.asStateFlow()

    // Per-App DPI Scaling
    private val _isPerAppDpiEnabled = MutableStateFlow(prefs.getBoolean("per_app_dpi_enabled", false))
    val isPerAppDpiEnabled: StateFlow<Boolean> = _isPerAppDpiEnabled.asStateFlow()

    private val _perAppDpiRules = MutableStateFlow<Map<String, PerAppDpiRule>>(
        prefs.getString("per_app_dpi_rules", null)?.let {
            try {
                Json.decodeFromString<Map<String, PerAppDpiRule>>(it)
            } catch (e: SerializationException) {
                Log.e(TAG, "Failed to decode per_app_dpi_rules JSON", e)
                emptyMap()
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Illegal argument while decoding per_app_dpi_rules JSON", e)
                emptyMap()
            }
        } ?: emptyMap()
    )
    val perAppDpiRules: StateFlow<Map<String, PerAppDpiRule>> = _perAppDpiRules.asStateFlow()

    private val _perAppDpiBaseDpi = MutableStateFlow<Int?>(
        if (prefs.contains("per_app_dpi_base_dpi")) prefs.getInt("per_app_dpi_base_dpi", 300) else null
    )
    val perAppDpiBaseDpi: StateFlow<Int?> = _perAppDpiBaseDpi.asStateFlow()

    // Keyboard Backlight
    private val _isKeyboardBacklightCustomEnabled = MutableStateFlow(prefs.getBoolean("keyboard_backlight_custom_enabled", false))
    val isKeyboardBacklightCustomEnabled: StateFlow<Boolean> = _isKeyboardBacklightCustomEnabled.asStateFlow()

    private val _keyboardBacklightTimeoutSeconds = MutableStateFlow(prefs.getInt("keyboard_backlight_timeout_seconds", 60))
    val keyboardBacklightTimeoutSeconds: StateFlow<Int> = _keyboardBacklightTimeoutSeconds.asStateFlow()

    private val _keyboardBacklightKeepOnWithScreen = MutableStateFlow(prefs.getBoolean("keyboard_backlight_keep_on_with_screen", false))
    val keyboardBacklightKeepOnWithScreen: StateFlow<Boolean> = _keyboardBacklightKeepOnWithScreen.asStateFlow()

    private val _keyboardBacklightBrightness = MutableStateFlow(prefs.getInt("keyboard_backlight_brightness", 30))
    val keyboardBacklightBrightness: StateFlow<Int> = _keyboardBacklightBrightness.asStateFlow()

    // Circle to Search Cleanup
    private val _isCtsAutoCleanupEnabled = MutableStateFlow(prefs.getBoolean("cts_auto_cleanup_enabled", true))
    val isCtsAutoCleanupEnabled: StateFlow<Boolean> = _isCtsAutoCleanupEnabled.asStateFlow()

    private val _ctsCleanupDelaySeconds = MutableStateFlow(prefs.getInt("cts_cleanup_delay_seconds", 4))
    val ctsCleanupDelaySeconds: StateFlow<Int> = _ctsCleanupDelaySeconds.asStateFlow()

    private val _lastCtsCleanedTimestamp = MutableStateFlow(prefs.getLong("last_cts_cleaned_timestamp", 0L))
    val lastCtsCleanedTimestamp: StateFlow<Long> = _lastCtsCleanedTimestamp.asStateFlow()

    private val _lastCtsCleanedCount = MutableStateFlow(prefs.getInt("last_cts_cleaned_count", 0))
    val lastCtsCleanedCount: StateFlow<Int> = _lastCtsCleanedCount.asStateFlow()

    suspend fun setDisplayFixEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("display_fix_enabled", enabled).apply()
        _isDisplayFixEnabled.value = enabled
    }

    suspend fun saveLastRunRecord(record: LastRunRecord) = withContext(Dispatchers.IO) {
        prefs.edit().putString("last_run_record", Json.encodeToString(record)).apply()
        _lastRunRecord.value = record
    }

    suspend fun saveDisplaySnapshot(snapshot: DisplaySnapshot?) = withContext(Dispatchers.IO) {
        if (snapshot == null) {
            prefs.edit().remove("display_snapshot").apply()
        } else {
            prefs.edit().putString("display_snapshot", Json.encodeToString(snapshot)).apply()
        }
        _displaySnapshot.value = snapshot
    }

    suspend fun setPullDownEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("pulldown_enabled", enabled).apply()
        _isPullDownEnabled.value = enabled
    }

    suspend fun setPullDownOnLockscreen(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("pulldown_on_lockscreen", enabled).apply()
        _isPullDownOnLockscreen.value = enabled
    }

    suspend fun setPullDownDebugTint(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("pulldown_debug_tint", enabled).apply()
        _isPullDownDebugTint.value = enabled
    }

    suspend fun setSmartShadeDetectionEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("pulldown_smart_shade_detection", enabled).apply()
        _isSmartShadeDetectionEnabled.value = enabled
    }

    suspend fun setPullDownHapticFeedbackEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("pulldown_haptic_feedback", enabled).apply()
        _isPullDownHapticFeedbackEnabled.value = enabled
    }

    suspend fun setPullDownMethod(method: PullDownMethod) = withContext(Dispatchers.IO) {
        prefs.edit().putString("pulldown_method", method.name).apply()
        _pullDownMethod.value = method
    }

    suspend fun saveAccessibilitySnapshot(snapshot: AccessibilitySettingsSnapshot?) = withContext(Dispatchers.IO) {
        if (snapshot == null) {
            prefs.edit().remove("pulldown_accessibility_snapshot").apply()
        } else {
            prefs.edit().putString("pulldown_accessibility_snapshot", Json.encodeToString(snapshot)).apply()
        }
        _accessibilitySnapshot.value = snapshot
    }

    suspend fun setVolumeModeShortcutEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("volume_mode_shortcut_enabled", enabled).apply()
        _isVolumeModeShortcutEnabled.value = enabled
    }

    suspend fun setVolumeModeToastEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("volume_mode_toast_enabled", enabled).apply()
        _isVolumeModeToastEnabled.value = enabled
    }

    suspend fun setVolumeModeHapticEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("volume_mode_haptic_enabled", enabled).apply()
        _isVolumeModeHapticEnabled.value = enabled
    }

    suspend fun setVolumeModeCycleOption(option: VolumeCycleOption) = withContext(Dispatchers.IO) {
        prefs.edit().putString("volume_mode_cycle_option", option.name).apply()
        _volumeModeCycleOption.value = option
    }

    suspend fun setVolumeModeSilentBehavior(behavior: SilentModeBehavior) = withContext(Dispatchers.IO) {
        prefs.edit().putString("volume_mode_silent_behavior", behavior.name).apply()
        _volumeModeSilentBehavior.value = behavior
    }

    suspend fun setFlashlightIndicatorEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("flashlight_indicator_enabled", enabled).apply()
        _isFlashlightIndicatorEnabled.value = enabled
    }

    suspend fun setFlashlightIndicatorTapToDismiss(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("flashlight_indicator_tap_dismiss", enabled).apply()
        _isFlashlightIndicatorTapToDismiss.value = enabled
    }

    suspend fun setFlashlightIndicatorColor(color: FlashlightIndicatorColor) = withContext(Dispatchers.IO) {
        prefs.edit().putString("flashlight_indicator_color", color.name).apply()
        _flashlightIndicatorColor.value = color
    }

    suspend fun setPerAppDpiEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("per_app_dpi_enabled", enabled).apply()
        _isPerAppDpiEnabled.value = enabled
    }

    suspend fun savePerAppDpiRules(rules: Map<String, PerAppDpiRule>) = withContext(Dispatchers.IO) {
        prefs.edit().putString("per_app_dpi_rules", Json.encodeToString(rules)).apply()
        _perAppDpiRules.value = rules
    }

    suspend fun setPerAppDpiBaseDpi(baseDpi: Int?) = withContext(Dispatchers.IO) {
        if (baseDpi == null) {
            prefs.edit().remove("per_app_dpi_base_dpi").apply()
        } else {
            prefs.edit().putInt("per_app_dpi_base_dpi", baseDpi).apply()
        }
        _perAppDpiBaseDpi.value = baseDpi
    }

    suspend fun setKeyboardBacklightCustomEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("keyboard_backlight_custom_enabled", enabled).apply()
        _isKeyboardBacklightCustomEnabled.value = enabled
    }

    suspend fun setKeyboardBacklightTimeoutSeconds(seconds: Int) = withContext(Dispatchers.IO) {
        val clamped = seconds.coerceIn(5, 300)
        prefs.edit().putInt("keyboard_backlight_timeout_seconds", clamped).apply()
        _keyboardBacklightTimeoutSeconds.value = clamped
    }

    suspend fun setKeyboardBacklightKeepOnWithScreen(keepOn: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("keyboard_backlight_keep_on_with_screen", keepOn).apply()
        _keyboardBacklightKeepOnWithScreen.value = keepOn
    }

    suspend fun setKeyboardBacklightBrightness(brightness: Int) = withContext(Dispatchers.IO) {
        val clamped = brightness.coerceIn(0, 100)
        prefs.edit().putInt("keyboard_backlight_brightness", clamped).apply()
        _keyboardBacklightBrightness.value = clamped
    }

    suspend fun setCtsAutoCleanupEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("cts_auto_cleanup_enabled", enabled).apply()
        _isCtsAutoCleanupEnabled.value = enabled
    }

    suspend fun setCtsCleanupDelaySeconds(seconds: Int) = withContext(Dispatchers.IO) {
        val clamped = seconds.coerceIn(2, 15)
        prefs.edit().putInt("cts_cleanup_delay_seconds", clamped).apply()
        _ctsCleanupDelaySeconds.value = clamped
    }

    suspend fun setLastCtsCleaned(timestamp: Long, count: Int) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putLong("last_cts_cleaned_timestamp", timestamp)
            .putInt("last_cts_cleaned_count", count)
            .apply()
        _lastCtsCleanedTimestamp.value = timestamp
        _lastCtsCleanedCount.value = count
    }
}

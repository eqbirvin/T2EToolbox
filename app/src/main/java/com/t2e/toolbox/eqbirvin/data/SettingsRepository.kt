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
}

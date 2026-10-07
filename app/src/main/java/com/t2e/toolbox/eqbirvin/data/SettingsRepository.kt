package com.t2e.toolbox.eqbirvin.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("t2e_settings", Context.MODE_PRIVATE)

    private val _isDisplayFixEnabled = MutableStateFlow(prefs.getBoolean("display_fix_enabled", false))
    val isDisplayFixEnabled: StateFlow<Boolean> = _isDisplayFixEnabled.asStateFlow()

    private val _lastRunRecord = MutableStateFlow<LastRunRecord?>(
        prefs.getString("last_run_record", null)?.let {
            try { Json.decodeFromString(it) } catch (e: Exception) { null }
        }
    )
    val lastRunRecord: StateFlow<LastRunRecord?> = _lastRunRecord.asStateFlow()

    private val _displaySnapshot = MutableStateFlow<DisplaySnapshot?>(
        prefs.getString("display_snapshot", null)?.let {
            try { Json.decodeFromString(it) } catch (e: Exception) { null }
        }
    )
    val displaySnapshot: StateFlow<DisplaySnapshot?> = _displaySnapshot.asStateFlow()

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
}

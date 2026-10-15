package com.t2e.toolbox.eqbirvin.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private const val TAG = "VolumeModeRepository"

enum class RingerModeState(val value: Int, val label: String) {
    NORMAL(AudioManager.RINGER_MODE_NORMAL, "Sound"),
    VIBRATE(AudioManager.RINGER_MODE_VIBRATE, "Vibrate"),
    SILENT(AudioManager.RINGER_MODE_SILENT, "Silent");

    companion object {
        fun fromValue(value: Int): RingerModeState = when (value) {
            AudioManager.RINGER_MODE_SILENT -> SILENT
            AudioManager.RINGER_MODE_VIBRATE -> VIBRATE
            else -> NORMAL
        }
    }
}

sealed class RingerCycleResult {
    data class Success(
        val oldMode: RingerModeState,
        val newMode: RingerModeState,
        val displayLabel: String = newMode.label
    ) : RingerCycleResult()
    data class PermissionRequired(val targetMode: RingerModeState) : RingerCycleResult()
    data class Error(val message: String) : RingerCycleResult()
}

fun calculateNextRingerMode(
    current: RingerModeState,
    option: VolumeCycleOption = VolumeCycleOption.ALL_MODES
): RingerModeState {
    return when (option) {
        VolumeCycleOption.ALL_MODES -> when (current) {
            RingerModeState.NORMAL -> RingerModeState.VIBRATE
            RingerModeState.VIBRATE -> RingerModeState.SILENT
            RingerModeState.SILENT -> RingerModeState.NORMAL
        }
        VolumeCycleOption.SOUND_AND_VIBRATE -> when (current) {
            RingerModeState.NORMAL -> RingerModeState.VIBRATE
            RingerModeState.VIBRATE -> RingerModeState.NORMAL
            RingerModeState.SILENT -> RingerModeState.NORMAL
        }
        VolumeCycleOption.SOUND_AND_SILENT -> when (current) {
            RingerModeState.NORMAL -> RingerModeState.SILENT
            RingerModeState.SILENT -> RingerModeState.NORMAL
            RingerModeState.VIBRATE -> RingerModeState.NORMAL
        }
        VolumeCycleOption.VIBRATE_AND_SILENT -> when (current) {
            RingerModeState.VIBRATE -> RingerModeState.SILENT
            RingerModeState.SILENT -> RingerModeState.VIBRATE
            RingerModeState.NORMAL -> RingerModeState.VIBRATE
        }
    }
}

class VolumeModeRepository(
    private val context: Context,
    private val settingsRepo: SettingsRepository
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private val _currentRingerMode = MutableStateFlow(readCurrentRingerMode())
    val currentRingerMode: StateFlow<RingerModeState> = _currentRingerMode.asStateFlow()

    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION ||
                intent?.action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
                _currentRingerMode.value = readCurrentRingerMode()
            }
        }
    }

    init {
        try {
            val filter = IntentFilter().apply {
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            }
            context.registerReceiver(ringerReceiver, filter)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to register ringer receiver", e)
        }
    }

    fun readCurrentRingerMode(): RingerModeState {
        val filter = notificationManager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
        if (filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
            return RingerModeState.SILENT
        }
        val modeVal = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        return RingerModeState.fromValue(modeVal)
    }

    suspend fun refreshRingerMode(): RingerModeState = withContext(Dispatchers.IO) {
        val mode = readCurrentRingerMode()
        _currentRingerMode.value = mode
        mode
    }

    suspend fun isNotificationPolicyAccessGranted(): Boolean = withContext(Dispatchers.IO) {
        notificationManager?.isNotificationPolicyAccessGranted ?: false
    }

    suspend fun grantNotificationPolicyAccessWithShizuku(): Boolean = withContext(Dispatchers.IO) {
        val cmd = "cmd notification allow_dnd ${context.packageName}"
        val result = ShizukuShell.runShell(cmd)
        if (result.exitCode != 0) {
            Log.e(TAG, "Failed to grant DND access via Shizuku: ${result.stderr}")
            false
        } else {
            isNotificationPolicyAccessGranted()
        }
    }

    suspend fun setShortcutComponentEnabled(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        val component = ComponentName(context, "com.t2e.toolbox.eqbirvin.volume.VolumeModeActivity")
        try {
            context.packageManager.setComponentEnabledSetting(
                component,
                state,
                PackageManager.DONT_KILL_APP
            )
            settingsRepo.setVolumeModeShortcutEnabled(enabled)
            true
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException setting component state", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException setting component state", e)
            false
        }
    }

    suspend fun isShortcutComponentEnabled(): Boolean = withContext(Dispatchers.IO) {
        val component = ComponentName(context, "com.t2e.toolbox.eqbirvin.volume.VolumeModeActivity")
        val state = context.packageManager.getComponentEnabledSetting(component)
        state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    suspend fun cycleRingerMode(): RingerCycleResult = withContext(Dispatchers.IO) {
        val am = audioManager ?: return@withContext RingerCycleResult.Error("AudioManager unavailable")
        val currentMode = readCurrentRingerMode()
        val cycleOption = settingsRepo.volumeModeCycleOption.value
        val targetMode = calculateNextRingerMode(currentMode, cycleOption)
        applyRingerMode(currentMode, targetMode, am, isCycling = true)
    }

    suspend fun setRingerMode(targetMode: RingerModeState): RingerCycleResult = withContext(Dispatchers.IO) {
        val am = audioManager ?: return@withContext RingerCycleResult.Error("AudioManager unavailable")
        val currentMode = readCurrentRingerMode()
        if (currentMode == targetMode) {
            return@withContext RingerCycleResult.Success(currentMode, targetMode)
        }
        applyRingerMode(currentMode, targetMode, am, isCycling = false)
    }

    private suspend fun applyRingerMode(
        currentMode: RingerModeState,
        targetMode: RingerModeState,
        am: AudioManager,
        isCycling: Boolean = false
    ): RingerCycleResult {
        val silentBehavior = settingsRepo.volumeModeSilentBehavior.value

        // Privileged path via Shizuku: directly invokes AudioService's internal ringer control,
        // avoiding AOSP's automatic ZenMode (DND) override when switching to SILENT.
        if (ShizukuShell.isReady()) {
            val (shellCmd, displayLabel) = when (targetMode) {
                RingerModeState.SILENT -> {
                    if (silentBehavior == SilentModeBehavior.DND) {
                        "cmd audio set-ringer-mode SILENT && cmd notification set_dnd priority" to "Do Not Disturb"
                    } else {
                        "cmd audio set-ringer-mode SILENT && cmd notification set_dnd off" to "Silent"
                    }
                }
                RingerModeState.VIBRATE -> {
                    "cmd audio set-ringer-mode VIBRATE && cmd notification set_dnd off" to "Vibrate"
                }
                RingerModeState.NORMAL -> {
                    "cmd audio set-ringer-mode NORMAL && cmd notification set_dnd off" to "Sound"
                }
            }

            val shellResult = ShizukuShell.runShell(shellCmd)
            if (shellResult.exitCode == 0) {
                _currentRingerMode.value = targetMode
                return RingerCycleResult.Success(currentMode, targetMode, displayLabel)
            } else {
                Log.w(TAG, "Shizuku set ringer command failed: ${shellResult.stderr}, falling back to standard API")
            }
        }

        // Standard API path: requires ACCESS_NOTIFICATION_POLICY for SILENT transitions
        if (targetMode == RingerModeState.SILENT || currentMode == RingerModeState.SILENT) {
            var hasDnd = isNotificationPolicyAccessGranted()
            if (!hasDnd) {
                grantNotificationPolicyAccessWithShizuku()
                hasDnd = isNotificationPolicyAccessGranted()
            }
            if (!hasDnd) {
                if (isCycling && targetMode == RingerModeState.SILENT && currentMode != RingerModeState.SILENT) {
                    Log.w(TAG, "DND access not granted for SILENT during cycle; falling back to NORMAL")
                    return applyRingerMode(currentMode, RingerModeState.NORMAL, am, isCycling = false)
                }
                Log.w(TAG, "DND access not granted to change to/from SILENT")
                return RingerCycleResult.PermissionRequired(targetMode)
            }
        }

        return try {
            val displayLabel = when (targetMode) {
                RingerModeState.SILENT -> {
                    if (silentBehavior == SilentModeBehavior.DND) {
                        notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                        am.ringerMode = AudioManager.RINGER_MODE_SILENT
                        "Do Not Disturb"
                    } else {
                        // Silent (Mute only): set ringer to SILENT, then ensure DND filter is cleared to ALL
                        am.ringerMode = AudioManager.RINGER_MODE_SILENT
                        notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        if (am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                            am.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                            am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                        }
                        "Silent"
                    }
                }
                RingerModeState.VIBRATE -> {
                    notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    "Vibrate"
                }
                RingerModeState.NORMAL -> {
                    notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    am.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
                    am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                    "Sound"
                }
            }
            _currentRingerMode.value = targetMode
            RingerCycleResult.Success(currentMode, targetMode, displayLabel)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException setting ringerMode to $targetMode", e)
            RingerCycleResult.PermissionRequired(targetMode)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException setting ringerMode to $targetMode", e)
            RingerCycleResult.Error("Invalid ringer mode")
        }
    }

    fun performHapticFeedback(targetMode: RingerModeState) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            when (targetMode) {
                RingerModeState.VIBRATE -> {
                    // Strong double buzz for vibrate mode
                    val timings = longArrayOf(0, 120, 80, 180)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                }
                RingerModeState.NORMAL -> {
                    // Crisp click for sound mode
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    vibrator.vibrate(effect)
                }
                RingerModeState.SILENT -> {
                    if (settingsRepo.volumeModeSilentBehavior.value == SilentModeBehavior.DND) {
                        // Distinct double-beat pulse for DND
                        val timings = longArrayOf(0, 40, 50, 40, 50, 40)
                        val amplitudes = intArrayOf(0, 140, 0, 140, 0, 140)
                        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                        vibrator.vibrate(effect)
                    } else {
                        // Light subtle double tick for silent mode
                        val timings = longArrayOf(0, 40, 60, 40)
                        val amplitudes = intArrayOf(0, 100, 0, 100)
                        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                        vibrator.vibrate(effect)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while triggering haptic feedback", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException while triggering haptic feedback", e)
        } catch (e: UnsupportedOperationException) {
            Log.e(TAG, "UnsupportedOperationException while triggering haptic feedback", e)
        }
    }
}

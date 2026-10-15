package com.t2e.toolbox.eqbirvin.ui.volumemode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.RingerCycleResult
import com.t2e.toolbox.eqbirvin.data.RingerModeState
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.ShizukuStatus
import com.t2e.toolbox.eqbirvin.data.SilentModeBehavior
import com.t2e.toolbox.eqbirvin.data.VolumeCycleOption
import com.t2e.toolbox.eqbirvin.data.VolumeModeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VolumeModeUiState(
    val isShortcutEnabled: Boolean = false,
    val currentRingerMode: RingerModeState = RingerModeState.NORMAL,
    val isDndAccessGranted: Boolean = false,
    val isShizukuReady: Boolean = false,
    val isToastEnabled: Boolean = true,
    val isHapticEnabled: Boolean = true,
    val cycleOption: VolumeCycleOption = VolumeCycleOption.ALL_MODES,
    val silentBehavior: SilentModeBehavior = SilentModeBehavior.MUTE,
    val lastActionMessage: String? = null
)

private data class VolumeSettingsGroup(
    val toast: Boolean,
    val haptic: Boolean,
    val cycleOption: VolumeCycleOption,
    val silentBehavior: SilentModeBehavior
)

class VolumeModeViewModel(
    private val settingsRepo: SettingsRepository,
    private val volumeRepo: VolumeModeRepository,
    private val displayRepo: DisplayFixRepository
) : ViewModel() {

    private val _isDndGranted = MutableStateFlow(false)
    private val _lastActionMessage = MutableStateFlow<String?>(null)

    private val togglesFlow = combine(
        settingsRepo.isVolumeModeShortcutEnabled,
        volumeRepo.currentRingerMode,
        _isDndGranted,
        displayRepo.shizukuStatus,
        _lastActionMessage
    ) { isShortcut, ringerMode, dndGranted, shizukuStatus, message ->
        VolumeModeUiState(
            isShortcutEnabled = isShortcut,
            currentRingerMode = ringerMode,
            isDndAccessGranted = dndGranted,
            isShizukuReady = shizukuStatus == ShizukuStatus.READY,
            lastActionMessage = message
        )
    }

    private val settingsFlow = combine(
        settingsRepo.isVolumeModeToastEnabled,
        settingsRepo.isVolumeModeHapticEnabled,
        settingsRepo.volumeModeCycleOption,
        settingsRepo.volumeModeSilentBehavior
    ) { toast, haptic, cycleOption, silentBehavior ->
        VolumeSettingsGroup(toast, haptic, cycleOption, silentBehavior)
    }

    val uiState: StateFlow<VolumeModeUiState> = combine(
        togglesFlow,
        settingsFlow
    ) { toggles, settings ->
        toggles.copy(
            isToastEnabled = settings.toast,
            isHapticEnabled = settings.haptic,
            cycleOption = settings.cycleOption,
            silentBehavior = settings.silentBehavior
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VolumeModeUiState())

    init {
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            displayRepo.refreshShizukuStatus()
            _isDndGranted.value = volumeRepo.isNotificationPolicyAccessGranted()
            volumeRepo.refreshRingerMode()
            val componentEnabled = volumeRepo.isShortcutComponentEnabled()
            if (componentEnabled != settingsRepo.isVolumeModeShortcutEnabled.value) {
                settingsRepo.setVolumeModeShortcutEnabled(componentEnabled)
            }
        }
    }

    fun toggleShortcut(enabled: Boolean) {
        viewModelScope.launch {
            val success = volumeRepo.setShortcutComponentEnabled(enabled)
            if (success && enabled) {
                // If DND is not granted, attempt Shizuku grant automatically if Shizuku is ready
                if (!volumeRepo.isNotificationPolicyAccessGranted() && displayRepo.isShizukuReady()) {
                    volumeRepo.grantNotificationPolicyAccessWithShizuku()
                }
            }
            refreshState()
        }
    }

    fun toggleToast(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setVolumeModeToastEnabled(enabled)
        }
    }

    fun toggleHaptic(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setVolumeModeHapticEnabled(enabled)
        }
    }

    fun setCycleOption(option: VolumeCycleOption) {
        viewModelScope.launch {
            settingsRepo.setVolumeModeCycleOption(option)
        }
    }

    fun setSilentModeBehavior(behavior: SilentModeBehavior) {
        viewModelScope.launch {
            settingsRepo.setVolumeModeSilentBehavior(behavior)
        }
    }

    fun cycleMode() {
        viewModelScope.launch {
            val result = volumeRepo.cycleRingerMode()
            handleResult(result)
            refreshState()
        }
    }

    fun setSpecificMode(targetMode: RingerModeState) {
        viewModelScope.launch {
            val result = volumeRepo.setRingerMode(targetMode)
            handleResult(result)
            refreshState()
        }
    }

    fun grantDndViaShizuku() {
        viewModelScope.launch {
            val success = volumeRepo.grantNotificationPolicyAccessWithShizuku()
            _lastActionMessage.value = if (success) {
                "DND access granted successfully via Shizuku!"
            } else {
                "Failed to grant DND access via Shizuku."
            }
            refreshState()
        }
    }

    private fun handleResult(result: RingerCycleResult) {
        when (result) {
            is RingerCycleResult.Success -> {
                if (settingsRepo.isVolumeModeHapticEnabled.value) {
                    volumeRepo.performHapticFeedback(result.newMode)
                }
                _lastActionMessage.value = "Switched to ${result.displayLabel}"
            }
            is RingerCycleResult.PermissionRequired -> {
                _lastActionMessage.value = "Do Not Disturb permission required for ${result.targetMode.label}"
            }
            is RingerCycleResult.Error -> {
                _lastActionMessage.value = "Error: ${result.message}"
            }
        }
    }
}

package com.t2e.toolbox.eqbirvin.ui.keyboardbacklight

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.KeyboardBacklightRepository
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.ShizukuShell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KeyboardBacklightUiState(
    val isShizukuReady: Boolean = false,
    val isCustomEnabled: Boolean = false,
    val timeoutSeconds: Int = 60,
    val keepOnWithScreen: Boolean = false,
    val brightness: Int = 30,
    val liveDeviceTimeoutSeconds: Int? = null,
    val liveDeviceBrightness: Int? = null,
    val statusMessage: String? = null,
    val isStatusError: Boolean = false,
    val isApplying: Boolean = false
)

class KeyboardBacklightViewModel(
    private val settingsRepo: SettingsRepository,
    private val backlightRepo: KeyboardBacklightRepository,
    private val displayRepo: DisplayFixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeyboardBacklightUiState())
    val uiState: StateFlow<KeyboardBacklightUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepo.isKeyboardBacklightCustomEnabled,
                settingsRepo.keyboardBacklightTimeoutSeconds,
                settingsRepo.keyboardBacklightKeepOnWithScreen,
                settingsRepo.keyboardBacklightBrightness
            ) { customEnabled, timeout, keepOn, brightness ->
                _uiState.update { current ->
                    current.copy(
                        isCustomEnabled = customEnabled,
                        timeoutSeconds = timeout,
                        keepOnWithScreen = keepOn,
                        brightness = brightness,
                        isShizukuReady = ShizukuShell.isReady()
                    )
                }
            }.collect {}
        }
        refreshLiveDeviceState()
    }

    fun refreshLiveDeviceState() {
        viewModelScope.launch {
            val isReady = ShizukuShell.isReady()
            val liveTimeout = if (isReady) backlightRepo.readCurrentTimeoutSeconds() else null
            val liveBrightness = if (isReady) backlightRepo.readCurrentBrightness() else null
            _uiState.update {
                it.copy(
                    isShizukuReady = isReady,
                    liveDeviceTimeoutSeconds = liveTimeout,
                    liveDeviceBrightness = liveBrightness
                )
            }
        }
    }

    fun toggleCustomEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setKeyboardBacklightCustomEnabled(enabled)
            if (enabled && ShizukuShell.isReady()) {
                applyCurrentSettings()
            }
        }
    }

    fun setTimeoutSeconds(seconds: Int) {
        viewModelScope.launch {
            val clamped = seconds.coerceIn(5, 300)
            settingsRepo.setKeyboardBacklightTimeoutSeconds(clamped)
            if (_uiState.value.isCustomEnabled && !_uiState.value.keepOnWithScreen && ShizukuShell.isReady()) {
                backlightRepo.applyTimeout(clamped, keepOnWithScreen = false)
                refreshLiveDeviceState()
            }
        }
    }

    fun toggleKeepOnWithScreen(keepOn: Boolean) {
        viewModelScope.launch {
            settingsRepo.setKeyboardBacklightKeepOnWithScreen(keepOn)
            if (_uiState.value.isCustomEnabled && ShizukuShell.isReady()) {
                backlightRepo.applyTimeout(_uiState.value.timeoutSeconds, keepOnWithScreen = keepOn)
                refreshLiveDeviceState()
            }
        }
    }

    fun setBrightness(level: Int) {
        viewModelScope.launch {
            val clamped = level.coerceIn(0, 100)
            settingsRepo.setKeyboardBacklightBrightness(clamped)
            if (_uiState.value.isCustomEnabled && ShizukuShell.isReady()) {
                backlightRepo.applyBrightness(clamped)
                refreshLiveDeviceState()
            }
        }
    }

    fun applyCurrentSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true, statusMessage = null) }
            val res = backlightRepo.applyAllSettings()
            refreshLiveDeviceState()
            _uiState.update {
                it.copy(
                    isApplying = false,
                    statusMessage = if (res.isSuccess) "Keyboard backlight settings applied successfully." else "Failed: ${res.stderr.ifEmpty { "Error code ${res.exitCode}" }}",
                    isStatusError = !res.isSuccess
                )
            }
        }
    }

    fun requestShizukuPermission() {
        viewModelScope.launch {
            displayRepo.requestShizukuPermission()
            refreshLiveDeviceState()
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

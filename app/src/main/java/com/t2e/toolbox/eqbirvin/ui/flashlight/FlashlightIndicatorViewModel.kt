package com.t2e.toolbox.eqbirvin.ui.flashlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.FlashlightIndicatorColor
import com.t2e.toolbox.eqbirvin.data.FlashlightRepository
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.ShizukuStatus
import com.t2e.toolbox.eqbirvin.data.UnifiedPullDownRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FlashlightIndicatorUiState(
    val isIndicatorEnabled: Boolean = true,
    val isFlashlightOn: Boolean = false,
    val isTapToDismiss: Boolean = false,
    val indicatorColor: FlashlightIndicatorColor = FlashlightIndicatorColor.AMBER,
    val isServiceConnected: Boolean = false,
    val isShizukuReady: Boolean = false
)

class FlashlightIndicatorViewModel(
    private val settingsRepo: SettingsRepository,
    private val flashlightRepo: FlashlightRepository,
    private val pullDownRepo: UnifiedPullDownRepository,
    private val displayRepo: DisplayFixRepository
) : ViewModel() {

    private val togglesFlow = combine(
        settingsRepo.isFlashlightIndicatorEnabled,
        flashlightRepo.isFlashlightOn,
        pullDownRepo.serviceConnected,
        displayRepo.shizukuStatus
    ) { isEnabled, isTorchOn, isConnected, shizukuStatus ->
        FlashlightIndicatorUiState(
            isIndicatorEnabled = isEnabled,
            isFlashlightOn = isTorchOn,
            isServiceConnected = isConnected,
            isShizukuReady = shizukuStatus == ShizukuStatus.READY
        )
    }

    private val settingsFlow = combine(
        settingsRepo.isFlashlightIndicatorTapToDismiss,
        settingsRepo.flashlightIndicatorColor
    ) { tapToDismiss, color ->
        Pair(tapToDismiss, color)
    }

    val uiState: StateFlow<FlashlightIndicatorUiState> = combine(
        togglesFlow,
        settingsFlow
    ) { toggles, settings ->
        toggles.copy(
            isTapToDismiss = settings.first,
            indicatorColor = settings.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FlashlightIndicatorUiState())

    fun toggleIndicatorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setFlashlightIndicatorEnabled(enabled)
        }
    }

    fun toggleTapToDismiss(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setFlashlightIndicatorTapToDismiss(enabled)
        }
    }

    fun setColor(color: FlashlightIndicatorColor) {
        viewModelScope.launch {
            settingsRepo.setFlashlightIndicatorColor(color)
        }
    }

    fun toggleFlashlight() {
        viewModelScope.launch {
            flashlightRepo.toggleFlashlight()
        }
    }

    fun enableServiceWithShizuku() {
        viewModelScope.launch {
            pullDownRepo.enableWithShizuku()
        }
    }
}

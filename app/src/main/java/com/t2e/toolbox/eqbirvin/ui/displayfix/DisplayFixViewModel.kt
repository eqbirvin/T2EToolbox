package com.t2e.toolbox.eqbirvin.ui.displayfix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t2e.toolbox.eqbirvin.data.CurrentDisplayState
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.LastRunRecord
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.ShizukuStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DisplayFixUiState(
    val isEnabled: Boolean = false,
    val shizukuStatus: ShizukuStatus = ShizukuStatus.NOT_RUNNING,
    val lastRunRecord: LastRunRecord? = null,
    val currentDisplayState: CurrentDisplayState? = null,
    val isWorking: Boolean = false
) {
    val isShizukuReady: Boolean get() = shizukuStatus == ShizukuStatus.READY
}

class DisplayFixViewModel(
    private val settingsRepo: SettingsRepository,
    private val displayRepo: DisplayFixRepository
) : ViewModel() {

    private val _isWorking = MutableStateFlow(false)
    private val _currentDisplayState = MutableStateFlow<CurrentDisplayState?>(null)

    val uiState: StateFlow<DisplayFixUiState> = combine(
        settingsRepo.isDisplayFixEnabled,
        displayRepo.shizukuStatus,
        settingsRepo.lastRunRecord,
        _currentDisplayState,
        _isWorking
    ) { isEnabled, shizukuStatus, lastRunRecord, currentDisplayState, isWorking ->
        DisplayFixUiState(
            isEnabled = isEnabled,
            shizukuStatus = shizukuStatus,
            lastRunRecord = lastRunRecord,
            currentDisplayState = currentDisplayState,
            isWorking = isWorking
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DisplayFixUiState())

    init {
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            displayRepo.refreshShizukuStatus()
            _currentDisplayState.value = displayRepo.getCurrentDisplayState()
        }
    }

    fun onRequestPermission() {
        viewModelScope.launch {
            displayRepo.requestShizukuPermission()
        }
    }

    fun toggleFix(enabled: Boolean) {
        viewModelScope.launch {
            _isWorking.value = true
            settingsRepo.setDisplayFixEnabled(enabled)
            if (enabled) {
                displayRepo.enableFix("Manual")
            } else {
                displayRepo.disableFix("Manual")
            }
            refreshState()
            _isWorking.value = false
        }
    }

    fun runNow() {
        viewModelScope.launch {
            _isWorking.value = true
            displayRepo.enableFix("Manual")
            refreshState()
            _isWorking.value = false
        }
    }
}

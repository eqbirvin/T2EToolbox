package com.t2e.toolbox.eqbirvin.ui.displayfix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t2e.toolbox.eqbirvin.data.CurrentDisplayState
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.LastRunRecord
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DisplayFixUiState(
    val isEnabled: Boolean = false,
    val isShizukuReady: Boolean = false,
    val lastRunRecord: LastRunRecord? = null,
    val currentDisplayState: CurrentDisplayState? = null,
    val isWorking: Boolean = false
)

class DisplayFixViewModel(
    private val settingsRepo: SettingsRepository,
    private val displayRepo: DisplayFixRepository
) : ViewModel() {

    private val _isWorking = MutableStateFlow(false)
    private val _isShizukuReady = MutableStateFlow(displayRepo.isShizukuReady())
    private val _currentDisplayState = MutableStateFlow<CurrentDisplayState?>(null)

    val uiState: StateFlow<DisplayFixUiState> = combine(
        settingsRepo.isDisplayFixEnabled,
        _isShizukuReady,
        settingsRepo.lastRunRecord,
        _currentDisplayState,
        _isWorking
    ) { isEnabled, isShizukuReady, lastRunRecord, currentDisplayState, isWorking ->
        DisplayFixUiState(
            isEnabled = isEnabled,
            isShizukuReady = isShizukuReady,
            lastRunRecord = lastRunRecord,
            currentDisplayState = currentDisplayState,
            isWorking = isWorking
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DisplayFixUiState())

    init {
        refreshState()
    }

    fun refreshState() {
        _isShizukuReady.value = displayRepo.isShizukuReady()
        viewModelScope.launch {
            _currentDisplayState.value = displayRepo.getCurrentDisplayState()
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

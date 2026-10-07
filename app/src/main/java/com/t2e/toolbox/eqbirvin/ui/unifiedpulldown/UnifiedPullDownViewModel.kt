package com.t2e.toolbox.eqbirvin.ui.unifiedpulldown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.PullDownMethod
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.ShizukuStatus
import com.t2e.toolbox.eqbirvin.data.UnifiedPullDownRepository
import com.t2e.toolbox.eqbirvin.gesture.PullDownAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ServiceConnectionState {
    ENABLED_AND_CONNECTED,
    ENABLED_NOT_CONNECTED,
    DISABLED
}

data class UnifiedPullDownUiState(
    val serviceState: ServiceConnectionState = ServiceConnectionState.DISABLED,
    val isEnabled: Boolean = false,
    val isActiveOnLockscreen: Boolean = true,
    val isDebugTintEnabled: Boolean = false,
    val isSmartShadeDetectionEnabled: Boolean = true,
    val method: PullDownMethod = PullDownMethod.ACCESSIBILITY,
    val shizukuStatus: ShizukuStatus = ShizukuStatus.NOT_RUNNING,
    val shizukuMessage: String? = null,
    val isWorking: Boolean = false
) {
    val canTest: Boolean
        get() = when (method) {
            PullDownMethod.ACCESSIBILITY -> serviceState == ServiceConnectionState.ENABLED_AND_CONNECTED
            PullDownMethod.SHIZUKU -> serviceState == ServiceConnectionState.ENABLED_AND_CONNECTED && shizukuStatus == ShizukuStatus.READY
        }

    val testDisabledReason: String?
        get() = when (method) {
            PullDownMethod.ACCESSIBILITY -> {
                if (serviceState != ServiceConnectionState.ENABLED_AND_CONNECTED) "Accessibility service not connected" else null
            }
            PullDownMethod.SHIZUKU -> {
                if (serviceState != ServiceConnectionState.ENABLED_AND_CONNECTED) {
                    "Accessibility service not connected"
                } else if (shizukuStatus != ShizukuStatus.READY) {
                    "Shizuku not ready (${shizukuStatus.name})"
                } else null
            }
        }
}

private data class SettingsTuple(
    val enabled: Boolean,
    val onLock: Boolean,
    val debugTint: Boolean,
    val smartShade: Boolean,
    val method: PullDownMethod
)

class UnifiedPullDownViewModel(
    private val settingsRepo: SettingsRepository,
    private val pullDownRepo: UnifiedPullDownRepository,
    private val displayFixRepo: DisplayFixRepository
) : ViewModel() {

    private val _isServiceEnabledInSettings = MutableStateFlow(false)
    private val _shizukuMessage = MutableStateFlow<String?>(null)
    private val _isWorking = MutableStateFlow(false)

    private val serviceStateFlow = combine(_isServiceEnabledInSettings, pullDownRepo.serviceConnected) { enabled, connected ->
        when {
            connected -> ServiceConnectionState.ENABLED_AND_CONNECTED
            enabled -> ServiceConnectionState.ENABLED_NOT_CONNECTED
            else -> ServiceConnectionState.DISABLED
        }
    }

    private val settingsFlow = combine(
        settingsRepo.isPullDownEnabled,
        settingsRepo.isPullDownOnLockscreen,
        settingsRepo.isPullDownDebugTint,
        settingsRepo.isSmartShadeDetectionEnabled,
        settingsRepo.pullDownMethod
    ) { enabled, onLock, debugTint, smartShade, method ->
        SettingsTuple(enabled, onLock, debugTint, smartShade, method)
    }

    val uiState: StateFlow<UnifiedPullDownUiState> = combine(
        serviceStateFlow,
        settingsFlow,
        displayFixRepo.shizukuStatus,
        _shizukuMessage,
        _isWorking
    ) { serviceState, settings, shizukuStatus, msg, working ->
        UnifiedPullDownUiState(
            serviceState = serviceState,
            isEnabled = settings.enabled,
            isActiveOnLockscreen = settings.onLock,
            isDebugTintEnabled = settings.debugTint,
            isSmartShadeDetectionEnabled = settings.smartShade,
            method = settings.method,
            shizukuStatus = shizukuStatus,
            shizukuMessage = msg,
            isWorking = working
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UnifiedPullDownUiState())

    init {
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            _isServiceEnabledInSettings.value = pullDownRepo.isServiceEnabled()
            displayFixRepo.refreshShizukuStatus()
        }
    }

    fun togglePullDownEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setPullDownEnabled(enabled)
        }
    }

    fun toggleLockscreen(onLock: Boolean) {
        viewModelScope.launch {
            settingsRepo.setPullDownOnLockscreen(onLock)
        }
    }

    fun toggleDebugTint(tint: Boolean) {
        viewModelScope.launch {
            settingsRepo.setPullDownDebugTint(tint)
        }
    }

    fun toggleSmartShadeDetection(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setSmartShadeDetectionEnabled(enabled)
        }
    }

    fun setMethod(method: PullDownMethod) {
        viewModelScope.launch {
            settingsRepo.setPullDownMethod(method)
        }
    }

    fun enableWithShizuku() {
        viewModelScope.launch {
            _isWorking.value = true
            val result = pullDownRepo.enableWithShizuku()
            _shizukuMessage.value = result.message
            refreshState()
            _isWorking.value = false
        }
    }

    fun disableWithShizuku() {
        viewModelScope.launch {
            _isWorking.value = true
            val result = pullDownRepo.disableWithShizuku()
            _shizukuMessage.value = result.message
            refreshState()
            _isWorking.value = false
        }
    }

    fun testOpenNotifications() {
        viewModelScope.launch {
            pullDownRepo.sendTestCommand(PullDownAction.OPEN_NOTIFICATIONS)
        }
    }

    fun testOpenQuickSettings() {
        viewModelScope.launch {
            pullDownRepo.sendTestCommand(PullDownAction.OPEN_QUICK_SETTINGS)
        }
    }
}

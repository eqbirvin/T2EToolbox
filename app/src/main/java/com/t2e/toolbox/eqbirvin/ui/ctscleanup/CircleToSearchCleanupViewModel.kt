package com.t2e.toolbox.eqbirvin.ui.ctscleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t2e.toolbox.eqbirvin.data.CircleToSearchCleanupRepository
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.ShizukuShell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CtsCleanupUiState(
    val isAutoCleanEnabled: Boolean = true,
    val delaySeconds: Int = 4,
    val fileCount: Int = 0,
    val totalBytes: Long = 0L,
    val formattedSize: String = "0 B",
    val isCleaning: Boolean = false,
    val lastCleanedText: String? = null,
    val statusMessage: String? = null,
    val isStatusError: Boolean = false,
    val isShizukuReady: Boolean = false
)

class CircleToSearchCleanupViewModel(
    private val settingsRepo: SettingsRepository,
    private val ctsRepo: CircleToSearchCleanupRepository,
    private val displayRepo: DisplayFixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CtsCleanupUiState())
    val uiState: StateFlow<CtsCleanupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepo.isCtsAutoCleanupEnabled,
                settingsRepo.ctsCleanupDelaySeconds,
                ctsRepo.storageInfo,
                ctsRepo.isCleaning
            ) { autoClean, delay, storage, cleaning ->
                val lastTime: Long = settingsRepo.lastCtsCleanedTimestamp.value
                val lastCount: Int = settingsRepo.lastCtsCleanedCount.value
                val lastCleanedStr = if (lastTime > 0L) {
                    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    "Last cleaned: ${sdf.format(Date(lastTime))} ($lastCount items removed)"
                } else null

                _uiState.update { current ->
                    current.copy(
                        isAutoCleanEnabled = autoClean,
                        delaySeconds = delay,
                        fileCount = storage.fileCount,
                        totalBytes = storage.totalBytes,
                        formattedSize = storage.formattedSize,
                        isCleaning = cleaning,
                        lastCleanedText = lastCleanedStr,
                        isShizukuReady = ShizukuShell.isReady()
                    )
                }
            }.collect {}
        }
        viewModelScope.launch {
            settingsRepo.lastCtsCleanedTimestamp.collect { lastTime ->
                val lastCount: Int = settingsRepo.lastCtsCleanedCount.value
                val lastCleanedStr = if (lastTime > 0L) {
                    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    "Last cleaned: ${sdf.format(Date(lastTime))} ($lastCount items removed)"
                } else null
                _uiState.update { it.copy(lastCleanedText = lastCleanedStr) }
            }
        }
        refresh()
    }

    fun refresh() {
        ctsRepo.refreshStorageInfo()
        _uiState.update { it.copy(isShizukuReady = ShizukuShell.isReady()) }
    }

    fun toggleAutoCleanup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setCtsAutoCleanupEnabled(enabled)
            if (enabled) {
                ctsRepo.startObserving()
            }
        }
    }

    fun setDelaySeconds(seconds: Int) {
        viewModelScope.launch {
            val clamped = seconds.coerceIn(2, 15)
            settingsRepo.setCtsCleanupDelaySeconds(clamped)
        }
    }

    fun cleanNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = null) }
            val res = ctsRepo.cleanNow()
            if (res.success) {
                val freedStr = if (res.freedBytes > 0) {
                    val mb = res.freedBytes / (1024.0 * 1024.0)
                    if (mb >= 1.0) String.format("%.2f MB", mb) else "${res.freedBytes / 1024} KB"
                } else "0 KB"
                val msg = if (res.deletedCount > 0) {
                    "Cleaned ${res.deletedCount} screenshots ($freedStr freed)."
                } else {
                    "No Circle to Search screenshots to clean."
                }
                _uiState.update {
                    it.copy(
                        statusMessage = msg,
                        isStatusError = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        statusMessage = "Cleanup failed: ${res.errorMessage ?: "Unknown error"}",
                        isStatusError = true
                    )
                }
            }
        }
    }

    fun requestShizukuPermission() {
        viewModelScope.launch {
            displayRepo.requestShizukuPermission()
            refresh()
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

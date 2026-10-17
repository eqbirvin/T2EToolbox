package com.t2e.toolbox.eqbirvin.ui.typingblock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.TypingScrollBlockRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TypingScrollBlockViewModel(
    private val settingsRepo: SettingsRepository,
    private val typingBlockRepo: TypingScrollBlockRepository,
    private val displayRepo: DisplayFixRepository
) : ViewModel() {

    val uiState: StateFlow<TypingScrollBlockUiState> = combine(
        settingsRepo.isBlockScrollWhileTypingEnabled,
        settingsRepo.scrollBlockDebounceMs,
        typingBlockRepo.isServiceConnected,
        typingBlockRepo.isTypingActive
    ) { enabled, debounce, connected, typingActive ->
        TypingScrollBlockUiState(
            isEnabled = enabled,
            debounceMs = debounce,
            isServiceConnected = connected,
            isTypingActive = typingActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TypingScrollBlockUiState()
    )

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setBlockScrollWhileTypingEnabled(enabled)
        }
    }

    fun setDebounceMs(ms: Int) {
        viewModelScope.launch {
            settingsRepo.setScrollBlockDebounceMs(ms)
        }
    }
}

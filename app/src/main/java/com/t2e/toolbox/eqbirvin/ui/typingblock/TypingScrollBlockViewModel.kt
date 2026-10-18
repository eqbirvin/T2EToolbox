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

    private val settingsFlow = combine(
        settingsRepo.isBlockScrollWhileTypingEnabled,
        settingsRepo.isBlockOnKeystrokeEnabled,
        settingsRepo.isBlockOnTextFieldFocusEnabled,
        settingsRepo.scrollBlockDebounceMs
    ) { enabled, onKeystroke, onTextField, debounce ->
        SettingsTuple(enabled, onKeystroke, onTextField, debounce)
    }

    private val runtimeFlow = combine(
        typingBlockRepo.isServiceConnected,
        typingBlockRepo.isTypingActive,
        typingBlockRepo.isTextFieldFocused,
        typingBlockRepo.isScrollSuppressed
    ) { connected, typingActive, textFieldFocused, suppressed ->
        RuntimeTuple(connected, typingActive, textFieldFocused, suppressed)
    }

    val uiState: StateFlow<TypingScrollBlockUiState> = combine(
        settingsFlow,
        runtimeFlow
    ) { settings, runtime ->
        TypingScrollBlockUiState(
            isEnabled = settings.enabled,
            isBlockOnKeystroke = settings.onKeystroke,
            isBlockOnTextFieldFocus = settings.onTextField,
            debounceMs = settings.debounce,
            isServiceConnected = runtime.connected,
            isTypingActive = runtime.typingActive,
            isTextFieldFocused = runtime.textFieldFocused,
            isScrollSuppressed = runtime.suppressed
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

    fun toggleBlockOnKeystroke(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setBlockOnKeystrokeEnabled(enabled)
        }
    }

    fun toggleBlockOnTextFieldFocus(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setBlockOnTextFieldFocusEnabled(enabled)
        }
    }

    fun setDebounceMs(ms: Int) {
        viewModelScope.launch {
            settingsRepo.setScrollBlockDebounceMs(ms)
        }
    }

    private data class SettingsTuple(
        val enabled: Boolean,
        val onKeystroke: Boolean,
        val onTextField: Boolean,
        val debounce: Int
    )

    private data class RuntimeTuple(
        val connected: Boolean,
        val typingActive: Boolean,
        val textFieldFocused: Boolean,
        val suppressed: Boolean
    )
}

package com.t2e.toolbox.eqbirvin.ui.typingblock

data class TypingScrollBlockUiState(
    val isEnabled: Boolean = false,
    val debounceMs: Int = 800,
    val isServiceConnected: Boolean = false,
    val isTypingActive: Boolean = false
)

data class TypingScrollBlockActions(
    val onToggleEnabled: (Boolean) -> Unit,
    val onSetDebounceMs: (Int) -> Unit,
    val onOpenAccessibilitySettings: () -> Unit,
    val onBack: () -> Unit
)

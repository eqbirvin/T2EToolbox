package com.t2e.toolbox.eqbirvin.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TypingScrollBlockRepository(
    private val settingsRepository: SettingsRepository
) {
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _isTypingActive = MutableStateFlow(false)
    val isTypingActive: StateFlow<Boolean> = _isTypingActive.asStateFlow()

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
        if (!connected) {
            _isTypingActive.value = false
        }
    }

    fun setTypingActive(active: Boolean) {
        _isTypingActive.value = active
    }
}

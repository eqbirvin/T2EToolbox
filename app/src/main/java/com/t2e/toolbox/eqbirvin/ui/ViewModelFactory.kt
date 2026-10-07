package com.t2e.toolbox.eqbirvin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.UnifiedPullDownRepository
import com.t2e.toolbox.eqbirvin.ui.displayfix.DisplayFixViewModel
import com.t2e.toolbox.eqbirvin.ui.unifiedpulldown.UnifiedPullDownViewModel

class AppViewModelFactory(
    private val settingsRepo: SettingsRepository,
    private val displayRepo: DisplayFixRepository,
    private val pullDownRepo: UnifiedPullDownRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DisplayFixViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DisplayFixViewModel(settingsRepo, displayRepo) as T
        }
        if (modelClass.isAssignableFrom(UnifiedPullDownViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UnifiedPullDownViewModel(settingsRepo, pullDownRepo, displayRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

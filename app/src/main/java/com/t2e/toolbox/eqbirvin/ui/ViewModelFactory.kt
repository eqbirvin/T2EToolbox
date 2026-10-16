package com.t2e.toolbox.eqbirvin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.FlashlightRepository
import com.t2e.toolbox.eqbirvin.data.PerAppDpiRepository
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.UnifiedPullDownRepository
import com.t2e.toolbox.eqbirvin.data.VolumeModeRepository
import com.t2e.toolbox.eqbirvin.ui.displayfix.DisplayFixViewModel
import com.t2e.toolbox.eqbirvin.ui.flashlight.FlashlightIndicatorViewModel
import com.t2e.toolbox.eqbirvin.ui.perappdpi.PerAppDpiViewModel
import com.t2e.toolbox.eqbirvin.ui.unifiedpulldown.UnifiedPullDownViewModel
import com.t2e.toolbox.eqbirvin.ui.volumemode.VolumeModeViewModel

class AppViewModelFactory(
    private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val displayRepo: DisplayFixRepository,
    private val pullDownRepo: UnifiedPullDownRepository,
    private val volumeRepo: VolumeModeRepository,
    private val flashlightRepo: FlashlightRepository,
    private val perAppDpiRepo: PerAppDpiRepository,
    private val keyboardBacklightRepo: com.t2e.toolbox.eqbirvin.data.KeyboardBacklightRepository
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
        if (modelClass.isAssignableFrom(VolumeModeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VolumeModeViewModel(settingsRepo, volumeRepo, displayRepo) as T
        }
        if (modelClass.isAssignableFrom(FlashlightIndicatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlashlightIndicatorViewModel(settingsRepo, flashlightRepo, pullDownRepo, displayRepo) as T
        }
        if (modelClass.isAssignableFrom(PerAppDpiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PerAppDpiViewModel(context, settingsRepo, perAppDpiRepo, displayRepo) as T
        }
        if (modelClass.isAssignableFrom(com.t2e.toolbox.eqbirvin.ui.keyboardbacklight.KeyboardBacklightViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.t2e.toolbox.eqbirvin.ui.keyboardbacklight.KeyboardBacklightViewModel(settingsRepo, keyboardBacklightRepo, displayRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

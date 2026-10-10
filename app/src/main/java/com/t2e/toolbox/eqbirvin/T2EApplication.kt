package com.t2e.toolbox.eqbirvin

import android.app.Application
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.UnifiedPullDownRepository
import com.t2e.toolbox.eqbirvin.data.VolumeModeRepository

class T2EApplication : Application() {
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    val displayFixRepository: DisplayFixRepository by lazy {
        DisplayFixRepository(applicationContext, settingsRepository)
    }

    val unifiedPullDownRepository: UnifiedPullDownRepository by lazy {
        UnifiedPullDownRepository(applicationContext, settingsRepository)
    }

    val volumeModeRepository: VolumeModeRepository by lazy {
        VolumeModeRepository(applicationContext, settingsRepository)
    }
}

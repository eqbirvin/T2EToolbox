package com.t2e.toolbox.eqbirvin

import android.app.Application
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.FlashlightRepository
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

    val flashlightRepository: FlashlightRepository by lazy {
        FlashlightRepository(applicationContext, settingsRepository)
    }

    val perAppDpiRepository: com.t2e.toolbox.eqbirvin.data.PerAppDpiRepository by lazy {
        com.t2e.toolbox.eqbirvin.data.PerAppDpiRepository(applicationContext, settingsRepository)
    }

    val keyboardBacklightRepository: com.t2e.toolbox.eqbirvin.data.KeyboardBacklightRepository by lazy {
        com.t2e.toolbox.eqbirvin.data.KeyboardBacklightRepository(applicationContext, settingsRepository)
    }

    val circleToSearchCleanupRepository: com.t2e.toolbox.eqbirvin.data.CircleToSearchCleanupRepository by lazy {
        com.t2e.toolbox.eqbirvin.data.CircleToSearchCleanupRepository(applicationContext, settingsRepository)
    }

    val typingScrollBlockRepository: com.t2e.toolbox.eqbirvin.data.TypingScrollBlockRepository by lazy {
        com.t2e.toolbox.eqbirvin.data.TypingScrollBlockRepository(settingsRepository)
    }

    override fun onCreate() {
        super.onCreate()
        circleToSearchCleanupRepository.startObserving()
        val screenFilter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_SCREEN_ON)
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                    when (intent?.action) {
                        android.content.Intent.ACTION_SCREEN_ON -> keyboardBacklightRepository.onScreenOn()
                        android.content.Intent.ACTION_SCREEN_OFF -> keyboardBacklightRepository.onScreenOff()
                    }
                }
            },
            screenFilter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}

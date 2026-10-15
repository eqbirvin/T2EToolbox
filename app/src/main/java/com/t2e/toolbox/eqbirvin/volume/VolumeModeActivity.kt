package com.t2e.toolbox.eqbirvin.volume

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.t2e.toolbox.eqbirvin.T2EApplication
import com.t2e.toolbox.eqbirvin.data.RingerCycleResult
import kotlinx.coroutines.launch

class VolumeModeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableActivityTransitions()

        val app = application as T2EApplication
        val volumeRepo = app.volumeModeRepository
        val settingsRepo = app.settingsRepository

        lifecycleScope.launch {
            try {
                val result = volumeRepo.cycleRingerMode()
                val showToast = settingsRepo.isVolumeModeToastEnabled.value
                val doHaptic = settingsRepo.isVolumeModeHapticEnabled.value

                when (result) {
                    is RingerCycleResult.Success -> {
                        if (doHaptic) {
                            volumeRepo.performHapticFeedback(result.newMode)
                        }
                        if (showToast) {
                            Toast.makeText(
                                applicationContext,
                                "Sound Mode: ${result.displayLabel}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    is RingerCycleResult.PermissionRequired -> {
                        Toast.makeText(
                            applicationContext,
                            "Do Not Disturb access required for Silent mode. Open T2E Toolbox to grant.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is RingerCycleResult.Error -> {
                        Toast.makeText(
                            applicationContext,
                            "Error changing volume mode: ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } finally {
                finishAndRemoveTask()
                disableActivityTransitions()
            }
        }
    }

    private fun disableActivityTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}

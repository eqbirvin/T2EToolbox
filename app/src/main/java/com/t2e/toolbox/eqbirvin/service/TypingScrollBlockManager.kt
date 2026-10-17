package com.t2e.toolbox.eqbirvin.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.t2e.toolbox.eqbirvin.data.TypingScrollBlockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "TypingScrollBlockMgr"

class TypingScrollBlockManager(
    private val service: AccessibilityService,
    private val repository: TypingScrollBlockRepository,
    private val serviceScope: CoroutineScope
) {
    var isEnabled: Boolean = false
        private set
    var debounceMs: Int = 800
        private set

    private var isTouchpadClaimed: Boolean = false
    private var debounceJob: Job? = null

    fun updateConfig(enabled: Boolean, debounce: Int) {
        val wasEnabled = isEnabled
        isEnabled = enabled
        debounceMs = debounce.coerceIn(300, 2500)
        Log.d(TAG, "updateConfig: enabled=$enabled, debounceMs=$debounceMs")
        if (wasEnabled && !enabled) {
            cleanup()
        }
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isEnabled) return false

        if (isTypingKey(event) && event.action == KeyEvent.ACTION_DOWN) {
            onTypingKeyDown()
        }

        // Crucial: Always return false so the physical key press reaches the focused text field
        return false
    }

    fun onMotionEvent(event: MotionEvent): Boolean {
        if (!isEnabled || !isTouchpadClaimed) return false

        if (event.isFromSource(InputDevice.SOURCE_TOUCHPAD)) {
            // Accidental trackpad swipe during active typing debounce - consume and drop
            return true
        }
        return false
    }

    private fun onTypingKeyDown() {
        if (!isTouchpadClaimed) {
            claimTouchpad(true)
            repository.setTypingActive(true)
        }

        debounceJob?.cancel()
        debounceJob = serviceScope.launch(Dispatchers.Main) {
            delay(debounceMs.toLong())
            claimTouchpad(false)
            repository.setTypingActive(false)
        }
    }

    private fun claimTouchpad(claim: Boolean) {
        try {
            val info = service.serviceInfo ?: return
            val currentSources = info.motionEventSources
            val targetSources = if (claim) {
                currentSources or InputDevice.SOURCE_TOUCHPAD
            } else {
                currentSources and InputDevice.SOURCE_TOUCHPAD.inv()
            }

            if (currentSources != targetSources) {
                info.motionEventSources = targetSources
                service.serviceInfo = info
                Log.d(TAG, "Touchpad claim changed: claim=$claim, sources=$targetSources")
            }
            isTouchpadClaimed = claim
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException updating serviceInfo motionEventSources", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException updating serviceInfo motionEventSources", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException updating serviceInfo motionEventSources", e)
        }
    }

    private fun isTypingKey(event: KeyEvent): Boolean {
        val code = event.keyCode

        // Exclude system navigation, volume, power, media
        if (code == KeyEvent.KEYCODE_BACK ||
            code == KeyEvent.KEYCODE_HOME ||
            code == KeyEvent.KEYCODE_APP_SWITCH ||
            code == KeyEvent.KEYCODE_VOLUME_UP ||
            code == KeyEvent.KEYCODE_VOLUME_DOWN ||
            code == KeyEvent.KEYCODE_VOLUME_MUTE ||
            code == KeyEvent.KEYCODE_POWER ||
            code == KeyEvent.KEYCODE_CAMERA ||
            code == KeyEvent.KEYCODE_BRIGHTNESS_UP ||
            code == KeyEvent.KEYCODE_BRIGHTNESS_DOWN ||
            code == KeyEvent.KEYCODE_HEADSETHOOK
        ) {
            return false
        }

        if (event.isPrintingKey) return true
        if (KeyEvent.isModifierKey(code)) return true

        return when (code) {
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_FORWARD_DEL,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_SYM -> true
            else -> false
        }
    }

    fun cleanup() {
        debounceJob?.cancel()
        debounceJob = null
        if (isTouchpadClaimed) {
            claimTouchpad(false)
        }
        repository.setTypingActive(false)
    }
}

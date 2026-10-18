package com.t2e.toolbox.eqbirvin.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.t2e.toolbox.eqbirvin.data.ShizukuShell
import com.t2e.toolbox.eqbirvin.data.TypingScrollBlockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "TypingScrollBlockMgr"
private const val AGUI_INPUT_STATUS_PROP = "debug.sf.agui_input_status"

class TypingScrollBlockManager(
    private val service: AccessibilityService,
    private val repository: TypingScrollBlockRepository,
    private val serviceScope: CoroutineScope
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val propSyncDispatcher = Dispatchers.IO.limitedParallelism(1)

    var isEnabled: Boolean = false
        private set
    var isBlockOnKeystroke: Boolean = true
        private set
    var isBlockOnTextFieldFocus: Boolean = true
        private set
    var debounceMs: Int = 800
        private set

    private var isDebounceClaimActive: Boolean = false
    private var isTextFieldFocusClaimActive: Boolean = false
    private var isTouchpadClaimed: Boolean = false
    private var debounceJob: Job? = null
    private var imeRaceGuardJob: Job? = null

    fun updateConfig(
        masterEnabled: Boolean,
        blockOnKeystroke: Boolean,
        blockOnTextFieldFocus: Boolean,
        debounce: Int
    ) {
        val wasEnabled = isEnabled
        isEnabled = masterEnabled
        isBlockOnKeystroke = blockOnKeystroke
        isBlockOnTextFieldFocus = blockOnTextFieldFocus
        debounceMs = debounce.coerceIn(300, 2500)
        Log.d(
            TAG,
            "updateConfig: enabled=$masterEnabled, keystroke=$blockOnKeystroke, textField=$blockOnTextFieldFocus, debounceMs=$debounceMs"
        )

        if (wasEnabled && !masterEnabled) {
            cleanup()
        } else {
            if (isEnabled && isBlockOnTextFieldFocus) {
                inspectCurrentWindowFocus()
            }
            reevaluateClaim()
        }
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isEnabled || !isBlockOnKeystroke) return false

        if (isTypingKey(event) && event.action == KeyEvent.ACTION_DOWN) {
            onTypingKeyDown()
        }

        // Crucial: Always return false so the physical key press reaches the focused text field
        return false
    }

    fun onTextChanged() {
        if (!isEnabled) return
        if (isBlockOnKeystroke) {
            onTypingKeyDown()
        }
        if (isBlockOnTextFieldFocus && !isTextFieldFocusClaimActive) {
            onTextFieldFocusChanged(true)
        }
    }

    fun onTextFieldFocusChanged(focused: Boolean) {
        if (!isEnabled) return
        val changed = isTextFieldFocusClaimActive != focused
        isTextFieldFocusClaimActive = focused
        if (changed) {
            repository.setTextFieldFocused(focused)
        }
        reevaluateClaim()
        if (focused && isBlockOnTextFieldFocus) {
            scheduleImeRaceGuard()
        } else if (!focused) {
            imeRaceGuardJob?.cancel()
            imeRaceGuardJob = null
        }
    }

    @Suppress("DEPRECATION")
    fun handleWindowState(isHomeOrRecents: Boolean) {
        if (!isEnabled || !isBlockOnTextFieldFocus) return
        if (isHomeOrRecents) {
            onTextFieldFocusChanged(false)
            return
        }
        inspectCurrentWindowFocus()
    }

    @Suppress("DEPRECATION")
    fun handleFocusEvent(event: AccessibilityEvent) {
        if (!isEnabled || !isBlockOnTextFieldFocus) return

        val node = try {
            event.source
        } catch (e: IllegalStateException) {
            null
        } catch (e: SecurityException) {
            null
        }

        val isEditable = try {
            if (node != null) {
                node.isEditable || (node.className?.contains("EditText", ignoreCase = true) == true)
            } else {
                val className = event.className?.toString() ?: ""
                className.contains("EditText", ignoreCase = true)
            }
        } finally {
            try {
                node?.recycle()
            } catch (ignored: IllegalStateException) {
            }
        }
        onTextFieldFocusChanged(isEditable)
    }

    @Suppress("DEPRECATION")
    private fun inspectCurrentWindowFocus() {
        val root = try {
            service.rootInActiveWindow
        } catch (e: IllegalStateException) {
            null
        } catch (e: SecurityException) {
            null
        }
        val focusedNode = try {
            root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } catch (e: IllegalStateException) {
            null
        } catch (e: SecurityException) {
            null
        }
        val isEditable = focusedNode?.isEditable == true ||
            (focusedNode?.className?.contains("EditText", ignoreCase = true) == true)
        try {
            focusedNode?.recycle()
            root?.recycle()
        } catch (ignored: IllegalStateException) {
        }

        onTextFieldFocusChanged(isEditable)
    }

    fun onMotionEvent(event: MotionEvent): Boolean {
        if (!isEnabled || !isTouchpadClaimed) return false

        if (event.isFromSource(InputDevice.SOURCE_TOUCHPAD)) {
            return true
        }
        return false
    }

    private fun onTypingKeyDown() {
        isDebounceClaimActive = true
        repository.setTypingActive(true)
        reevaluateClaim()

        debounceJob?.cancel()
        debounceJob = serviceScope.launch(Dispatchers.Main) {
            delay(debounceMs.toLong())
            isDebounceClaimActive = false
            repository.setTypingActive(false)
            reevaluateClaim()
        }
    }

    private fun reevaluateClaim() {
        val shouldClaim = isEnabled && (
            (isBlockOnKeystroke && isDebounceClaimActive) ||
            (isBlockOnTextFieldFocus && isTextFieldFocusClaimActive)
        )
        val expectedProp = if (shouldClaim) "1" else "0"
        val propMismatch = shouldClaim && readNativeAguiInputStatus() != expectedProp
        if (shouldClaim != isTouchpadClaimed || propMismatch) {
            claimTouchpad(shouldClaim)
        }
        repository.setScrollSuppressed(shouldClaim)
    }

    private fun scheduleImeRaceGuard() {
        imeRaceGuardJob?.cancel()
        imeRaceGuardJob = serviceScope.launch(Dispatchers.Main) {
            delay(120L)
            if (isEnabled && isTouchpadClaimed && readNativeAguiInputStatus() != "1") {
                syncNativeAguiInputStatus(true)
            }
            delay(180L)
            if (isEnabled && isTouchpadClaimed && readNativeAguiInputStatus() != "1") {
                syncNativeAguiInputStatus(true)
            }
        }
    }

    private fun claimTouchpad(claim: Boolean) {
        isTouchpadClaimed = claim
        syncNativeAguiInputStatus(claim)

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
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException updating serviceInfo motionEventSources", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException updating serviceInfo motionEventSources", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException updating serviceInfo motionEventSources", e)
        }
    }

    private fun syncNativeAguiInputStatus(claim: Boolean) {
        val targetVal = if (claim) "1" else "0"
        if (readNativeAguiInputStatus() == targetVal) return

        serviceScope.launch(propSyncDispatcher) {
            if (readNativeAguiInputStatus() == targetVal) return@launch
            if (ShizukuShell.isReady()) {
                val res = ShizukuShell.runShell("setprop $AGUI_INPUT_STATUS_PROP $targetVal")
                if (res.exitCode == 0) {
                    Log.d(TAG, "Synced $AGUI_INPUT_STATUS_PROP=$targetVal")
                } else {
                    Log.w(TAG, "Failed to sync $AGUI_INPUT_STATUS_PROP=$targetVal: ${res.stderr}")
                }
            } else {
                Log.w(TAG, "Shizuku not ready; cannot set $AGUI_INPUT_STATUS_PROP=$targetVal")
            }
        }
    }

    private fun readNativeAguiInputStatus(): String {
        return try {
            val spClass = Class.forName("android.os.SystemProperties")
            val getMethod = spClass.getMethod("get", String::class.java, String::class.java)
            (getMethod.invoke(null, AGUI_INPUT_STATUS_PROP, "0") as? String) ?: "0"
        } catch (e: ClassNotFoundException) {
            "0"
        } catch (e: NoSuchMethodException) {
            "0"
        } catch (e: IllegalAccessException) {
            "0"
        } catch (e: java.lang.reflect.InvocationTargetException) {
            "0"
        }
    }

    private fun isTypingKey(event: KeyEvent): Boolean {
        val code = event.keyCode

        // Exclude system navigation, volume, power, media, and synthetic cursor DPAD keys
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
            code == KeyEvent.KEYCODE_HEADSETHOOK ||
            code == KeyEvent.KEYCODE_DPAD_UP ||
            code == KeyEvent.KEYCODE_DPAD_DOWN ||
            code == KeyEvent.KEYCODE_DPAD_LEFT ||
            code == KeyEvent.KEYCODE_DPAD_RIGHT
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
        imeRaceGuardJob?.cancel()
        imeRaceGuardJob = null
        isDebounceClaimActive = false
        isTextFieldFocusClaimActive = false
        if (isTouchpadClaimed || readNativeAguiInputStatus() == "1") {
            claimTouchpad(false)
        }
        repository.setTypingActive(false)
        repository.setTextFieldFocused(false)
        repository.setScrollSuppressed(false)
    }
}

package com.t2e.toolbox.eqbirvin.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.t2e.toolbox.eqbirvin.data.ShizukuShell
import com.t2e.toolbox.eqbirvin.data.TypingScrollBlockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "TypingScrollBlockMgr"
private const val AGUI_INPUT_STATUS_PROP = "debug.sf.agui_input_status"
private const val SINK_WINDOW_TITLE = "InputMethod"

class TypingScrollBlockManager(
    private val service: AccessibilityService,
    private val windowManager: WindowManager,
    private val repository: TypingScrollBlockRepository,
    private val serviceScope: CoroutineScope
) {
    private val propSyncMutex = Mutex()

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
    @Volatile
    private var isTouchpadClaimed: Boolean = false
    private var debounceJob: Job? = null
    private var imeWatchdogJob: Job? = null

    private var sinkView: View? = null
    private var isSinkAttached: Boolean = false

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
        debounceMs = debounce.coerceIn(300, 10000)
        Log.d(
            TAG,
            "updateConfig: enabled=$masterEnabled, keystroke=$blockOnKeystroke, textField=$blockOnTextFieldFocus, debounceMs=$debounceMs"
        )

        if (wasEnabled && !masterEnabled) {
            cleanup()
        } else {
            syncSinkWindow()
            if (isEnabled && isBlockOnTextFieldFocus) {
                inspectCurrentWindowFocus()
            } else if (!isBlockOnTextFieldFocus && isTextFieldFocusClaimActive) {
                isTextFieldFocusClaimActive = false
                repository.setTextFieldFocused(false)
            }
            reevaluateClaim()
        }
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isEnabled) return false

        if (isTypingKey(event) && event.action == KeyEvent.ACTION_DOWN) {
            if (isBlockOnKeystroke) {
                onTypingKeyDown()
            }
            if (isBlockOnTextFieldFocus && !isTextFieldFocusClaimActive) {
                inspectCurrentWindowFocus()
            }
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
            Log.d(TAG, "TextField focus changed: focused=$focused")
        }
        reevaluateClaim()
    }

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

        val isSourceEditable = try {
            if (node != null) {
                node.isEditable || (node.className?.contains("EditText", ignoreCase = true) == true)
            } else {
                false
            }
        } finally {
            try {
                node?.recycle()
            } catch (ignored: IllegalStateException) {
            }
        }

        if (isSourceEditable) {
            onTextFieldFocusChanged(true)
        } else {
            // Secondary focus/selection events (e.g. Compose virtual nodes or popup windows)
            // may have a non-editable or null event.source while the active window's FOCUS_INPUT
            // is still an editable text field. Always verify against rootInActiveWindow.
            inspectCurrentWindowFocus()
        }
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
        if (root == null) return

        val focusedNode = try {
            root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } catch (e: IllegalStateException) {
            null
        } catch (e: SecurityException) {
            null
        }
        val isEditable = focusedNode?.isEditable == true ||
            (focusedNode?.className?.contains("EditText", ignoreCase = true) == true)
        try {
            focusedNode?.recycle()
            root.recycle()
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

    private fun computeShouldClaim(): Boolean {
        return isEnabled && (
            (isBlockOnKeystroke && isDebounceClaimActive) ||
                (isBlockOnTextFieldFocus && isTextFieldFocusClaimActive)
            )
    }

    private fun reevaluateClaim() {
        val shouldClaim = computeShouldClaim()
        val expectedProp = if (shouldClaim) "1" else "0"
        val propMismatch = readNativeAguiInputStatus() != expectedProp
        if (shouldClaim != isTouchpadClaimed || propMismatch) {
            claimTouchpad(shouldClaim)
        } else if (shouldClaim) {
            startImeWatchdog()
        }
        repository.setScrollSuppressed(shouldClaim)
    }

    private fun claimTouchpad(claim: Boolean) {
        isTouchpadClaimed = claim
        syncSinkWindow()
        requestPropSync()

        if (claim) {
            startImeWatchdog()
        } else {
            imeWatchdogJob?.cancel()
            imeWatchdogJob = null
        }

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

    private fun startImeWatchdog() {
        imeWatchdogJob?.cancel()
        imeWatchdogJob = serviceScope.launch(Dispatchers.Main) {
            for (delayMs in longArrayOf(150L, 250L, 400L)) {
                delay(delayMs)
                if (!isEnabled || !isTouchpadClaimed) break
                if (readNativeAguiInputStatus() != "1") {
                    requestPropSync()
                }
            }
        }
    }

    private fun syncSinkWindow() {
        if (isEnabled) {
            attachSinkWindow()
        } else {
            detachSinkWindow()
        }
    }

    /**
     * Attaches a 1x1 transparent, non-touchable, non-focusable accessibility overlay window
     * titled "InputMethod".
     *
     * Why this is required on Unihertz Titan 2 Elite (`libinputflinger.so`):
     * When `debug.sf.agui_input_status == "1"`, `InputDispatcher::notifyMotion` (0x21e400) skips
     * converting `AINPUT_SOURCE_TOUCHPAD` into `AINPUT_SOURCE_TOUCHSCREEN`, and `dispatchMotionLocked`
     * (0x203bd8) instructs `findTouchedWindowAt` (0x205184) to find a window whose title contains
     * `"InputMethod"` to absorb the touchpad motion events (bypassing `FLAG_NOT_TOUCHABLE` at 0x205214).
     * When using a zero-window physical keyboard IME like PhysiBoard, no `"InputMethod"` window
     * exists, causing `findTouchedWindowAt` to fall through to the foreground app window and scroll it.
     *
     * When `debug.sf.agui_input_status == "0"` (or for any normal `AINPUT_SOURCE_TOUCHSCREEN` touch),
     * `findTouchedWindowAt` skips the `"InputMethod"` check and obeys `FLAG_NOT_TOUCHABLE`, ignoring
     * this 1x1 window completely.
     */
    private fun attachSinkWindow() {
        if (isSinkAttached) return
        val view = object : View(service) {
            override fun onTouchEvent(event: MotionEvent?): Boolean = true
            override fun onGenericMotionEvent(event: MotionEvent?): Boolean = true
        }
        val params = WindowManager.LayoutParams(
            1,
            1,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = SINK_WINDOW_TITLE
        }
        try {
            windowManager.addView(view, params)
            sinkView = view
            isSinkAttached = true
            Log.d(TAG, "Attached InputMethod touchpad sink overlay")
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "BadTokenException attaching sink overlay", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException attaching sink overlay", e)
        }
    }

    private fun detachSinkWindow() {
        if (!isSinkAttached) return
        val view = sinkView ?: return
        try {
            windowManager.removeView(view)
            Log.d(TAG, "Detached InputMethod touchpad sink overlay")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException removing sink overlay", e)
        }
        sinkView = null
        isSinkAttached = false
    }

    private fun requestPropSync() {
        serviceScope.launch(Dispatchers.IO) {
            propSyncMutex.withLock {
                val targetVal = if (isTouchpadClaimed && isEnabled) "1" else "0"
                if (readNativeAguiInputStatus() == targetVal) return@withLock
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
        isEnabled = false
        debounceJob?.cancel()
        debounceJob = null
        imeWatchdogJob?.cancel()
        imeWatchdogJob = null
        isDebounceClaimActive = false
        isTextFieldFocusClaimActive = false
        detachSinkWindow()
        if (isTouchpadClaimed || readNativeAguiInputStatus() == "1") {
            claimTouchpad(false)
        }
        repository.setTypingActive(false)
        repository.setTextFieldFocused(false)
        repository.setScrollSuppressed(false)
    }
}

package com.t2e.toolbox.eqbirvin.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import com.t2e.toolbox.eqbirvin.data.FlashlightIndicatorColor
import com.t2e.toolbox.eqbirvin.data.FlashlightRepository
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val TAG = "FlashlightOverlayMgr"

class FlashlightOverlayManager(
    private val service: AccessibilityService,
    private val windowManager: WindowManager,
    private val flashlightRepo: FlashlightRepository,
    private val settingsRepo: SettingsRepository
) {
    private var indicatorView: FlashlightIndicatorView? = null
    private var isAttached = false
    private var currentParams: WindowManager.LayoutParams? = null
    private var currentHeight = 0
    private var observerJob: Job? = null
    private var currentTapToDismiss = false

    fun start(scope: CoroutineScope) {
        stop()
        observerJob = scope.launch {
            combine(
                settingsRepo.isFlashlightIndicatorEnabled,
                flashlightRepo.isFlashlightOn,
                settingsRepo.isFlashlightIndicatorTapToDismiss,
                settingsRepo.flashlightIndicatorColor
            ) { isEnabled, isTorchOn, tapToDismiss, color ->
                IndicatorState(isEnabled && isTorchOn, tapToDismiss, color)
            }.collect { state ->
                handleStateChange(scope, state)
            }
        }
    }

    private data class IndicatorState(
        val shouldShow: Boolean,
        val tapToDismiss: Boolean,
        val color: FlashlightIndicatorColor
    )

    private fun handleStateChange(scope: CoroutineScope, state: IndicatorState) {
        if (state.shouldShow) {
            if (!isAttached) {
                attach(scope, state.tapToDismiss, state.color)
            } else {
                updateExisting(scope, state.tapToDismiss, state.color)
            }
        } else {
            if (isAttached) {
                detach()
            }
        }
    }

    private fun getStatusBarHeight(): Int {
        val metrics = windowManager.currentWindowMetrics
        val insets = metrics.windowInsets.getInsets(WindowInsets.Type.statusBars())
        val top = insets.top
        if (top > 0) return top
        val resId = service.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) service.resources.getDimensionPixelSize(resId) else 0
    }

    private fun createLayoutParams(height: Int, tapToDismiss: Boolean): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (!tapToDismiss) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (height > 0) height else WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            setFitInsetsTypes(0)
        }
    }

    private fun attach(scope: CoroutineScope, tapToDismiss: Boolean, color: FlashlightIndicatorColor) {
        if (isAttached) return
        val height = getStatusBarHeight()
        currentHeight = height
        currentTapToDismiss = tapToDismiss

        val view = FlashlightIndicatorView(service).also {
            it.updateColor(color.colorArgb)
            it.setTapToDismiss(tapToDismiss) {
                scope.launch { flashlightRepo.setFlashlight(false) }
            }
            indicatorView = it
        }

        val params = createLayoutParams(height, tapToDismiss)
        currentParams = params

        try {
            windowManager.addView(view, params)
            isAttached = true
            Log.d(TAG, "Attached flashlight indicator overlay to status bar center")
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "BadTokenException attaching flashlight indicator overlay", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException attaching flashlight indicator overlay", e)
        }
    }

    private fun updateExisting(scope: CoroutineScope, tapToDismiss: Boolean, color: FlashlightIndicatorColor) {
        val view = indicatorView ?: return
        view.updateColor(color.colorArgb)

        if (currentTapToDismiss != tapToDismiss) {
            currentTapToDismiss = tapToDismiss
            view.setTapToDismiss(tapToDismiss) {
                scope.launch { flashlightRepo.setFlashlight(false) }
            }
            val height = getStatusBarHeight()
            val params = createLayoutParams(height, tapToDismiss)
            currentParams = params
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "IllegalArgumentException updating flashlight overlay layout", e)
            }
        }
    }

    fun onConfigurationChanged() {
        if (!isAttached) return
        val view = indicatorView ?: return
        val newHeight = getStatusBarHeight()
        if (newHeight > 0 && newHeight != currentHeight) {
            currentHeight = newHeight
            currentParams?.height = newHeight
            try {
                windowManager.updateViewLayout(view, currentParams)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "IllegalArgumentException updating flashlight overlay on config change", e)
            }
        }
    }

    fun detach() {
        if (!isAttached) return
        val view = indicatorView ?: return
        try {
            windowManager.removeView(view)
            Log.d(TAG, "Detached flashlight indicator overlay from status bar")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException removing flashlight indicator overlay", e)
        } finally {
            indicatorView = null
            isAttached = false
            currentParams = null
        }
    }

    fun stop() {
        observerJob?.cancel()
        observerJob = null
        detach()
    }
}

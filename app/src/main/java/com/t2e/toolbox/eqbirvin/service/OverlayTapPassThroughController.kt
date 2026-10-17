package com.t2e.toolbox.eqbirvin.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager

private const val TAG = "OverlayTapPassThrough"

class OverlayTapPassThroughController(
    private val service: AccessibilityService,
    private val windowManagerProvider: () -> WindowManager?,
    private val mainHandler: Handler
) {
    private var isPassingThroughTap = false
    private val restoreTouchableRunnable = Runnable {
        restoreTouchable()
    }
    private var targetViewProvider: (() -> View?)? = null
    private var layoutParamsProvider: (() -> WindowManager.LayoutParams?)? = null

    fun bind(
        viewProvider: () -> View?,
        paramsProvider: () -> WindowManager.LayoutParams?
    ) {
        targetViewProvider = viewProvider
        layoutParamsProvider = paramsProvider
    }

    fun passThroughTap(x: Float, y: Float) {
        if (isPassingThroughTap) return
        val view = targetViewProvider?.invoke() ?: return
        val params = layoutParamsProvider?.invoke() ?: return
        val wm = windowManagerProvider() ?: return

        isPassingThroughTap = true
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        try {
            wm.updateViewLayout(view, params)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException setting FLAG_NOT_TOUCHABLE", e)
            isPassingThroughTap = false
            return
        }

        mainHandler.postDelayed(restoreTouchableRunnable, 250)

        try {
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x, y + 1f)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 40)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            val dispatched = service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    mainHandler.removeCallbacks(restoreTouchableRunnable)
                    restoreTouchable()
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    mainHandler.removeCallbacks(restoreTouchableRunnable)
                    restoreTouchable()
                }
            }, null)

            if (!dispatched) {
                Log.w(TAG, "dispatchGesture returned false")
                mainHandler.removeCallbacks(restoreTouchableRunnable)
                restoreTouchable()
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException in passThroughTap gesture", e)
            mainHandler.removeCallbacks(restoreTouchableRunnable)
            restoreTouchable()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in passThroughTap gesture", e)
            mainHandler.removeCallbacks(restoreTouchableRunnable)
            restoreTouchable()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException in passThroughTap gesture", e)
            mainHandler.removeCallbacks(restoreTouchableRunnable)
            restoreTouchable()
        }
    }

    fun restoreTouchable() {
        if (!isPassingThroughTap) return
        val view = targetViewProvider?.invoke() ?: return
        val params = layoutParamsProvider?.invoke() ?: return
        val wm = windowManagerProvider() ?: return

        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        try {
            wm.updateViewLayout(view, params)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException restoring touchable flag", e)
        } finally {
            isPassingThroughTap = false
        }
    }

    fun cleanup() {
        mainHandler.removeCallbacks(restoreTouchableRunnable)
        restoreTouchable()
    }
}

package com.t2e.toolbox.eqbirvin.service

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

class StatusbarOverlayView(
    context: Context,
    private val onSwipeDetected: () -> Unit,
    private val onTapDetected: (Float, Float) -> Unit = { _, _ -> }
) : View(context) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val swipeThreshold = 2 * touchSlop

    private var downX = 0f
    private var downY = 0f
    private var gestureFired = false
    private var velocityTracker: VelocityTracker? = null

    init {
        isClickable = true
        isFocusable = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                gestureFired = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!gestureFired) {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (dy >= swipeThreshold && abs(dy) > abs(dx)) {
                        gestureFired = true
                        triggerSwipe()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!gestureFired) {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val yVel = velocityTracker?.yVelocity ?: 0f
                    val xVel = velocityTracker?.xVelocity ?: 0f
                    val dy = event.rawY - downY
                    if (yVel > minFlingVelocity && dy > touchSlop && abs(yVel) > abs(xVel)) {
                        gestureFired = true
                        triggerSwipe()
                    } else {
                        onTapDetected(downX, downY)
                    }
                }
                recycleVelocityTracker()
                gestureFired = false
            }
            MotionEvent.ACTION_CANCEL -> {
                recycleVelocityTracker()
                gestureFired = false
            }
        }
        return true
    }

    private fun triggerSwipe() {
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onSwipeDetected()
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }
}

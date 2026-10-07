package com.t2e.toolbox.eqbirvin.gesture

import android.os.SystemClock

enum class PullDownAction {
    NONE,
    OPEN_NOTIFICATIONS,
    OPEN_QUICK_SETTINGS
}

enum class DetectedShadeState {
    CLOSED,
    NOTIFICATIONS,
    QUICK_SETTINGS
}

fun resolveSmartAction(shadeState: DetectedShadeState): PullDownAction {
    return when (shadeState) {
        DetectedShadeState.CLOSED -> PullDownAction.OPEN_NOTIFICATIONS
        DetectedShadeState.NOTIFICATIONS -> PullDownAction.OPEN_QUICK_SETTINGS
        DetectedShadeState.QUICK_SETTINGS -> PullDownAction.OPEN_NOTIFICATIONS
    }
}

class PullDownStateMachine(
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
    private val timeoutMs: Long = 3000L
) {
    private var lastSwipeTime: Long = 0L
    private var isArmed: Boolean = false

    fun onSwipe(): PullDownAction {
        val now = clock()
        return if (isArmed && (now - lastSwipeTime) <= timeoutMs) {
            isArmed = false
            PullDownAction.OPEN_QUICK_SETTINGS
        } else {
            isArmed = true
            lastSwipeTime = now
            PullDownAction.OPEN_NOTIFICATIONS
        }
    }

    fun reset() {
        isArmed = false
        lastSwipeTime = 0L
    }

    val isWaitingForSecondSwipe: Boolean
        get() = isArmed && (clock() - lastSwipeTime) <= timeoutMs
}

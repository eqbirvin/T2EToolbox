package com.t2e.toolbox.eqbirvin.gesture

const val QS_SECOND_SWIPE_WINDOW_MS = 4000L

enum class PullDownAction {
    NONE,
    OPEN_NOTIFICATIONS,
    OPEN_QUICK_SETTINGS
}

class PullDownStateMachine(
    private val clock: () -> Long
) {
    private var armedUntilMs: Long = 0L

    fun onSwipe(now: Long = clock()): PullDownAction {
        return if (now <= armedUntilMs) {
            armedUntilMs = 0L
            PullDownAction.OPEN_QUICK_SETTINGS
        } else {
            armedUntilMs = now + QS_SECOND_SWIPE_WINDOW_MS
            PullDownAction.OPEN_NOTIFICATIONS
        }
    }

    fun reset() {
        armedUntilMs = 0L
    }

    val isArmed: Boolean
        get() = clock() <= armedUntilMs
}

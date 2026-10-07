package com.t2e.toolbox.eqbirvin.gesture

enum class PullDownAction {
    NONE,
    OPEN_NOTIFICATIONS,
    OPEN_QUICK_SETTINGS
}

class PullDownStateMachine {
    private var isArmed: Boolean = false

    fun onSwipe(): PullDownAction {
        return if (isArmed) {
            isArmed = false
            PullDownAction.OPEN_QUICK_SETTINGS
        } else {
            isArmed = true
            PullDownAction.OPEN_NOTIFICATIONS
        }
    }

    fun reset() {
        isArmed = false
    }

    val isWaitingForSecondSwipe: Boolean
        get() = isArmed
}

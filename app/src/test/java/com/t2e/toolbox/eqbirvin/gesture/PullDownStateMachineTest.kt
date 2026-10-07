package com.t2e.toolbox.eqbirvin.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class PullDownStateMachineTest {

    @Test
    fun testSwipeAlternationAndReset() {
        val stateMachine = PullDownStateMachine()

        // 1. First swipe -> notifications
        val firstAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_NOTIFICATIONS, firstAction)

        // 2. Second swipe (no timeout) -> QS
        val secondAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_QUICK_SETTINGS, secondAction)

        // 3. Third swipe -> notifications again
        val thirdAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_NOTIFICATIONS, thirdAction)

        // 4. Reset -> clears armed state back to notifications
        stateMachine.reset()
        val fourthAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_NOTIFICATIONS, fourthAction)
    }
}

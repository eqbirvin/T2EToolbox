package com.t2e.toolbox.eqbirvin.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class PullDownStateMachineTest {

    @Test
    fun testSwipeTransitionsAndTiming() {
        var currentTime = 1000L
        val stateMachine = PullDownStateMachine(clock = { currentTime })

        // 1. First swipe -> notifications
        val firstAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_NOTIFICATIONS, firstAction)

        // 2. Second swipe at 3999 ms -> QS
        currentTime += 3999L
        val secondAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_QUICK_SETTINGS, secondAction)

        // 3. Third swipe right after QS -> notifications
        currentTime += 10L
        val thirdAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_NOTIFICATIONS, thirdAction)

        // 4. Second swipe after 4001 ms (window expired) -> notifications
        currentTime += 4001L
        val fourthAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_NOTIFICATIONS, fourthAction)
    }
}

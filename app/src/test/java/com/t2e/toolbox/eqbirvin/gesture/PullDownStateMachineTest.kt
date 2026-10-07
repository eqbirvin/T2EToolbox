package com.t2e.toolbox.eqbirvin.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class PullDownStateMachineTest {

    @Test
    fun testSwipeTransitionsAndTiming() {
        var currentTime = 1000L
        val stateMachine = PullDownStateMachine(clock = { currentTime }, timeoutMs = 3000L)

        // 1. First swipe -> notifications
        val firstAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_NOTIFICATIONS, firstAction)

        // 2. Second swipe at 2999 ms (within 3s window) -> QS
        currentTime += 2999L
        val secondAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_QUICK_SETTINGS, secondAction)

        // 3. Third swipe right after QS -> notifications
        currentTime += 10L
        val thirdAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_NOTIFICATIONS, thirdAction)

        // 4. Second swipe after 3001 ms (window expired) -> resets to notifications
        currentTime += 3001L
        val fourthAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_NOTIFICATIONS, fourthAction)

        // 5. Reset -> clears armed state back to notifications
        stateMachine.reset()
        val fifthAction = stateMachine.onSwipe()
        assertEquals(PullDownAction.OPEN_NOTIFICATIONS, fifthAction)
    }
}

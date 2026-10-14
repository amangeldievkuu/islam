package com.sunnahcompanion.app.feature.tasbih

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TasbihCounterTest {

    @Test
    fun hundredTarget_doublePulseAt33And66_longAt100() {
        var count = 0
        val feedback = (1..100).associateWith {
            val step = TasbihCounter.next(count, 100)
            count = step.count
            step.feedback
        }
        assertEquals(TasbihFeedback.MILESTONE, feedback[33])
        assertEquals(TasbihFeedback.MILESTONE, feedback[66])
        assertEquals(TasbihFeedback.MILESTONE, feedback[99])
        assertEquals(TasbihFeedback.COMPLETE, feedback[100])
        assertEquals(96, feedback.values.count { it == TasbihFeedback.TICK })
    }

    @Test
    fun thirtyThreeTarget_completesAt33WithLongPulse() {
        val step = TasbihCounter.next(32, 33)
        assertEquals(33, step.count)
        assertTrue(step.completedRound)
        assertEquals(TasbihFeedback.COMPLETE, step.feedback)
    }

    @Test
    fun tapAfterCompletedRound_startsNextRoundAtOne() {
        val step = TasbihCounter.next(33, 33)
        assertEquals(1, step.count)
        assertFalse(step.completedRound)
        assertEquals(TasbihFeedback.TICK, step.feedback)
    }
}

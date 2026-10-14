package com.sunnahcompanion.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitMasteryPolicyTest {

    @Test
    fun masteredFromTheTwentyFirstCompletedDay() {
        assertFalse(HabitMasteryPolicy.isMastered(0))
        assertFalse(HabitMasteryPolicy.isMastered(20))
        assertTrue(HabitMasteryPolicy.isMastered(21))
        assertTrue(HabitMasteryPolicy.isMastered(30))
    }

    @Test
    fun daysRemainingNeverNegative() {
        assertEquals(21, HabitMasteryPolicy.daysRemaining(0))
        assertEquals(1, HabitMasteryPolicy.daysRemaining(20))
        assertEquals(0, HabitMasteryPolicy.daysRemaining(25))
    }
}

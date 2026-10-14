package com.sunnahcompanion.app.data.model

/**
 * Rules of the "Active 3" system: master a few Sunnahs at a time, and a habit graduates after
 * [MASTERY_DAYS] completed days since it was activated. Missed days never reset the count
 * (Yassiru wa la tu'assiru: make it easy).
 */
object HabitMasteryPolicy {
    const val MAX_ACTIVE = 3
    const val MASTERY_DAYS = 21

    fun isMastered(completedDays: Int): Boolean = completedDays >= MASTERY_DAYS

    /** Days still to go, never negative. */
    fun daysRemaining(completedDays: Int): Int = (MASTERY_DAYS - completedDays).coerceAtLeast(0)
}

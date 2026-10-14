package com.sunnahcompanion.app.core.database.model

/** Lifecycle of an "Active 3" habit slot. Persisted by [name]. */
enum class HabitStatus {
    /** Occupies one of the three slots and counts towards mastery. */
    ACTIVE,

    /** Graduated after enough completed days; frees its slot. */
    MASTERED,
}

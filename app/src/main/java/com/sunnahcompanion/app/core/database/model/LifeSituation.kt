package com.sunnahcompanion.app.core.database.model

/**
 * When an item is relevant: a physical trigger or state of heart the user can pick as a chip
 * ([showAsChip]), or a time window the "Now" stream derives from the clock. Persisted by
 * [name], so never rename an entry without a migration; the seed JSON references these names.
 */
enum class LifeSituation(val showAsChip: Boolean) {
    LEAVING_HOME(showAsChip = true),
    ENTERING_HOME(showAsChip = true),
    DRIVING(showAsChip = true),
    ANXIOUS(showAsChip = true),
    RAIN(showAsChip = true),
    HARDSHIP(showAsChip = true),
    GRATITUDE(showAsChip = true),
    BEFORE_SLEEP(showAsChip = true),
    MORNING(showAsChip = false),
    EVENING(showAsChip = false),
    AFTER_FARD_PRAYER(showAsChip = false),
    FRIDAY(showAsChip = false),
    ;

    companion object {
        val chips: List<LifeSituation> = entries.filter { it.showAsChip }
    }
}

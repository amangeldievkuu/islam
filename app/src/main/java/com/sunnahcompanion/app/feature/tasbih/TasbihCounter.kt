package com.sunnahcompanion.app.feature.tasbih

enum class TasbihFeedback { TICK, MILESTONE, COMPLETE }

data class TasbihStep(val count: Int, val completedRound: Boolean, val feedback: TasbihFeedback)

/** Pure counting rules, unit-tested; the ViewModel only wires them to state and side effects. */
object TasbihCounter {
    const val MILESTONE = 33

    /**
     * Counting continues from [current]; after a finished round, the next tap starts at 1.
     * Every 33rd count before the target gets a double pulse (33 and 66 on a 100 target),
     * reaching the target gets a long one.
     */
    fun next(current: Int, target: Int): TasbihStep {
        val count = if (current >= target) 1 else current + 1
        val feedback = when {
            count == target -> TasbihFeedback.COMPLETE
            count % MILESTONE == 0 -> TasbihFeedback.MILESTONE
            else -> TasbihFeedback.TICK
        }
        return TasbihStep(count = count, completedRound = count == target, feedback = feedback)
    }
}

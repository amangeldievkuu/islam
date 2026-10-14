package com.sunnahcompanion.app.trigger

import java.time.Duration

/** Pure rules deciding whether a contextual trigger should interrupt the user. */
object TriggerPolicy {
    /** Don't prompt for a dua the user already read this recently. */
    val RECENTLY_READ: Duration = Duration.ofHours(3)

    /** Absorbs GPS jitter at the fence edge and Bluetooth + activity firing for the same drive. */
    val PROMPT_COOLDOWN: Duration = Duration.ofMinutes(20)

    fun shouldPrompt(nowMillis: Long, lastLoggedAt: Long?, lastPromptAt: Long?): Boolean {
        if (lastLoggedAt != null && nowMillis - lastLoggedAt < RECENTLY_READ.toMillis()) return false
        if (lastPromptAt != null && nowMillis - lastPromptAt < PROMPT_COOLDOWN.toMillis()) return false
        return true
    }
}

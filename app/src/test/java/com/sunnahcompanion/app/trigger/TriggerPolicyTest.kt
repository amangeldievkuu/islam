package com.sunnahcompanion.app.trigger

import java.time.Duration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerPolicyTest {
    private val now = 1_000_000_000L
    private fun ago(d: Duration) = now - d.toMillis()

    @Test
    fun promptsWhenNeverReadOrPrompted() {
        assertTrue(TriggerPolicy.shouldPrompt(now, lastLoggedAt = null, lastPromptAt = null))
    }

    @Test
    fun skipsWhenReadWithinThreeHours() {
        assertFalse(TriggerPolicy.shouldPrompt(now, ago(Duration.ofHours(2).plusMinutes(59)), null))
        assertTrue(TriggerPolicy.shouldPrompt(now, ago(Duration.ofHours(3)), null))
    }

    @Test
    fun cooldownAbsorbsDuplicateTriggers() {
        assertFalse(TriggerPolicy.shouldPrompt(now, null, ago(Duration.ofMinutes(5))))
        assertTrue(TriggerPolicy.shouldPrompt(now, null, ago(Duration.ofMinutes(21))))
    }
}

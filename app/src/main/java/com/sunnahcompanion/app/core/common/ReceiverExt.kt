package com.sunnahcompanion.app.core.common

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Runs [block] off the main thread while keeping the broadcast alive until it finishes.
 * The system allows roughly 10 seconds, which is ample for a Room write or a notification.
 */
fun BroadcastReceiver.launchAsync(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
    val pendingResult = goAsync()
    scope.launch {
        try {
            block()
        } finally {
            pendingResult.finish()
        }
    }
}

package com.sunnahcompanion.app.trigger.driving

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.sunnahcompanion.app.core.common.ApplicationScope
import com.sunnahcompanion.app.core.common.launchAsync
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.trigger.ContextualTriggerHandler
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** Receives Activity Recognition transitions; car Bluetooth is handled by [BluetoothCarReceiver]. */
@AndroidEntryPoint
class DrivingDetectionReceiver : BroadcastReceiver() {

    @Inject lateinit var handler: ContextualTriggerHandler

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val now = SystemClock.elapsedRealtimeNanos()
        val startedDriving = result.transitionEvents.any { event ->
            event.activityType == DetectedActivity.IN_VEHICLE &&
                event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER &&
                // Re-registration can replay an old transition; only react to fresh ones.
                now - event.elapsedRealTimeNanos < MAX_EVENT_AGE_NANOS
        }
        if (startedDriving) launchAsync(scope) { handler.onDrivingStarted(TriggerSource.DRIVING) }
    }

    companion object {
        const val ACTION = "com.sunnahcompanion.app.action.ACTIVITY_TRANSITION"
        private val MAX_EVENT_AGE_NANOS = TimeUnit.MINUTES.toNanos(10)
    }
}

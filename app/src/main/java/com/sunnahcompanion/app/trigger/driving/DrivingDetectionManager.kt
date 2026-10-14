package com.sunnahcompanion.app.trigger.driving

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.sunnahcompanion.app.core.common.AppPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/** Subscribes to the low-power Activity Recognition Transition API for IN_VEHICLE entry. */
@Singleton
class DrivingDetectionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissions: AppPermissions,
) {
    private val client = ActivityRecognition.getClient(context)

    // Must be mutable: Play services attaches the ActivityTransitionResult.
    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, DrivingDetectionReceiver::class.java).setAction(DrivingDetectionReceiver.ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    val hasPermissions: Boolean get() = permissions.hasActivityRecognition

    @SuppressLint("MissingPermission") // Checked via hasPermissions.
    suspend fun register(): Result<Unit> = runCatching {
        check(hasPermissions) { "Physical activity permission is required" }
        val request = ActivityTransitionRequest(
            listOf(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
            ),
        )
        client.requestActivityTransitionUpdates(request, pendingIntent).await()
    }

    @SuppressLint("MissingPermission")
    suspend fun unregister(): Result<Unit> = runCatching {
        if (hasPermissions) client.removeActivityTransitionUpdates(pendingIntent).await()
    }
}

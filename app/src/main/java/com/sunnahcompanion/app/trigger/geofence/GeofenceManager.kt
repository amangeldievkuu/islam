package com.sunnahcompanion.app.trigger.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.sunnahcompanion.app.core.common.AppPermissions
import com.sunnahcompanion.app.core.datastore.HomeLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/** Registers a single exit geofence around the user's home. */
@Singleton
class GeofenceManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissions: AppPermissions,
) {
    private val client = LocationServices.getGeofencingClient(context)

    // Must be mutable: Play services fills in the GeofencingEvent extras.
    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java).setAction(GeofenceBroadcastReceiver.ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    val hasPermissions: Boolean get() = permissions.hasFineLocation && permissions.hasBackgroundLocation

    @SuppressLint("MissingPermission") // Checked via hasPermissions.
    suspend fun register(home: HomeLocation): Result<Unit> = runCatching {
        check(hasPermissions) { "Location \"Allow all the time\" is required" }
        val geofence = Geofence.Builder()
            .setRequestId(HOME_REQUEST_ID)
            .setCircularRegion(home.latitude, home.longitude, RADIUS_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(RESPONSIVENESS_MS)
            .build()
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0) // Don't fire for the state we're already in.
            .addGeofence(geofence)
            .build()
        client.addGeofences(request, pendingIntent).await()
    }

    suspend fun unregister(): Result<Unit> = runCatching {
        client.removeGeofences(pendingIntent).await()
    }

    private companion object {
        const val HOME_REQUEST_ID = "home"
        const val RADIUS_METERS = 100f
        const val RESPONSIVENESS_MS = 60_000
    }
}

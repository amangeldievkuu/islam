package com.sunnahcompanion.app.trigger.geofence

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sunnahcompanion.app.core.common.AppPermissions
import com.sunnahcompanion.app.core.datastore.HomeLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/** One-shot, high-accuracy fix used to save the user's home. */
class CurrentLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissions: AppPermissions,
) {
    @SuppressLint("MissingPermission") // Checked below.
    suspend fun current(): HomeLocation? {
        if (!permissions.hasFineLocation) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            ?: client.lastLocation.await()
        return location?.let { HomeLocation(it.latitude, it.longitude) }
    }
}

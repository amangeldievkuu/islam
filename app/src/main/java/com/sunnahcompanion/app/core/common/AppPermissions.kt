package com.sunnahcompanion.app.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Point-in-time runtime permission checks (minSdk 36: every permission here is runtime). */
@Singleton
class AppPermissions @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val hasNotifications get() = has(Manifest.permission.POST_NOTIFICATIONS)
    val hasFineLocation get() = has(Manifest.permission.ACCESS_FINE_LOCATION)
    val hasBackgroundLocation get() = has(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    val hasActivityRecognition get() = has(Manifest.permission.ACTIVITY_RECOGNITION)
    val hasBluetoothConnect get() = has(Manifest.permission.BLUETOOTH_CONNECT)

    private fun has(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

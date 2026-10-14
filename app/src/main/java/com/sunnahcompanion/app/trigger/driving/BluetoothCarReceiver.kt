package com.sunnahcompanion.app.trigger.driving

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunnahcompanion.app.core.common.AppPermissions
import com.sunnahcompanion.app.core.common.ApplicationScope
import com.sunnahcompanion.app.core.common.launchAsync
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.core.datastore.AppPreferencesDataSource
import com.sunnahcompanion.app.trigger.ContextualTriggerHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

/**
 * Fires the travel dua when a Bluetooth device the user marked as their car connects.
 * ACTION_ACL_CONNECTED is exempt from implicit-broadcast limits, so a manifest receiver works.
 */
@AndroidEntryPoint
class BluetoothCarReceiver : BroadcastReceiver() {

    @Inject lateinit var handler: ContextualTriggerHandler

    @Inject lateinit var preferences: AppPreferencesDataSource

    @Inject lateinit var permissions: AppPermissions

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED || !permissions.hasBluetoothConnect) return
        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
        val address = try {
            device.address
        } catch (_: SecurityException) {
            return
        }
        launchAsync(scope) {
            if (address in preferences.userPreferences.first().carDeviceAddresses) {
                handler.onDrivingStarted(TriggerSource.BLUETOOTH)
            }
        }
    }
}

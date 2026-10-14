package com.sunnahcompanion.app.trigger.driving

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import com.sunnahcompanion.app.core.common.AppPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class PairedDevice(val name: String, val address: String)

/** Lists bonded Bluetooth devices so the user can mark which ones are their car. */
class PairedDevices @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissions: AppPermissions,
) {
    @SuppressLint("MissingPermission") // Checked below.
    fun list(): List<PairedDevice> {
        if (!permissions.hasBluetoothConnect) return emptyList()
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
        return adapter.bondedDevices.orEmpty()
            .map { PairedDevice(name = it.name ?: it.address, address = it.address) }
            .sortedBy { it.name.lowercase() }
    }
}

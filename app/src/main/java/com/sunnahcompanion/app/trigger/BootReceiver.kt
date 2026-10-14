package com.sunnahcompanion.app.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunnahcompanion.app.core.common.ApplicationScope
import com.sunnahcompanion.app.core.common.launchAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** Geofences, activity transitions and alarms do not survive a reboot or an app update. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var registrar: TriggerRegistrar

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> launchAsync(scope) { registrar.sync() }
        }
    }
}

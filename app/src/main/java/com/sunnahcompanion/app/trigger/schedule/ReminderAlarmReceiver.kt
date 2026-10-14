package com.sunnahcompanion.app.trigger.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunnahcompanion.app.core.common.ApplicationScope
import com.sunnahcompanion.app.core.common.launchAsync
import com.sunnahcompanion.app.core.database.seed.ContentSeeder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderScheduler

    @Inject lateinit var seeder: ContentSeeder

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        launchAsync(scope) {
            seeder.ensureSeeded()
            scheduler.onAlarm()
        }
    }
}

package com.sunnahcompanion.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sunnahcompanion.app.core.common.ApplicationScope
import com.sunnahcompanion.app.core.common.launchAsync
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.data.repository.ContentRepository
import com.sunnahcompanion.app.notification.DuaNotifier
import com.sunnahcompanion.app.trigger.ContextualTriggerHandler
import com.sunnahcompanion.app.trigger.schedule.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * Simulates sensor events in debug builds, exercising the same handler the real receivers use
 * (including the 3-hour and cooldown rules):
 *
 *   adb shell am broadcast -a com.sunnahcompanion.app.debug.SIMULATE --es trigger leaving_home
 *   adb shell am broadcast -a com.sunnahcompanion.app.debug.SIMULATE --es trigger driving
 *   adb shell am broadcast -a com.sunnahcompanion.app.debug.SIMULATE --es trigger bluetooth
 *   adb shell am broadcast -a com.sunnahcompanion.app.debug.SIMULATE --es trigger reminders
 *
 * Or post an item's notification directly, bypassing the 3-hour and cooldown rules:
 *
 *   adb shell am broadcast -a com.sunnahcompanion.app.debug.SIMULATE --es trigger show --es item dua_riding_vehicle
 */
@AndroidEntryPoint
class DebugTriggerReceiver : BroadcastReceiver() {

    @Inject lateinit var handler: ContextualTriggerHandler

    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject lateinit var contentRepository: ContentRepository

    @Inject lateinit var notifier: DuaNotifier

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val trigger = intent.getStringExtra("trigger")
        Log.i(TAG, "Simulating $trigger")
        launchAsync(scope) {
            when (trigger) {
                "leaving_home" -> handler.onLeftHome()
                "driving" -> handler.onDrivingStarted(TriggerSource.DRIVING)
                "bluetooth" -> handler.onDrivingStarted(TriggerSource.BLUETOOTH)
                "reminders" -> reminderScheduler.onAlarm()
                "show" -> intent.getStringExtra("item")
                    ?.let { contentRepository.getItem(it) }
                    ?.let { notifier.showDua(it, TriggerSource.DRIVING) }
                else -> Log.w(TAG, "Unknown trigger: $trigger")
            }
        }
    }

    private companion object {
        const val TAG = "DebugTrigger"
    }
}

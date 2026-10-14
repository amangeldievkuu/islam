package com.sunnahcompanion.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sunnahcompanion.app.core.common.ApplicationScope
import com.sunnahcompanion.app.core.database.seed.ContentSeeder
import com.sunnahcompanion.app.notification.DuaNotifier
import com.sunnahcompanion.app.trigger.TriggerRegistrar
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class SunnahCompanionApp : Application(), Configuration.Provider {

    @Inject lateinit var contentSeeder: ContentSeeder

    @Inject lateinit var notifier: DuaNotifier

    @Inject lateinit var triggerRegistrar: TriggerRegistrar

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    /** On-demand WorkManager initialisation; the default initializer is removed in the manifest. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        applicationScope.launch { contentSeeder.ensureSeeded() }
        triggerRegistrar.start()
    }
}

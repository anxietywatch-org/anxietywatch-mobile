package com.anxietywatch.mobile

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Configuration.Provider es obligatorio para que WorkManager sepa usar HiltWorkerFactory --
 * sin esto, BackupSyncWorker (que tiene un @AssistedInject con SessionRepository) truena
 * en tiempo de ejecucion con "no zero-arg constructor" en cuanto WorkManager intenta
 * instanciarlo, aunque compile perfecto. Es el problema clasico de mezclar Hilt + WorkManager.
 */
@HiltAndroidApp
class AnxietyWatchApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

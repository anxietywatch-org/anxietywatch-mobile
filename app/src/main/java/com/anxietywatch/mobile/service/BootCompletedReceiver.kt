package com.anxietywatch.mobile.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anxietywatch.mobile.data.remote.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * E5 del documento atomico: revive el monitoreo si el telefono se reinicio, PERO solo si
 * el token de 30 min todavia no expiro.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (sessionRepository.hasValidSession()) {
                    MonitoringForegroundService.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

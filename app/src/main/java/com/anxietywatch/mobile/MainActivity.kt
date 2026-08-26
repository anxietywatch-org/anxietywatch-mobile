package com.anxietywatch.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import com.anxietywatch.mobile.data.remote.SessionExpiryNotifier
import com.anxietywatch.mobile.data.remote.SessionRepository
import com.anxietywatch.mobile.navigation.AnxietyWatchNavHost
import com.anxietywatch.mobile.push.CaregiverPushService
import com.anxietywatch.mobile.push.CaregiverAlertPayload
import com.anxietywatch.mobile.ui.common.ScreenScaffold
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingCriticalAlertPayload by mutableStateOf<CaregiverAlertPayload?>(null)

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var sessionExpiryNotifier: SessionExpiryNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readCriticalAlertIntent(intent)
        enableEdgeToEdge()
        setContent {
            AnxietyWatchTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScreenScaffold {
                    AnxietyWatchNavHost(
                        sessionRepository = sessionRepository,
                        sessionExpiryNotifier = sessionExpiryNotifier,
                        criticalAlertPayload = pendingCriticalAlertPayload,
                        onCriticalAlertConsumed = ::consumeCriticalAlertIntent,
                    )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readCriticalAlertIntent(intent)
    }

    private fun readCriticalAlertIntent(intent: Intent?) {
        val eventId = intent?.requiredExtra(CaregiverPushService.EXTRA_EVENT_ID) ?: return
        val patientName = intent.requiredExtra(CaregiverPushService.EXTRA_PATIENT_NAME) ?: return
        val alertMessage = intent.requiredExtra(CaregiverPushService.EXTRA_ALERT_MESSAGE) ?: return
        pendingCriticalAlertPayload = CaregiverAlertPayload(
            eventId = eventId,
            patientName = patientName,
            alertMessage = alertMessage,
            location = intent.getStringExtra(CaregiverPushService.EXTRA_LOCATION)?.takeIf { it.isNotBlank() },
            emergencyPhone = intent.getStringExtra(CaregiverPushService.EXTRA_EMERGENCY_PHONE)
                ?.takeIf { it.isNotBlank() },
        )
    }

    private fun consumeCriticalAlertIntent() {
        pendingCriticalAlertPayload = null
        intent.removeExtra(CaregiverPushService.EXTRA_EVENT_ID)
        intent.removeExtra(CaregiverPushService.EXTRA_PATIENT_NAME)
        intent.removeExtra(CaregiverPushService.EXTRA_ALERT_MESSAGE)
        intent.removeExtra(CaregiverPushService.EXTRA_LOCATION)
        intent.removeExtra(CaregiverPushService.EXTRA_EMERGENCY_PHONE)
    }

    private fun Intent.requiredExtra(key: String): String? =
        getStringExtra(key)?.trim()?.takeIf { it.isNotEmpty() }
}

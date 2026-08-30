package com.anxietywatch.mobile.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.anxietywatch.mobile.MainActivity
import com.anxietywatch.mobile.R
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaregiverAlertPayload(
    val eventId: String,
    val patientName: String,
    val alertMessage: String,
    val location: String? = null,
    val emergencyPhone: String? = null,
)

@AndroidEntryPoint
class CaregiverPushService : FirebaseMessagingService() {

    @Inject
    lateinit var api: AnxietyWatchApi

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            runCatching {
                PushTokenRegistrar.register(api, token)
            }.onSuccess {
                // TODO: quitar este log de diagnóstico temporal.
                Log.d("PushDebug", "Dispositivo registrado correctamente para push.")
            }.onFailure { error ->
                Log.e(TAG, "No se pudo registrar el dispositivo para notificaciones.", error)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // TODO: quitar este log de diagnóstico temporal.
        Log.d(
            "PushDebug",
            "Push recibido. data=${message.data} " +
                "notification=${message.notification?.title}/${message.notification?.body}",
        )
        createNotificationChannel()

        val eventId = message.data[EVENT_ID_KEY]?.trim()?.takeIf { it.isNotEmpty() }
        val patientName = message.data[PATIENT_NAME_KEY]?.trim()?.takeIf { it.isNotEmpty() }
        val alertMessage = message.data[ALERT_MESSAGE_KEY]?.trim()?.takeIf { it.isNotEmpty() }
        val openAppIntent = Intent()
            .setClass(this, MainActivity::class.java)
            .setPackage(packageName)
            .apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (eventId != null && patientName != null && alertMessage != null) {
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_PATIENT_NAME, patientName)
                putExtra(EXTRA_ALERT_MESSAGE, alertMessage)
                message.data[LOCATION_KEY]?.takeIf { it.isNotBlank() }?.let {
                    putExtra(EXTRA_LOCATION, it)
                }
                message.data[EMERGENCY_PHONE_KEY]?.takeIf { it.isNotBlank() }?.let {
                    putExtra(EXTRA_EMERGENCY_PHONE, it)
                }
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            eventId?.hashCode() ?: message.messageId?.hashCode() ?: 0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = message.notification?.title
            ?: message.data[TITLE_KEY]
            ?: "Nueva alerta de AnxietyWatch"
        val body = message.notification?.body
            ?: message.data[BODY_KEY]
            ?: "Abre la aplicación para revisar la información disponible."

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_monitoring_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            getSystemService(NotificationManager::class.java).notify(
                eventId?.hashCode() ?: message.messageId?.hashCode() ?: DEFAULT_NOTIFICATION_ID,
                notification,
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas críticas",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alertas de pacientes vinculados que requieren atención."
            enableVibration(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_EVENT_ID = "caregiver_critical_event_id"
        const val EXTRA_PATIENT_NAME = "caregiver_critical_patient_name"
        const val EXTRA_ALERT_MESSAGE = "caregiver_critical_alert_message"
        const val EXTRA_LOCATION = "caregiver_critical_location"
        const val EXTRA_EMERGENCY_PHONE = "caregiver_critical_emergency_phone"

        private const val CHANNEL_ID = "critical_alerts"
        private const val EVENT_ID_KEY = "eventId"
        private const val PATIENT_NAME_KEY = "patientName"
        private const val ALERT_MESSAGE_KEY = "alertMessage"
        private const val LOCATION_KEY = "location"
        private const val EMERGENCY_PHONE_KEY = "emergencyPhone"
        private const val TITLE_KEY = "title"
        private const val BODY_KEY = "body"
        private const val DEFAULT_NOTIFICATION_ID = 2001
        private const val TAG = "CaregiverPushService"
    }
}

package com.anxietywatch.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.anxietywatch.mobile.MainActivity
import com.anxietywatch.mobile.R
import com.anxietywatch.mobile.data.remote.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Estado E2 del documento atomico: "Segundo plano - Reposo". Mantiene la app viva mientras
 * el usuario no la esta usando, para que el puente con el reloj (PhoneDataLayerListenerService,
 * que Play Services despierta solo) siga teniendo un proceso Android donde correr.
 *
 * Dos mecanismos de reintento, exactamente como describe la seccion 4.8 del documento
 * atomico ("el WorkManager periodico (cada 15 min) O el NetworkCallback (apenas vuelva la
 * senal) se encargan de reintentarlo"):
 * 1) [scheduleBackupSync] -- red de seguridad cada 15 min, por si el callback se pierde
 *    (ej. el sistema mato el proceso completo).
 * 2) [registerNetworkCallback] -- dispara la subida DE INMEDIATO en cuanto vuelve la
 *    conexion, sin esperar el ciclo de 15 min. Esto es lo que hace que "reanude desde
 *    donde se quedo" pase en segundos, no minutos.
 */
@AndroidEntryPoint
class MonitoringForegroundService : Service() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = buildNotification()

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundServiceType)

        scope.launch {
            if (!sessionRepository.hasValidSession()) {
                stopSelf()
                return@launch
            }
            scheduleBackupSync()
            registerNetworkCallback()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        unregisterNetworkCallback()
        scope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, MonitoringForegroundService::class.java)
        restartIntent.setPackage(packageName)
        val restartPendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmService = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmService.set(
            android.app.AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + 1000,
            restartPendingIntent,
        )
        super.onTaskRemoved(rootIntent)
    }

    private fun scheduleBackupSync() {
        val request = PeriodicWorkRequestBuilder<BackupSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(BackupSyncWorker.constraints())
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            BackupSyncWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // "apenas vuelva la senal": lanza un intento inmediato, sin esperar los
                // 15 min del periodico. WorkManager deduplica solo (ExistingWorkPolicy.KEEP)
                // si ya hay uno corriendo en ese instante.
                val immediateRequest = OneTimeWorkRequestBuilder<BackupSyncWorker>()
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                    )
                    .build()
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "${BackupSyncWorker.UNIQUE_WORK_NAME}_immediate",
                    ExistingWorkPolicy.KEEP,
                    immediateRequest,
                )
            }
        }

        connectivityManager.registerNetworkCallback(request, callback)
        networkCallback = callback
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
        networkCallback = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monitoreo de bienestar",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Mantiene activa la conexión con tu reloj mientras usas otras apps."
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoreo de bienestar activo")
            .setContentText("AnxietyWatch está escuchando tu reloj en segundo plano.")
            .setSmallIcon(R.drawable.ic_monitoring_notification)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "monitoring_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, MonitoringForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitoringForegroundService::class.java))
        }
    }
}

private fun CoroutineScope.cancel() {
    (coroutineContext[kotlinx.coroutines.Job])?.cancel()
}

package com.anxietywatch.mobile.fog

import android.util.Log
import com.anxietywatch.mobile.network.DeviceRegisterRequest
import com.anxietywatch.mobile.network.NetworkModule
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AnxietyWatchFcmService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            try {
                if (NetworkModule.getSessionManager().isLoggedIn()) {
                    NetworkModule.api.registerDevice(DeviceRegisterRequest(platform = "android", token = token))
                    Log.i(TAG, "Token FCM real registrado en el backend.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo registrar el token FCM: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.data["type"]
        val patientName = message.data["patientName"] ?: "Tu paciente"

        if (type == "critical_alert") {
            NetworkModule.getSessionManager().apply {
                // Guardamos la alerta real recibida por push para que la UI la muestre
                // en cuanto la app pase a primer plano (o inmediatamente si ya está abierta).
                setPendingCriticalAlert(patientName)
            }
        }
    }

    companion object {
        private const val TAG = "AnxietyWatchFcm"
    }
}
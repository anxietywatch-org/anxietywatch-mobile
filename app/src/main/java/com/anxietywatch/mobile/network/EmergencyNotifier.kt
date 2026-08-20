package com.anxietywatch.mobile.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object EmergencyNotifier {

    /**
     * Manda un SMS real al contacto de emergencia guardado en el perfil del paciente.
     * Requiere permiso SEND_SMS ya concedido y un número real guardado.
     * Devuelve true solo si el SMS se envió de verdad.
     */
    suspend fun notifyEmergencyContactBySms(context: Context, patientFullName: String): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return false

        val phone = try {
            NetworkModule.api.getProfile().emergencyContactPhone
        } catch (e: Exception) {
            null
        }
        if (phone.isNullOrBlank()) return false

        return try {
            val message = "AnxietyWatch: $patientFullName podría estar teniendo un episodio de ansiedad y necesita apoyo. Por favor contáctale."
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Avisa al backend real que hay una crisis (POST /api/v1/sos/trigger).
     * Esto ya dispara -del lado del servidor- la identificación de cuidadores
     * vinculados (CaregiverAlertDispatcher, confirmado real). La entrega del
     * push en sí depende de que el backend tenga Firebase Admin SDK integrado.
     */
    suspend fun triggerRealSos(deviceId: String): Boolean {
        return try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(java.util.Date())
            NetworkModule.api.triggerSos(
                SosTriggerRequest(
                    eventId = UUID.randomUUID().toString(),
                    deviceId = deviceId,
                    triggeredAt = timestamp,
                    source = "MOBILE"
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
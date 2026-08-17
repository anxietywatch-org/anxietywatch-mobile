package com.anxietywatch.mobile.data.bridge

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identidad local que necesita el puente con el reloj: qué dispositivo está vinculado,
 * en qué sesión de monitoreo estamos, y el contador de secuencia de lotes. Vive en
 * DataStore para sobrevivir a que el sistema mate el proceso (ver E5 del documento atómico).
 */
@Singleton
class MonitoringSessionContext @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val deviceIdKey = stringPreferencesKey("paired_device_id")
    private val sessionIdKey = stringPreferencesKey("monitoring_session_id")
    private val sessionStartedAtKey = longPreferencesKey("monitoring_session_started_at")
    private val sequenceKey = longPreferencesKey("telemetry_sequence")
    private val latKey = stringPreferencesKey("last_known_lat")
    private val lngKey = stringPreferencesKey("last_known_lng")

    /** Se llama una vez, en el flujo de "Vincular reloj" (E15/vinculación por código). */
    suspend fun setPairedDeviceId(deviceId: String) {
        dataStore.edit { it[deviceIdKey] = deviceId }
    }

    fun pairedDeviceId(): String = runBlocking {
        dataStore.data.first()[deviceIdKey] ?: DEFAULT_DEVICE_ID
    }

    /**
     * Una "sesión de monitoreo" agrupa lotes de telemetría entre aperturas de la app/servicio.
     * Se renueva cada 24h para que no crezca indefinidamente; el detalle exacto de rotación
     * se puede afinar cuando el backend empiece a usarla para consultas.
     */
    fun currentSessionId(): String = runBlocking {
        val prefs = dataStore.data.first()
        val startedAt = prefs[sessionStartedAtKey]
        val existing = prefs[sessionIdKey]
        val expired = startedAt == null || (System.currentTimeMillis() - startedAt) > ONE_DAY_MILLIS

        if (existing != null && !expired) return@runBlocking existing

        val newSessionId = UUID.randomUUID().toString()
        dataStore.edit {
            it[sessionIdKey] = newSessionId
            it[sessionStartedAtKey] = System.currentTimeMillis()
            it[sequenceKey] = 0L
        }
        newSessionId
    }

    fun nextSequence(): Int = runBlocking {
        val next = (dataStore.data.first()[sequenceKey] ?: 0L) + 1
        dataStore.edit { it[sequenceKey] = next }
        next.toInt()
    }

    fun lastKnownLatitude(): Double? = runBlocking {
        dataStore.data.first()[latKey]?.toDoubleOrNull()
    }

    fun lastKnownLongitude(): Double? = runBlocking {
        dataStore.data.first()[lngKey]?.toDoubleOrNull()
    }

    /** Llamar desde FusedLocationProviderClient cuando haya consentimiento y flujo SOS activo. */
    suspend fun updateLastKnownLocation(latitude: Double, longitude: Double) {
        dataStore.edit {
            it[latKey] = latitude.toString()
            it[lngKey] = longitude.toString()
        }
    }

    private companion object {
        const val DEFAULT_DEVICE_ID = "00000000-0000-0000-0000-000000000000"
        const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}

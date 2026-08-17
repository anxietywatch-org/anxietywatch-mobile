package com.anxietywatch.mobile.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El JWT y su expiracion viven en [SecureTokenStore] (cifrado con AES-256, Android
 * Keystore) -- NUNCA en este DataStore normal. Aqui solo va lo que no es sensible por si
 * mismo: el rol (self/family_member/patient) y el deviceId local del telefono.
 *
 * El token dura 30 minutos (confirmado por el equipo de backend) -- por eso [isExpired]
 * existe: cualquier parte de la app puede chequear proactivamente antes de llamar a la
 * API, en vez de esperar a que llegue un 401.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureTokenStore: SecureTokenStore,
) {
    private val roleKey = stringPreferencesKey("user_role")
    private val deviceIdKey = stringPreferencesKey("mobile_device_id")

    val roleFlow: Flow<String?> = dataStore.data.map { it[roleKey] }

    /** Lectura sincrona a proposito: la usa AuthInterceptor en cada request de red, donde
     *  no conviene depender de un Flow suspendible dentro de un interceptor de OkHttp. */
    fun currentToken(): String? = secureTokenStore.getToken()

    suspend fun saveSession(jwt: String, role: String, expiresAt: String) {
        secureTokenStore.saveToken(jwt, expiresAt)
        dataStore.edit { it[roleKey] = role }
    }

    suspend fun clearSession() {
        secureTokenStore.clear()
        dataStore.edit { it.remove(roleKey) }
    }

    /** true si no hay sesión, o si el token de 30 min ya venció. */
    fun isExpired(): Boolean {
        val expiresAtRaw = secureTokenStore.getExpiresAt() ?: return true
        val expiresAt = runCatching { Instant.parse(expiresAtRaw) }.getOrNull() ?: return true
        return Instant.now().isAfter(expiresAt)
    }

    fun hasValidSession(): Boolean {
        val hasToken = secureTokenStore.getToken() != null
        return hasToken && !isExpired()
    }

    suspend fun mobileDeviceId(): String {
        val existing = dataStore.data.first()[deviceIdKey]
        if (existing != null) return existing

        val generated = UUID.randomUUID().toString()
        dataStore.edit { it[deviceIdKey] = generated }
        return generated
    }
}

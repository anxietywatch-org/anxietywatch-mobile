package com.anxietywatch.mobile.data.remote

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SessionProfile(
    val displayName: String? = null,
    val email: String? = null,
    val role: String? = null,
)

interface CaregiverSessionSource {
    val profileFlow: Flow<SessionProfile>
    suspend fun clearSession()
}

/**
 * El JWT y su expiracion viven en [SecureTokenStore] (cifrado con AES-256, Android
 * Keystore) -- NUNCA en este DataStore normal. Aqui solo va lo que no es sensible por si
 * mismo: el rol (self/family_member/patient) y el deviceId local del telefono.
 *
 * El backend actual emite JWT con siete días de vigencia. [isExpired] conserva la
 * comprobación proactiva para evitar usar una sesión vencida y no prolonga el token localmente.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureTokenStore: SecureTokenStore,
) : CaregiverSessionSource {
    private val roleKey = stringPreferencesKey("user_role")
    private val deviceIdKey = stringPreferencesKey("mobile_device_id")
    private val displayNameKey = stringPreferencesKey("session_display_name")
    private val emailKey = stringPreferencesKey("session_email")

    val roleFlow: Flow<String?> = dataStore.data.map { it[roleKey] }

    override val profileFlow: Flow<SessionProfile> = dataStore.data.map {
        SessionProfile(it[displayNameKey], it[emailKey], it[roleKey])
    }

    /** Lectura sincrona a proposito: la usa AuthInterceptor en cada request de red, donde
     *  no conviene depender de un Flow suspendible dentro de un interceptor de OkHttp. */
    fun currentToken(): String? = secureTokenStore.getToken()

    suspend fun saveSession(
        jwt: String,
        role: String,
        expiresAt: String,
        displayName: String? = null,
        email: String? = null,
    ) {
        secureTokenStore.saveToken(jwt, expiresAt)
        dataStore.edit {
            it[roleKey] = role
            if (displayName.isNullOrBlank()) it.remove(displayNameKey) else it[displayNameKey] = displayName
            if (email.isNullOrBlank()) it.remove(emailKey) else it[emailKey] = email
        }
    }

    override suspend fun clearSession() {
        secureTokenStore.clear()
        dataStore.edit {
            it.remove(roleKey)
            it.remove(displayNameKey)
            it.remove(emailKey)
        }
    }

    /** true si no hay sesión, si expiresAt no se puede interpretar o si ya venció. */
    fun isExpired(): Boolean {
        val expiresAtRaw = secureTokenStore.getExpiresAt() ?: return true
        val expiresAt = try {
            Instant.parse(expiresAtRaw)
        } catch (exception: DateTimeParseException) {
            Log.w(TAG, "No se pudo interpretar expiresAt persistido: $expiresAtRaw", exception)
            return true
        }
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

    private companion object {
        const val TAG = "SessionRepository"
    }
}

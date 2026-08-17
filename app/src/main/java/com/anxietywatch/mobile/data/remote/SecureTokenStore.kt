package com.anxietywatch.mobile.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DevSecOps: el JWT NUNCA se guarda en texto plano. EncryptedSharedPreferences cifra
 * tanto las claves como los valores con AES-256, y la llave maestra vive en el Android
 * Keystore -- respaldado por hardware seguro (StrongBox/TEE) en la mayoria de equipos
 * modernos, nunca en un archivo que se pueda leer solo con acceso root.
 *
 * Separado de SessionRepository (que sigue usando DataStore normal) a proposito: solo lo
 * verdaderamente sensible (el JWT) pasa por aqui. El deviceId local o el rol no necesitan
 * este nivel de proteccion y complicarian el codigo sin ganar seguridad real.
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "anxietywatch_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveToken(jwt: String, expiresAt: String) {
        prefs.edit()
            .putString(KEY_JWT, jwt)
            .putString(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_JWT, null)

    fun getExpiresAt(): String? = prefs.getString(KEY_EXPIRES_AT, null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_JWT = "jwt_token"
        const val KEY_EXPIRES_AT = "jwt_expires_at"
    }
}

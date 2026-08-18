package com.anxietywatch.mobile.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "anxietywatch_secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(token: String, expiresAt: String, userRole: String) {
        encryptedPrefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EXPIRES_AT, expiresAt)
            .putString(KEY_USER_ROLE, userRole)
            .apply()
    }

    fun getToken(): String? = encryptedPrefs.getString(KEY_TOKEN, null)

    fun getUserRole(): String? = encryptedPrefs.getString(KEY_USER_ROLE, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() {
        encryptedPrefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ROLE = "user_role"
    }
}
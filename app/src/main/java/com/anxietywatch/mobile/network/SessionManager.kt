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

    fun saveSession(token: String, expiresAt: String, userRole: String, userFullName: String) {
        encryptedPrefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EXPIRES_AT, expiresAt)
            .putString(KEY_USER_ROLE, userRole)
            .putString(KEY_FULL_NAME, userFullName)
            .apply()
    }

    fun getToken(): String? = encryptedPrefs.getString(KEY_TOKEN, null)
    fun getUserRole(): String? = encryptedPrefs.getString(KEY_USER_ROLE, null)
    fun getFullName(): String? = encryptedPrefs.getString(KEY_FULL_NAME, null)
    fun isLoggedIn(): Boolean = getToken() != null

    fun saveAvatarUri(uri: String) {
        encryptedPrefs.edit().putString(KEY_AVATAR_URI, uri).apply()
    }
    fun getAvatarUri(): String? = encryptedPrefs.getString(KEY_AVATAR_URI, null)

    // Campos guardados solo en el dispositivo (el backend aún no los soporta)
    fun saveLocalProfileExtras(age: String, gender: String, heightCm: String, weightKg: String) {
        encryptedPrefs.edit()
            .putString(KEY_AGE, age)
            .putString(KEY_GENDER, gender)
            .putString(KEY_HEIGHT, heightCm)
            .putString(KEY_WEIGHT, weightKg)
            .apply()
    }

    fun saveWellnessExtras(baselineAnxiety: String, triggers: String, relaxationTechnique: String, sleepHours: String) {
        encryptedPrefs.edit()
            .putString(KEY_BASELINE_ANXIETY, baselineAnxiety)
            .putString(KEY_TRIGGERS, triggers)
            .putString(KEY_RELAXATION_TECHNIQUE, relaxationTechnique)
            .putString(KEY_SLEEP_HOURS, sleepHours)
            .apply()
    }
    fun recordBreathingSessionCompleted() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val current = getBreathingSessionDates()
        val updated = (current + today).takeLast(60)
        encryptedPrefs.edit().putString(KEY_BREATHING_SESSIONS, updated.joinToString(",")).apply()
    }

    fun getBreathingSessionDates(): List<String> {
        val raw = encryptedPrefs.getString(KEY_BREATHING_SESSIONS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",")
    }

    fun getBaselineAnxiety(): String? = encryptedPrefs.getString(KEY_BASELINE_ANXIETY, null)
    fun getTriggers(): String? = encryptedPrefs.getString(KEY_TRIGGERS, null)
    fun getRelaxationTechnique(): String? = encryptedPrefs.getString(KEY_RELAXATION_TECHNIQUE, null)
    fun getSleepHours(): String? = encryptedPrefs.getString(KEY_SLEEP_HOURS, null)

    fun getAge(): String? = encryptedPrefs.getString(KEY_AGE, null)
    fun getGender(): String? = encryptedPrefs.getString(KEY_GENDER, null)
    fun getHeightCm(): String? = encryptedPrefs.getString(KEY_HEIGHT, null)
    fun getWeightKg(): String? = encryptedPrefs.getString(KEY_WEIGHT, null)

    // Progreso de onboarding, para retomar donde el usuario se quedó
    fun setWelcomeSeen() = encryptedPrefs.edit().putBoolean(KEY_ONBOARD_WELCOME, true).apply()
    fun hasSeenWelcome(): Boolean = encryptedPrefs.getBoolean(KEY_ONBOARD_WELCOME, false)

    fun setPermissionsGranted() = encryptedPrefs.edit().putBoolean(KEY_ONBOARD_PERMISSIONS, true).apply()
    fun hasGrantedPermissions(): Boolean = encryptedPrefs.getBoolean(KEY_ONBOARD_PERMISSIONS, false)

    fun setMedicalInfoDone() = encryptedPrefs.edit().putBoolean(KEY_ONBOARD_MEDICAL, true).apply()
    fun hasCompletedMedicalInfo(): Boolean = encryptedPrefs.getBoolean(KEY_ONBOARD_MEDICAL, false)

    fun setWatchStepDone() = encryptedPrefs.edit().putBoolean(KEY_ONBOARD_WATCH, true).apply()
    fun hasCompletedWatchStep(): Boolean = encryptedPrefs.getBoolean(KEY_ONBOARD_WATCH, false)

    fun saveLinkedWatchAddress(address: String) = encryptedPrefs.edit().putString(KEY_WATCH_ADDRESS, address).apply()
    fun getLinkedWatchAddress(): String? = encryptedPrefs.getString(KEY_WATCH_ADDRESS, null)

    fun clearSession() {
        encryptedPrefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_AVATAR_URI = "avatar_uri"
        private const val KEY_AGE = "profile_age"
        private const val KEY_GENDER = "profile_gender"
        private const val KEY_HEIGHT = "profile_height"
        private const val KEY_WEIGHT = "profile_weight"
        private const val KEY_ONBOARD_WELCOME = "onboard_welcome"
        private const val KEY_ONBOARD_PERMISSIONS = "onboard_permissions"
        private const val KEY_ONBOARD_MEDICAL = "onboard_medical"
        private const val KEY_ONBOARD_WATCH = "onboard_watch"
        private const val KEY_WATCH_ADDRESS = "watch_address"
        private const val KEY_BASELINE_ANXIETY = "wellness_baseline"
        private const val KEY_TRIGGERS = "wellness_triggers"
        private const val KEY_RELAXATION_TECHNIQUE = "wellness_relaxation"
        private const val KEY_SLEEP_HOURS = "wellness_sleep_hours"
        private const val KEY_BREATHING_SESSIONS = "breathing_sessions"
    }
}
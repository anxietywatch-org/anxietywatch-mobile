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

    fun recordGroundingSessionCompleted() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val current = getGroundingSessionDates()
        val updated = (current + today).takeLast(60)
        encryptedPrefs.edit().putString(KEY_GROUNDING_SESSIONS, updated.joinToString(",")).apply()
    }

    fun getGroundingSessionDates(): List<String> {
        val raw = encryptedPrefs.getString(KEY_GROUNDING_SESSIONS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",")
    }

    fun saveLatestHeartRate(bpm: Int) {
        encryptedPrefs.edit()
            .putInt(KEY_LATEST_HR, bpm)
            .putLong(KEY_LATEST_HR_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getLatestHeartRate(): Int? {
        val value = encryptedPrefs.getInt(KEY_LATEST_HR, -1)
        return if (value == -1) null else value
    }

    fun getLatestHeartRateAgeMillis(): Long? {
        val time = encryptedPrefs.getLong(KEY_LATEST_HR_TIME, -1)
        return if (time == -1L) null else System.currentTimeMillis() - time
    }

    fun saveLastSosEvent(eventId: String, wasCancelled: Boolean) {
        encryptedPrefs.edit()
            .putString(KEY_LAST_SOS_ID, eventId)
            .putBoolean(KEY_LAST_SOS_CANCELLED, wasCancelled)
            .apply()
    }

    // isAnomalyPending() la sigue escribiendo PhoneFogListenerService cuando llega
    // BPM real por encima del umbral -- esto NO es simulación, es el estado real
    // de detección basado en datos reales del reloj.
    fun setAnomalyPending(pending: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_ANOMALY_PENDING, pending).apply()
    }
    fun isAnomalyPending(): Boolean = encryptedPrefs.getBoolean(KEY_ANOMALY_PENDING, false)

    fun recordCrisisEvent(durationSeconds: Int) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "$timestamp|$durationSeconds"
        val current = getCrisisEvents()
        val updated = (current + entry).takeLast(50)
        encryptedPrefs.edit().putString(KEY_CRISIS_EVENTS, updated.joinToString(";;")).apply()
    }
    fun getCrisisEvents(): List<String> {
        val raw = encryptedPrefs.getString(KEY_CRISIS_EVENTS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(";;")
    }

    fun recordFalsePositive() {
        val current = encryptedPrefs.getInt(KEY_FALSE_POSITIVE_COUNT, 0)
        encryptedPrefs.edit().putInt(KEY_FALSE_POSITIVE_COUNT, current + 1).apply()
    }
    fun setPendingCriticalAlert(patientName: String) {
        encryptedPrefs.edit()
            .putBoolean(KEY_PENDING_CRITICAL_ALERT, true)
            .putString(KEY_PENDING_CRITICAL_ALERT_PATIENT, patientName)
            .apply()
    }
    fun hasPendingCriticalAlert(): Boolean = encryptedPrefs.getBoolean(KEY_PENDING_CRITICAL_ALERT, false)
    fun getPendingCriticalAlertPatientName(): String? = encryptedPrefs.getString(KEY_PENDING_CRITICAL_ALERT_PATIENT, null)
    fun clearPendingCriticalAlert() {
        encryptedPrefs.edit().putBoolean(KEY_PENDING_CRITICAL_ALERT, false).apply()
    }

    fun recordCriticalAlertHistory(patientName: String) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "$timestamp|$patientName"
        val current = getCriticalAlertHistory()
        val updated = (current + entry).takeLast(50)
        encryptedPrefs.edit().putString(KEY_CRITICAL_ALERT_HISTORY, updated.joinToString(";;")).apply()
    }
    fun getCriticalAlertHistory(): List<String> {
        val raw = encryptedPrefs.getString(KEY_CRITICAL_ALERT_HISTORY, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(";;")
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
    fun saveLinkedCaregiverInfo(name: String) {
        encryptedPrefs.edit().putString(KEY_LINKED_CAREGIVER, name).apply()
    }
    fun getLinkedCaregiverInfo(): String? = encryptedPrefs.getString(KEY_LINKED_CAREGIVER, null)
    // Preferencias reales (E13/E04)
    fun setNotificationsEnabled(enabled: Boolean) = encryptedPrefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    fun areNotificationsEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    fun setSoundEnabled(enabled: Boolean) = encryptedPrefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    fun isSoundEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_SOUND_ENABLED, false)

    // Sensibilidad de detección configurable (H085/H086)
    fun setAnomalyThresholdBpm(bpm: Int) = encryptedPrefs.edit().putInt(KEY_ANOMALY_THRESHOLD, bpm).apply()
    fun getAnomalyThresholdBpm(): Int = encryptedPrefs.getInt(KEY_ANOMALY_THRESHOLD, 120)

    fun setDetectionPaused(paused: Boolean, untilMillis: Long) {
        encryptedPrefs.edit()
            .putBoolean(KEY_DETECTION_PAUSED, paused)
            .putLong(KEY_DETECTION_PAUSED_UNTIL, untilMillis)
            .apply()
    }
    fun isDetectionCurrentlyPaused(): Boolean {
        val paused = encryptedPrefs.getBoolean(KEY_DETECTION_PAUSED, false)
        if (!paused) return false
        val until = encryptedPrefs.getLong(KEY_DETECTION_PAUSED_UNTIL, 0)
        return System.currentTimeMillis() < until
    }

    // Expiración de sesión por inactividad (H196/H198)
    fun updateLastActivityTimestamp() {
        encryptedPrefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
    }
    fun hasSessionExpiredByInactivity(): Boolean {
        if (!isLoggedIn()) return false
        val last = encryptedPrefs.getLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
        val elapsed = System.currentTimeMillis() - last
        return elapsed > SESSION_INACTIVITY_LIMIT_MILLIS
    }
    fun setSessionExpiredFlag(expired: Boolean) = encryptedPrefs.edit().putBoolean(KEY_SESSION_EXPIRED_FLAG, expired).apply()
    fun consumeSessionExpiredFlag(): Boolean {
        val value = encryptedPrefs.getBoolean(KEY_SESSION_EXPIRED_FLAG, false)
        if (value) encryptedPrefs.edit().putBoolean(KEY_SESSION_EXPIRED_FLAG, false).apply()
        return value
    }
    fun clearLinkedCaregiverInfo() {
        encryptedPrefs.edit().remove(KEY_LINKED_CAREGIVER).apply()
    }
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
        private const val KEY_LATEST_HR = "latest_heart_rate"
        private const val KEY_LATEST_HR_TIME = "latest_heart_rate_time"
        private const val KEY_LAST_SOS_ID = "last_sos_id"
        private const val KEY_LAST_SOS_CANCELLED = "last_sos_cancelled"
        private const val KEY_ANOMALY_PENDING = "anomaly_pending"
        private const val KEY_CRISIS_EVENTS = "crisis_events"
        private const val KEY_FALSE_POSITIVE_COUNT = "false_positive_count"
        private const val KEY_GROUNDING_SESSIONS = "grounding_sessions"
        private const val KEY_CRITICAL_ALERT_HISTORY = "critical_alert_history"
        private const val KEY_PENDING_CRITICAL_ALERT = "pending_critical_alert"
        private const val KEY_PENDING_CRITICAL_ALERT_PATIENT = "pending_critical_alert_patient"
        private const val KEY_LINKED_CAREGIVER = "linked_caregiver_name"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_ANOMALY_THRESHOLD = "anomaly_threshold_bpm"
        private const val KEY_DETECTION_PAUSED = "detection_paused"
        private const val KEY_DETECTION_PAUSED_UNTIL = "detection_paused_until"
        private const val KEY_LAST_ACTIVITY = "last_activity_timestamp"
        private const val KEY_SESSION_EXPIRED_FLAG = "session_expired_flag"
        val SESSION_INACTIVITY_LIMIT_MILLIS = 30 * 60 * 1000L
    }
}
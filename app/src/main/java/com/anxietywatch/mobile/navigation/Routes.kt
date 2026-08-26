package com.anxietywatch.mobile.navigation

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object TokenEntry : Routes("token_entry") // E02: ingreso por token, sin login tradicional
    data object PermissionsPatient : Routes("permissions_patient") // E03
    data object PermissionsCaregiver : Routes("permissions_caregiver") // E04
    data object PatientProfile : Routes("patient_profile") // E05
    data object HomePatient : Routes("home_patient") // E06
    data object Interruption : Routes("interruption") // E07 — pantalla de validación 15s
    data object CrisisActive : Routes("crisis_active") // E08 — respiración en caja
    data object History : Routes("history") // E12
    data object SettingsPatient : Routes("settings_patient") // E13
    data object WatchPairing : Routes("watch_pairing")
    data object ManageWatch : Routes("manage_watch")
    data object DashboardCaregiver : Routes("dashboard_caregiver") // E14
    data object CaregiverPatients : Routes("caregiver_patients")
    data object CaregiverAlerts : Routes("caregiver_alerts")
    data object CaregiverProfile : Routes("caregiver_profile")
    data object CaregiverAlertDetail : Routes("caregiver_alert_detail/{alertId}") {
        fun build(alertId: String) = "caregiver_alert_detail/$alertId"
    }
    data object PatientDetail : Routes("patient_detail/{patientId}") { // E16
        fun build(patientId: String) = "patient_detail/$patientId"
    }
    data object EventDetail : Routes("event_detail/{eventId}") {
        fun build(eventId: String) = "event_detail/$eventId"
    }
    data object SupportGuide : Routes("support_guide/{eventId}") { // E18
        fun build(eventId: String) = "support_guide/$eventId"
    }
}

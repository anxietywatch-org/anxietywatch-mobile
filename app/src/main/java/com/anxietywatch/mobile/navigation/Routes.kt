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
    data object DashboardCaregiver : Routes("dashboard_caregiver") // E14
    data object PatientDetail : Routes("patient_detail/{patientId}") { // E16
        fun build(patientId: String) = "patient_detail/$patientId"
    }
    data object CriticalAlert : Routes("critical_alert/{eventId}") // E17
    data object SupportGuide : Routes("support_guide/{eventId}") // E18
}

// TODO: NavHost real con composable() por ruta, a construir a medida que se agreguen las
// pantallas — dejo las rutas ya nombradas para que coincidan 1:1 con las épicas del backlog
// (PRODUCT_BACKLOG.xlsx, hoja "Movil") y no haya que rediseñar la navegación después.

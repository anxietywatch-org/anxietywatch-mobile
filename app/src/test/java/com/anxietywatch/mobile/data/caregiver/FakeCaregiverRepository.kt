package com.anxietywatch.mobile.data.caregiver

class FakeCaregiverRepository : CaregiverRepository {
    override suspend fun loadDashboard(): CaregiverDashboardSource = CaregiverDashboardSource(
        patients = listOf(
            CaregiverPatientSource(
                id = "patient-alex",
                displayName = "Alex",
                bpm = 72,
                lastUpdated = "Hace 2 min",
                connectivity = "Conectado",
                freshness = "Reciente",
            ),
            CaregiverPatientSource(
                id = "patient-sofia",
                displayName = "Sofía",
                bpm = 96,
                lastUpdated = "Hace 5 min",
                connectivity = "Conectado",
                freshness = "Reciente",
            ),
        ),
    )

    override suspend fun getPatientDetail(patientId: String): CaregiverPatientDetailSource? = when (patientId) {
        "patient-alex" -> CaregiverPatientDetailSource(
            id = "patient-alex",
            displayName = "Alex",
            bpm = 72,
            anxiety = 68,
            lastUpdated = "Hace 2 min",
            connectivity = "Conectado",
            freshness = "Reciente",
            alertState = "Revisión disponible",
            recentEvents = listOf(
                CaregiverRecentEventSource(
                    id = "event-alex-1",
                    title = "Actividad reciente",
                    description = "Registro disponible para revisión.",
                    occurredAt = "Hoy",
                ),
            ),
            recentAlerts = listOf(
                CaregiverRecentAlertSource(
                    id = "alert-alex-1",
                    title = "Revisión pendiente",
                    description = "Hay información reciente para revisar.",
                    occurredAt = "Hoy",
                    status = "Pendiente",
                ),
            ),
        )
        "patient-sofia" -> CaregiverPatientDetailSource(
            id = "patient-sofia",
            displayName = "Sofía",
            bpm = 96,
            lastUpdated = "Hace 5 min",
        )
        else -> null
    }

    override suspend fun getAlerts(): List<CaregiverAlertSource> = listOf(
        CaregiverAlertSource(
            id = "alert-alex-1",
            patientId = "patient-alex",
            patientDisplayName = "Alex",
            timestamp = "Hoy",
            type = "Revisión",
            status = "Pendiente",
            title = "Revisión pendiente",
            summary = "Hay información reciente para revisar.",
            bpm = 72,
            anxiety = 68,
        ),
    )

    override suspend fun getAlertDetail(alertId: String): CaregiverAlertSource? =
        getAlerts().firstOrNull { it.id == alertId }
}

package com.anxietywatch.mobile.data.caregiver

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

data class CaregiverPatientSource(
    val id: String,
    val displayName: String,
    val bpm: Int? = null,
    val anxiety: Int? = null,
    val lastUpdated: String? = null,
    val connectivity: String? = null,
    val freshness: String? = null,
    val alertState: String? = null,
)

data class CaregiverDashboardSource(
    val patients: List<CaregiverPatientSource>,
)

data class CaregiverRecentEventSource(
    val id: String,
    val title: String,
    val description: String? = null,
    val occurredAt: String? = null,
)

data class CaregiverRecentAlertSource(
    val id: String,
    val title: String,
    val description: String? = null,
    val occurredAt: String? = null,
    val status: String? = null,
)

data class CaregiverPatientDetailSource(
    val id: String,
    val displayName: String,
    val bpm: Int? = null,
    val anxiety: Int? = null,
    val lastUpdated: String? = null,
    val connectivity: String? = null,
    val freshness: String? = null,
    val alertState: String? = null,
    val recentEvents: List<CaregiverRecentEventSource> = emptyList(),
    val recentAlerts: List<CaregiverRecentAlertSource> = emptyList(),
)

interface CaregiverRepository {
    suspend fun loadDashboard(): CaregiverDashboardSource
    suspend fun getPatientDetail(patientId: String): CaregiverPatientDetailSource?
}

/** Temporary frontend source until a confirmed caregiver API contract exists. */
class FakeCaregiverRepository @Inject constructor() : CaregiverRepository {
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
            anxiety = null,
            lastUpdated = "Hace 5 min",
            connectivity = "Conectado",
            freshness = "Reciente",
            recentEvents = emptyList(),
            recentAlerts = emptyList(),
        )
        else -> null
    }
}

@Module
@InstallIn(SingletonComponent::class)
object CaregiverRepositoryModule {
    @Provides
    @Singleton
    fun provideCaregiverRepository(): CaregiverRepository = FakeCaregiverRepository()
}

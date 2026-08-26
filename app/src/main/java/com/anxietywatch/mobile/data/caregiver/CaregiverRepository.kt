package com.anxietywatch.mobile.data.caregiver

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.CaregiverEventResponseDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

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

data class CaregiverAlertSource(
    val id: String,
    val patientId: String,
    val patientDisplayName: String,
    val timestamp: String? = null,
    val type: String? = null,
    val status: String? = null,
    val title: String,
    val summary: String? = null,
    val acknowledged: Boolean? = null,
    val resolved: Boolean? = null,
    val bpm: Int? = null,
    val anxiety: Int? = null,
)

interface CaregiverRepository {
    suspend fun loadDashboard(): CaregiverDashboardSource
    suspend fun getPatientDetail(patientId: String): CaregiverPatientDetailSource?
    suspend fun getAlerts(): List<CaregiverAlertSource> = emptyList()
    suspend fun getAlertDetail(alertId: String): CaregiverAlertSource? = getAlerts().firstOrNull { it.id == alertId }
}

class RealCaregiverRepository @Inject constructor(
    private val api: AnxietyWatchApi,
) : CaregiverRepository {
    override suspend fun loadDashboard(): CaregiverDashboardSource =
        CaregiverDashboardSource(
            patients = api.getCaregiverPatients().map { patient ->
                CaregiverPatientSource(
                    id = patient.patientId,
                    displayName = patient.fullName,
                )
            },
        )

    override suspend fun getPatientDetail(patientId: String): CaregiverPatientDetailSource {
        val patient = api.getCaregiverPatientDetail(patientId)
        val events = api.getCaregiverPatientEvents(patientId)
        val heartRate = api.getCaregiverLatestHeartRate(patientId)

        return CaregiverPatientDetailSource(
            id = patient.patientId,
            displayName = patient.fullName,
            bpm = heartRate?.heartRateBpm?.roundToInt(),
            lastUpdated = heartRate?.measuredAt,
            recentEvents = events.map { it.toSource() },
        )
    }

    // Develop exposes no caregiver alert collection or alert-detail endpoint.
    override suspend fun getAlerts(): List<CaregiverAlertSource> = emptyList()

    override suspend fun getAlertDetail(alertId: String): CaregiverAlertSource? = null
}

private fun CaregiverEventResponseDto.toSource() = CaregiverRecentEventSource(
    id = eventId,
    title = type,
    description = status,
    occurredAt = occurredAt,
)

@Module
@InstallIn(SingletonComponent::class)
object CaregiverRepositoryModule {
    @Provides
    @Singleton
    fun provideCaregiverRepository(api: AnxietyWatchApi): CaregiverRepository = RealCaregiverRepository(api)
}

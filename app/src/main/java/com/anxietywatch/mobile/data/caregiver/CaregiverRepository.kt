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

interface CaregiverRepository {
    suspend fun loadDashboard(): CaregiverDashboardSource
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
}

@Module
@InstallIn(SingletonComponent::class)
object CaregiverRepositoryModule {
    @Provides
    @Singleton
    fun provideCaregiverRepository(): CaregiverRepository = FakeCaregiverRepository()
}

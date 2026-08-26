package com.anxietywatch.mobile.ui.dashboard

import com.anxietywatch.mobile.data.caregiver.CaregiverDashboardSource
import com.anxietywatch.mobile.data.caregiver.CaregiverPatientSource
import com.anxietywatch.mobile.data.caregiver.CaregiverRepository
import com.anxietywatch.mobile.data.caregiver.FakeCaregiverRepository
import com.anxietywatch.mobile.ui.common.ConnectivityStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Test

class CaregiverDashboardUiModelsTest {
    @Test
    fun loadingStateIsExplicit() {
        assertSame(DashboardCaregiverUiState.Loading, DashboardCaregiverUiState.Loading)
    }

    @Test
    fun contentStateCarriesPatients() {
        val patient = patient(bpm = 72)
        val state = DashboardCaregiverUiState.Content(CaregiverDashboardUiModel(listOf(patient)))

        assertEquals(listOf(patient), state.data.patients)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun emptyStateIsExplicit() {
        assertEquals(DashboardCaregiverUiState.Empty(), DashboardCaregiverUiState.Empty())
    }

    @Test
    fun errorStateCarriesMessage() {
        assertEquals("offline", DashboardCaregiverUiState.Error("offline").message)
    }

    @Test
    fun sourceMapsStableIdAndNullableMetrics() {
        val model = CaregiverDashboardSource(
            listOf(
                CaregiverPatientSource(
                    id = "patient-1",
                    displayName = "Paciente",
                    bpm = null,
                    anxiety = null,
                    connectivity = "desconectado",
                ),
            ),
        ).toUiModel().patients.single()

        assertEquals("patient-1", model.id)
        assertNull(model.bpm)
        assertNull(model.anxiety)
        assertEquals(ConnectivityStatus.Disconnected, model.connectivity)
    }

    @Test
    fun sourceMapsPresentBpm() {
        assertEquals(96, patient(bpm = 96).bpm)
    }

    @Test
    fun fakeRepositoryProvidesExplicitFrontendSource() = runBlocking {
        val result = FakeCaregiverRepository().loadDashboard()

        assertEquals(2, result.patients.size)
        assertEquals("patient-alex", result.patients.first().id)
    }

    @Test
    fun repositoryErrorsRemainObservableToCaller() = runBlocking {
        val repository = object : CaregiverRepository {
            override suspend fun loadDashboard(): CaregiverDashboardSource {
                error("source unavailable")
            }
        }

        val error = runCatching { repository.loadDashboard() }.exceptionOrNull()
        assertEquals("source unavailable", error?.message)
    }

    @Test
    fun refreshStatePreservesContent() {
        val data = CaregiverDashboardUiModel(listOf(patient(bpm = null)))
        val refreshing = DashboardCaregiverUiState.Content(data, isRefreshing = true)

        assertEquals(data, refreshing.data)
        assertEquals(true, refreshing.isRefreshing)
        assertNull(refreshing.refreshError)
    }

    private fun patient(bpm: Int?): CaregiverPatientUiModel = CaregiverPatientUiModel(
        id = "patient-test",
        displayName = "Paciente de prueba",
        bpm = bpm,
    )
}

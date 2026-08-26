package com.anxietywatch.mobile.ui.alerts

import com.anxietywatch.mobile.data.caregiver.CaregiverAlertSource
import com.anxietywatch.mobile.data.caregiver.FakeCaregiverRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class CaregiverAlertsTest {
    @Test fun loadingContentEmptyAndErrorStatesAreExplicit() {
        assertSame(CaregiverAlertsUiState.Loading, CaregiverAlertsUiState.Loading)
        assertEquals(1, CaregiverAlertsUiState.Content(listOf(alert().toUiModel())).data.size)
        assertTrue(CaregiverAlertsUiState.Empty().let { it is CaregiverAlertsUiState.Empty })
        assertEquals("offline", CaregiverAlertsUiState.Error("offline").message)
    }

    @Test fun fakeRepositoryReturnsAlertAndEmptyPatientDetailAlerts() = runBlocking {
        val repository = FakeCaregiverRepository()
        assertEquals("alert-alex-1", repository.getAlerts().single().id)
        assertTrue(repository.getAlerts().none { it.patientId == "patient-sofia" })
    }

    @Test fun alertIdsResolveDifferentDetailsWithoutFirstItemFallback() = runBlocking {
        val repository = object : com.anxietywatch.mobile.data.caregiver.CaregiverRepository {
            override suspend fun loadDashboard() = com.anxietywatch.mobile.data.caregiver.CaregiverDashboardSource(emptyList())
            override suspend fun getPatientDetail(patientId: String) = null
            override suspend fun getAlerts() = listOf(alert("a", "Paciente A"), alert("b", "Paciente B"))
        }
        assertEquals("Paciente A", repository.getAlertDetail("a")?.patientDisplayName)
        assertEquals("Paciente B", repository.getAlertDetail("b")?.patientDisplayName)
        assertNull(repository.getAlertDetail("unknown"))
    }

    @Test fun nullableMetricsRemainNull() {
        val model = alert(bpm = null, anxiety = null).toUiModel()
        assertNull(model.bpm)
        assertNull(model.anxiety)
    }

    @Test fun refreshContentRetainsPreviousDataOnRefreshError() {
        val previous = listOf(alert().toUiModel())
        val state = CaregiverAlertsUiState.Content(previous, isRefreshing = true, refreshError = "offline")
        assertEquals("a", state.data.single().id)
        assertEquals("offline", state.refreshError)
    }

    @Test fun validAlertDetailUsesItsOwnId() = runBlocking {
        val source = FakeCaregiverRepository().getAlertDetail("alert-alex-1")!!
        assertEquals("alert-alex-1", source.toUiModel().id)
    }

    @Test fun alertDetailStatesCoverLoadingErrorAndUnknown() {
        assertSame(CaregiverAlertDetailUiState.Loading, CaregiverAlertDetailUiState.Loading)
        assertEquals("a", CaregiverAlertDetailUiState.Content(alert().toUiModel()).data.id)
        assertEquals("Alerta no encontrada", CaregiverAlertDetailUiState.Error("Alerta no encontrada").message)
    }

    private fun alert(id: String = "a", patient: String = "Paciente", bpm: Int? = 72, anxiety: Int? = 55) = CaregiverAlertSource(
        id = id,
        patientId = "patient-$id",
        patientDisplayName = patient,
        title = "Revisión",
        bpm = bpm,
        anxiety = anxiety,
    )
}

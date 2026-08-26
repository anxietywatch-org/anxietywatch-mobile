package com.anxietywatch.mobile.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaregiverPatientsStateTest {
    @Test
    fun contentKeepsMultipleStablePatientIds() {
        val patients = listOf(
            CaregiverPatientUiModel("patient-alex", "Alex"),
            CaregiverPatientUiModel("patient-sofia", "Sofía"),
        )

        val state = DashboardCaregiverUiState.Content(CaregiverDashboardUiModel(patients))

        assertEquals(listOf("patient-alex", "patient-sofia"), state.data.patients.map { it.id })
    }

    @Test
    fun refreshContentPreservesPreviousPatientsOnPartialError() {
        val previous = CaregiverDashboardUiModel(
            listOf(CaregiverPatientUiModel("patient-alex", "Alex", bpm = null, anxiety = null)),
        )
        val state = DashboardCaregiverUiState.Content(
            data = previous,
            isRefreshing = false,
        ).copy(
            isRefreshing = false,
            refreshError = "source unavailable",
        )

        assertEquals(previous, state.data)
        assertEquals("source unavailable", state.refreshError)
        assertNull(state.data.patients.single().bpm)
        assertNull(state.data.patients.single().anxiety)
    }

    @Test
    fun refreshStateIsIndependentFromContentState() {
        val state = DashboardCaregiverUiState.Content(
            CaregiverDashboardUiModel(listOf(CaregiverPatientUiModel("patient-alex", "Alex"))),
            isRefreshing = true,
        )

        assertTrue(state.isRefreshing)
        assertEquals("patient-alex", state.data.patients.single().id)
    }
}

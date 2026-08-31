package com.anxietywatch.mobile.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardCaregiverScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateIsDisplayed() {
        setState(DashboardCaregiverUiState.Loading)

        composeRule.onNodeWithText("Cargando pacientes...").assertIsDisplayed()
    }

    @Test
    fun emptyStateIsDisplayed() {
        setState(DashboardCaregiverUiState.Empty())

        composeRule.onNodeWithText("No hay pacientes vinculados.").assertIsDisplayed()
    }

    @Test
    fun contentStateShowsPatientAndNullableBpm() {
        setState(
            DashboardCaregiverUiState.Content(
                CaregiverDashboardUiModel(
                    listOf(
                        CaregiverPatientUiModel(
                            id = "patient-null-bpm",
                            displayName = "Paciente sin lectura",
                        ),
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("Paciente sin lectura").assertIsDisplayed()
        composeRule.onNodeWithText("Sin lectura de BPM").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsRetry() {
        setState(DashboardCaregiverUiState.Error("No hay conexión"))

        composeRule.onNodeWithText("No hay conexión").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    @Test
    fun patientClickSendsStableId() {
        var clickedId = ""
        composeRule.setContent {
            AnxietyWatchTheme {
                DashboardCaregiverScreen(
                    state = DashboardCaregiverUiState.Content(
                        CaregiverDashboardUiModel(
                            listOf(CaregiverPatientUiModel("patient-stable", "Paciente estable", bpm = 72)),
                        ),
                    ),
                    onPatientClick = { clickedId = it },
                )
            }
        }

        composeRule.onNodeWithText("Paciente estable").performClick()
        assertEquals("patient-stable", clickedId)
    }

    @Test
    fun presentBpmIsDisplayed() {
        setState(
            DashboardCaregiverUiState.Content(
                CaregiverDashboardUiModel(
                    listOf(CaregiverPatientUiModel("patient-bpm", "Paciente BPM", bpm = 96)),
                ),
            ),
        )

        composeRule.onNodeWithText("96 BPM").assertIsDisplayed()
    }

    private fun setState(state: DashboardCaregiverUiState) {
        composeRule.setContent {
            AnxietyWatchTheme { DashboardCaregiverScreen(state = state) }
        }
    }
}

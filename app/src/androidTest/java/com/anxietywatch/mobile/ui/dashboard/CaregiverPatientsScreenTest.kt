package com.anxietywatch.mobile.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CaregiverPatientsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingIsDisplayed() {
        setState(DashboardCaregiverUiState.Loading)

        composeRule.onNodeWithText("Cargando pacientes...").assertIsDisplayed()
    }

    @Test
    fun emptyIsDisplayed() {
        setState(DashboardCaregiverUiState.Empty())

        composeRule.onNodeWithText("No hay pacientes vinculados.").assertIsDisplayed()
    }

    @Test
    fun errorRetryCallsProvidedCallback() {
        var retryCount = 0
        composeRule.setContent {
            AnxietyWatchTheme {
                CaregiverPatientsScreen(
                    state = DashboardCaregiverUiState.Error("No hay conexión"),
                    onRetry = { retryCount++ },
                )
            }
        }

        composeRule.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun twoPatientsAreDisplayed() {
        setState(content(alex(), sofia()))

        composeRule.onNodeWithText("Alex").assertIsDisplayed()
        composeRule.onNodeWithText("Sofía").assertIsDisplayed()
    }

    @Test
    fun nullBpmShowsPlaceholderAndNullAnxietyShowsUnavailable() {
        setState(content(CaregiverPatientUiModel("patient-null", "Sin datos")))

        composeRule.onNodeWithText("--").assertIsDisplayed()
        composeRule.onNodeWithText("Sin lectura").assertIsDisplayed()
        composeRule.onNodeWithText("Sin estado disponible").assertIsDisplayed()
    }

    @Test
    fun alexClickDeliversStableId() {
        var clickedId = ""
        composeRule.setContent {
            AnxietyWatchTheme {
                CaregiverPatientsScreen(
                    state = content(alex(), sofia()),
                    onPatientClick = { clickedId = it },
                )
            }
        }

        composeRule.onNodeWithText("Alex").performClick()
        assertEquals("patient-alex", clickedId)
    }

    @Test
    fun sofiaClickDeliversStableId() {
        var clickedId = ""
        composeRule.setContent {
            AnxietyWatchTheme {
                CaregiverPatientsScreen(
                    state = content(alex(), sofia()),
                    onPatientClick = { clickedId = it },
                )
            }
        }

        composeRule.onNodeWithText("Sofía").performClick()
        assertEquals("patient-sofia", clickedId)
    }

    private fun setState(state: DashboardCaregiverUiState) {
        composeRule.setContent { AnxietyWatchTheme { CaregiverPatientsScreen(state = state) } }
    }

    private fun content(vararg patients: CaregiverPatientUiModel) =
        DashboardCaregiverUiState.Content(CaregiverDashboardUiModel(patients.toList()))

    private fun alex() = CaregiverPatientUiModel("patient-alex", "Alex", bpm = 72)

    private fun sofia() = CaregiverPatientUiModel("patient-sofia", "Sofía", bpm = 96)
}

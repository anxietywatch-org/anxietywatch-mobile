package com.anxietywatch.mobile.ui.wellness

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PatientDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingIsDisplayed() {
        setState(CaregiverPatientDetailUiState.Loading)

        composeRule.onNodeWithText("Cargando paciente...").assertIsDisplayed()
    }

    @Test
    fun errorRetryIsDisplayedAndInvokesCallback() {
        var retryCount = 0
        composeRule.setContent {
            AnxietyWatchTheme {
                PatientDetailScreen(
                    patientId = "unknown-id",
                    state = CaregiverPatientDetailUiState.Error("Paciente no encontrado."),
                    onRetry = { retryCount++ },
                )
            }
        }

        composeRule.onNodeWithText("Paciente no encontrado.").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun alexShowsOwnDataAndNoSyntheticBars() {
        setState(CaregiverPatientDetailUiState.Content(alexDetail()))

        composeRule.onNodeWithText("Alex").assertIsDisplayed()
        composeRule.onNodeWithText("72").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("68").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Actividad reciente").assertCountEquals(2)
        composeRule.onNodeWithText("Revisión pendiente").assertIsDisplayed()
        listOf("78", "84", "91", "102", "128", "Parque Central", "IA Confirmado").forEach {
            composeRule.onAllNodesWithText(it).assertCountEquals(0)
        }
    }

    @Test
    fun sofiaLoadsDistinctDataAndEmptySections() {
        setState(CaregiverPatientDetailUiState.Content(sofiaDetail()))

        composeRule.onNodeWithText("Sofía").assertIsDisplayed()
        composeRule.onNodeWithText("96").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sin estado disponible").assertIsDisplayed()
        composeRule.onNodeWithText("Historial no disponible").assertIsDisplayed()
        composeRule.onNodeWithText("Alertas no disponibles").assertIsDisplayed()
    }

    @Test
    fun nullBpmShowsPlaceholderAndKnownAnxiety() {
        setState(CaregiverPatientDetailUiState.Content(alexDetail(bpm = null, anxiety = 40)))

        composeRule.onNodeWithText("--").assertIsDisplayed()
        composeRule.onNodeWithText("Sin lectura").assertIsDisplayed()
        composeRule.onNodeWithText("40").assertIsDisplayed()
    }

    @Test
    fun bothMetricsNullShowHonestStates() {
        setState(CaregiverPatientDetailUiState.Content(alexDetail(bpm = null, anxiety = null)))

        composeRule.onNodeWithText("--").assertIsDisplayed()
        composeRule.onNodeWithText("Sin lectura").assertIsDisplayed()
        composeRule.onNodeWithText("Sin estado disponible").assertIsDisplayed()
    }

    @Test
    fun backReturnsThroughCallback() {
        var wentBack = false
        composeRule.setContent {
            AnxietyWatchTheme {
                PatientDetailScreen(
                    patientId = "patient-alex",
                    state = CaregiverPatientDetailUiState.Content(alexDetail()),
                    onBack = { wentBack = true },
                )
            }
        }

        composeRule.onNodeWithText("Volver a pacientes").performClick()
        assertEquals(true, wentBack)
    }

    @Test
    fun eventAndAlertClicksUseSourceIds() {
        var eventId = ""
        var alertId = ""
        composeRule.setContent {
            AnxietyWatchTheme {
                PatientDetailScreen(
                    patientId = "patient-alex",
                    state = CaregiverPatientDetailUiState.Content(alexDetail()),
                    onEventClick = { eventId = it },
                    onAlertClick = { alertId = it },
                )
            }
        }

        composeRule.onAllNodesWithText("Actividad reciente").get(1).performClick()
        composeRule.onNodeWithText("Revisión pendiente").performClick()
        assertEquals("event-alex-1", eventId)
        assertEquals("alert-alex-1", alertId)
    }

    private fun setState(state: CaregiverPatientDetailUiState) {
        composeRule.setContent { AnxietyWatchTheme { PatientDetailScreen("patient-test", state = state) } }
    }

    private fun alexDetail(bpm: Int? = 72, anxiety: Int? = 68) = CaregiverPatientDetailUiModel(
        id = "patient-alex",
        displayName = "Alex",
        bpm = bpm,
        anxiety = anxiety,
        lastUpdated = "Hace 2 min",
        recentEvents = listOf(
            CaregiverRecentEventUiModel("event-alex-1", "Actividad reciente", "Registro", "Hoy"),
        ),
        recentAlerts = listOf(
            CaregiverRecentAlertUiModel("alert-alex-1", "Revisión pendiente", "Revisar", "Hoy", "Pendiente"),
        ),
    )

    private fun sofiaDetail() = CaregiverPatientDetailUiModel(
        id = "patient-sofia",
        displayName = "Sofía",
        bpm = 96,
        anxiety = null,
    )
}

package com.anxietywatch.mobile.ui.alerts

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

class CaregiverAlertsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun loadingIsDisplayed() {
        setAlerts(CaregiverAlertsUiState.Loading)
        composeRule.onNodeWithText("Cargando alertas...").assertIsDisplayed()
    }

    @Test fun emptyIsDisplayedWithoutSyntheticAlert() {
        setAlerts(CaregiverAlertsUiState.Empty())
        composeRule.onNodeWithText("No hay alertas").assertIsDisplayed()
        composeRule.onAllNodesWithText("Revisión pendiente").assertCountEquals(0)
    }

    @Test fun errorRetryInvokesCallback() {
        var retries = 0
        composeRule.setContent { AnxietyWatchTheme { CaregiverAlertsScreen(state = CaregiverAlertsUiState.Error("offline"), onRetry = { retries++ }) } }
        composeRule.onNodeWithText("offline").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)
    }

    @Test fun contentClickUsesAlertIdAndDoesNotRenderRemovedHardcodes() {
        var selected = ""
        composeRule.setContent {
            AnxietyWatchTheme {
                CaregiverAlertsScreen(state = CaregiverAlertsUiState.Content(listOf(alert())), onAlertClick = { selected = it })
            }
        }
        composeRule.onNodeWithText("Revisión pendiente").performClick()
        assertEquals("alert-1", selected)
        listOf("Parque Central", "IA Confirmado", "Alex está en crisis", "112").forEach {
            composeRule.onAllNodesWithText(it).assertCountEquals(0)
        }
    }

    @Test fun detailRendersNullableMetricsHonestlyAndBack() {
        var back = false
        composeRule.setContent {
            AnxietyWatchTheme {
                CaregiverAlertDetailScreen(
                    alertId = "alert-1",
                    state = CaregiverAlertDetailUiState.Content(alert(bpm = null, anxiety = null)),
                    onBack = { back = true },
                )
            }
        }
        composeRule.onNodeWithText("Paciente de prueba").assertIsDisplayed()
        composeRule.onNodeWithText("--").assertIsDisplayed()
        composeRule.onNodeWithText("Sin lectura").assertIsDisplayed()
        composeRule.onNodeWithText("Sin estado disponible").assertIsDisplayed()
        composeRule.onNodeWithText("Volver a alertas").performClick()
        assertEquals(true, back)
    }

    @Test fun detailUnknownAlertShowsError() {
        composeRule.setContent { AnxietyWatchTheme { CaregiverAlertDetailScreen("missing", state = CaregiverAlertDetailUiState.Error("Alerta no encontrada")) } }
        composeRule.onNodeWithText("Alerta no encontrada").assertIsDisplayed()
    }

    private fun setAlerts(state: CaregiverAlertsUiState) = composeRule.setContent { AnxietyWatchTheme { CaregiverAlertsScreen(state = state) } }

    private fun alert(bpm: Int? = 72, anxiety: Int? = 55) = CaregiverAlertUiModel(
        id = "alert-1", patientId = "patient-1", patientDisplayName = "Paciente de prueba",
        title = "Revisión pendiente", summary = "Resumen disponible", status = "Pendiente", bpm = bpm, anxiety = anxiety,
    )
}

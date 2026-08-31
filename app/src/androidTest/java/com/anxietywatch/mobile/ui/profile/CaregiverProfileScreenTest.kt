package com.anxietywatch.mobile.ui.profile

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CaregiverProfileScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun profileContentShowsSessionFieldsAndRole() {
        setState(CaregiverProfileUiState.Content(profile("Cuidador de prueba", "mail@example.test")))
        composeRule.onNodeWithText("Cuidador de prueba").assertIsDisplayed()
        composeRule.onNodeWithText("mail@example.test").assertIsDisplayed()
        composeRule.onNodeWithText("Cuidador").assertIsDisplayed()
    }

    @Test fun absentFieldsUseHonestPlaceholderAndNoMaria() {
        setState(CaregiverProfileUiState.Content(profile(null, null)))
        composeRule.onAllNodesWithText("Información no disponible").assertCountEquals(2)
        composeRule.onAllNodesWithText("María").assertCountEquals(0)
    }

    @Test fun logoutDialogCancelDoesNotInvokeCallback() {
        var logoutCount = 0
        setScreen(state = CaregiverProfileUiState.Content(profile("Nombre", "mail@example.test")), onLogout = { logoutCount++ })
        composeRule.onAllNodesWithText("Desvincular").get(0).performClick()
        composeRule.onNodeWithText("¿Quieres desvincular este dispositivo?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        assertEquals(0, logoutCount)
    }

    @Test fun logoutDialogConfirmInvokesCallback() {
        var logoutCount = 0
        setScreen(state = CaregiverProfileUiState.Content(profile("Nombre", "mail@example.test")), onLogout = { logoutCount++ })
        composeRule.onAllNodesWithText("Desvincular").get(0).performClick()
        composeRule.onAllNodesWithText("Desvincular").get(1).performClick()
        assertEquals(1, logoutCount)
    }

    @Test fun loadingIsDisplayed() {
        setState(CaregiverProfileUiState.Loading)
        composeRule.onNodeWithText("Cargando perfil...").assertIsDisplayed()
    }

    @Test fun errorRetryIsDisplayed() {
        var retryCount = 0
        setScreen(CaregiverProfileUiState.Error("offline"), onRetry = { retryCount++ })
        composeRule.onNodeWithText("offline").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retryCount)
    }

    private fun setState(state: CaregiverProfileUiState) = setScreen(state)

    private fun setScreen(
        state: CaregiverProfileUiState,
        onLogout: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        composeRule.setContent {
            AnxietyWatchTheme {
                CaregiverProfileScreen(state = state, onLogout = onLogout, onRetry = onRetry)
            }
        }
    }

    private fun profile(name: String?, email: String?) = CaregiverProfileUiModel(name, email, "family_member")
}

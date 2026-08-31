package com.anxietywatch.mobile.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UiStatesTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateShowsMessage() {
        composeRule.setContent {
            AnxietyWatchTheme { LoadingState("Cargando pacientes") }
        }

        composeRule.onNodeWithText("Cargando pacientes").assertIsDisplayed()
    }

    @Test
    fun errorStateInvokesRetry() {
        var retried = false
        composeRule.setContent {
            AnxietyWatchTheme {
                ErrorState("No hay conexión", onRetry = { retried = true })
            }
        }

        composeRule.onNodeWithText("Reintentar").performClick()
        assertTrue(retried)
    }

    @Test
    fun emptyStateShowsDescription() {
        composeRule.setContent {
            AnxietyWatchTheme {
                EmptyState("Sin pacientes", "Aún no hay vinculaciones")
            }
        }

        composeRule.onNodeWithText("Sin pacientes").assertIsDisplayed()
        composeRule.onNodeWithText("Aún no hay vinculaciones").assertIsDisplayed()
    }
}

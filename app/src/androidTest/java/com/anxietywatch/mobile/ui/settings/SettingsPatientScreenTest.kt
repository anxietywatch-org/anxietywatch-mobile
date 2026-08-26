package com.anxietywatch.mobile.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsPatientScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun logoutRequiresConfirmationAndCancelDoesNotLogout() {
        var logoutCount = 0
        composeRule.setContent {
            AnxietyWatchTheme {
                SettingsPatientScreen(
                    onPersonalProfile = {},
                    onManageWatch = {},
                    onLogout = { logoutCount++ },
                )
            }
        }

        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        assertEquals(0, logoutCount)

        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed()
        composeRule.onAllNodesWithText("Cerrar sesión")[1].performClick()
        assertEquals(1, logoutCount)
    }

    @Test
    fun darkModeSwitchHasAnExplicitAccessibleLabel() {
        composeRule.setContent {
            AnxietyWatchTheme {
                SettingsPatientScreen(
                    onPersonalProfile = {},
                    onManageWatch = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Modo oscuro").assertIsDisplayed()
    }
}

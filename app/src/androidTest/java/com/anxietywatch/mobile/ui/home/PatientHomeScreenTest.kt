package com.anxietywatch.mobile.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import org.junit.Rule
import org.junit.Test

class PatientHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nullHeartRateShowsUnavailableValue() {
        composeRule.setContent {
            AnxietyWatchTheme {
                PatientHeartRateCard(HomePatientUiState(bpm = null))
            }
        }

        composeRule.onNodeWithText("--").assertIsDisplayed()
        composeRule.onNodeWithText("Sin datos del reloj aún").assertIsDisplayed()
    }
}

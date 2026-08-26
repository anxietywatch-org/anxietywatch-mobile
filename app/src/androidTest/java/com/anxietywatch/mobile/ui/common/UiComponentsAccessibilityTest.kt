package com.anxietywatch.mobile.ui.common

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiComponentsAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun patientRowIsAnAccessibleActionWithVisibleStatus() {
        var selected = false
        composeRule.setContent {
            AnxietyWatchTheme {
                PatientRow(
                    name = "Alex",
                    status = "Desconectado",
                    heartRate = null,
                    lastSync = "Sin actualización",
                    showMissingHeartRatePlaceholder = true,
                    onClick = { selected = true },
                )
            }
        }

        composeRule.onNodeWithText("Alex").assert(expectValue(SemanticsProperties.Role, Role.Button)).performClick()
        composeRule.onNodeWithText("Desconectado").assertIsDisplayed()
        assertEquals(true, selected)
    }

    @Test
    fun alertRowIsAnAccessibleActionWithStatusText() {
        composeRule.setContent {
            AnxietyWatchTheme {
                AlertRow(
                    title = "Revisión pendiente",
                    patientName = "Alex",
                    occurredAt = "Hoy",
                    status = "Pendiente",
                )
            }
        }

        composeRule.onNodeWithText("Revisión pendiente").assert(expectValue(SemanticsProperties.Role, Role.Button))
        composeRule.onNodeWithText("Pendiente").assertIsDisplayed()
    }

    @Test
    fun patientRowKeepsContentReachableAtIncreasedFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 1.5f)) {
                AnxietyWatchTheme {
                    PatientRow(
                        name = "Alex",
                        status = "Conectado · reciente",
                        heartRate = 72,
                        lastSync = "Hoy",
                        anxietyLabel = "Ansiedad: normal",
                        onClick = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Alex").assertIsDisplayed()
        composeRule.onNodeWithText("Conectado · reciente").assertIsDisplayed()
        composeRule.onNodeWithText("Ansiedad: normal").assertIsDisplayed()
    }
}

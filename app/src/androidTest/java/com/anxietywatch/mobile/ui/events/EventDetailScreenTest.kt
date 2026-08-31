package com.anxietywatch.mobile.ui.events

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import com.anxietywatch.mobile.data.remote.EpisodeDto
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EventDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailShowsOnlyEpisodeData() {
        var wentBack = false
        composeRule.setContent {
            AnxietyWatchTheme {
                EventDetailScreen(
                    eventId = "episode-1",
                    onBack = { wentBack = true },
                    episode = EpisodeDto(
                        id = "episode-1",
                        date = "2026-08-25T10:15:00Z",
                        intensity = 42,
                        symptoms = listOf("Respiración rápida"),
                        notes = "Registro del paciente",
                    ),
                )
            }
        }

        composeRule.onNodeWithText("episode-1").assertIsDisplayed()
        composeRule.onNodeWithText("2026-08-25T10:15:00Z").assertIsDisplayed()
        composeRule.onNodeWithText("Respiración rápida").assertIsDisplayed()
        composeRule.onAllNodesWithText("Parque Central").assertCountEquals(0)
        composeRule.onAllNodesWithText("IA Confirmado").assertCountEquals(0)
        composeRule.onAllNodesWithText("128").assertCountEquals(0)
        composeRule.onNodeWithText("Volver al historial").performClick()
        assertTrue(wentBack)
    }

    @Test
    fun missingEpisodeShowsHonestUnavailableState() {
        composeRule.setContent {
            AnxietyWatchTheme { EventDetailScreen(eventId = "episode-missing") }
        }

        composeRule.onNodeWithText("Información limitada").assertIsDisplayed()
        composeRule.onNodeWithText("No existe un detalle remoto disponible para este evento.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Parque Central").assertCountEquals(0)
    }
}

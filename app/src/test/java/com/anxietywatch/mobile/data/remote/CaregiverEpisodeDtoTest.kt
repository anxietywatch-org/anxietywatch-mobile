package com.anxietywatch.mobile.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CaregiverEpisodeDtoTest {
    @Test
    fun decodesBackendEpisodeWithoutSeparateId() {
        val episode = Json.decodeFromString<CaregiverEpisodeDto>(
            """{"date":"2026-08-30T10:00:00Z","intensity":3,"symptoms":null,"notes":null,"detailsHidden":true}""",
        )

        assertEquals(episode.date, episode.id)
        assertEquals(3, episode.intensity)
        assertEquals(true, episode.detailsHidden)
    }
}

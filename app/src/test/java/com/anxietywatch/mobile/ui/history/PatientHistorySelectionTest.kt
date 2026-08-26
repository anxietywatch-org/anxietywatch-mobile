package com.anxietywatch.mobile.ui.history

import com.anxietywatch.mobile.data.remote.EpisodeDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatientHistorySelectionTest {

    @Test
    fun historySelectionUsesStableEpisodeId() {
        val episodes = listOf(
            EpisodeDto("episode-1", "2026-08-25T10:00:00Z", 20, emptyList()),
            EpisodeDto("episode-2", "2026-08-25T11:00:00Z", 40, emptyList()),
        )

        assertEquals("episode-2", findEpisodeById(episodes, "episode-2")?.id)
        assertNull(findEpisodeById(episodes, "missing"))
    }
}

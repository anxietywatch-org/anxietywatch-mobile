package com.anxietywatch.mobile.ui.home

import com.anxietywatch.mobile.data.bridge.LatestWatchSample
import com.anxietywatch.mobile.data.bridge.WatchState
import com.anxietywatch.mobile.data.remote.AnxietyLevelDto
import com.anxietywatch.mobile.data.remote.DashboardSummaryDto
import com.anxietywatch.mobile.data.remote.WeeklyRecordsDto
import com.anxietywatch.mobile.ui.common.ConnectivityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatientUiModelsTest {

    @Test
    fun bpmAndAnxietyRemainIndependent() {
        val state = homePatientUiStateFrom(
            summary = summary(current = 80),
            episodes = emptyList(),
            watchState = watchState(bpm = 72, connected = true, telemetryAt = 1_000L),
            nowMillis = 1_000L,
        )

        assertEquals(72, state.bpm)
        assertEquals("Nivel 80", state.anxiety?.label)
    }

    @Test
    fun missingBpmDoesNotBecomeZero() {
        val state = homePatientUiStateFrom(
            summary = summary(current = 20),
            episodes = emptyList(),
            watchState = watchState(bpm = null, connected = true, telemetryAt = 1_000L),
            nowMillis = 1_000L,
        )

        assertNull(state.bpm)
        assertEquals("Nivel 20", state.anxiety?.label)
    }

    @Test
    fun uiCanRepresentKnownBpmWithoutAnxiety() {
        val state = HomePatientUiState(bpm = 110, anxiety = null)

        assertEquals(110, state.bpm)
        assertNull(state.anxiety)
    }

    @Test
    fun connectivityStatesUseFreshnessPolicy() {
        assertEquals(
            ConnectivityStatus.ConnectedRecent,
            patientConnectivityFrom(watchState(72, true, 1_000L), 1_000L).status,
        )
        assertEquals(
            ConnectivityStatus.ConnectedStale,
            patientConnectivityFrom(watchState(72, true, 1_000L), 1_000L + PatientFreshnessPolicy.STALE_AFTER_MILLIS + 1).status,
        )
        assertEquals(
            ConnectivityStatus.Disconnected,
            patientConnectivityFrom(watchState(72, false, 1_000L), 1_000L).status,
        )
        assertEquals(
            ConnectivityStatus.Unknown,
            patientConnectivityFrom(watchState(null, false, null), 1_000L).status,
        )
    }

    private fun summary(current: Int) = DashboardSummaryDto(
        anxietyLevel = AnxietyLevelDto(current = current, trend = "stable"),
        weeklyRecords = WeeklyRecordsDto(used = 1, limit = 7),
        streakDays = 2,
        exercisesCompleted = 0,
    )

    private fun watchState(bpm: Int?, connected: Boolean, telemetryAt: Long?) = WatchState(
        latestSample = bpm?.let { LatestWatchSample(it, null, telemetryAt ?: 0L) },
        lastTelemetryAtMillis = telemetryAt,
        connected = connected,
        nodeName = "AnxietyWatch",
    )
}

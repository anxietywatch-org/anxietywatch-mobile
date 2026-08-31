package com.anxietywatch.mobile.ui.home

import com.anxietywatch.mobile.data.bridge.WatchState
import com.anxietywatch.mobile.data.remote.DashboardSummaryDto
import com.anxietywatch.mobile.data.remote.EpisodeDto
import com.anxietywatch.mobile.ui.common.ConnectivityStatus
import java.util.concurrent.TimeUnit

enum class PatientDataFreshness {
    Recent,
    Stale,
    Unknown,
}

data class PatientAnxietyUiState(
    val label: String,
    val detail: String,
)

data class PatientConnectivityUiState(
    val status: ConnectivityStatus,
    val deviceName: String?,
    val lastSyncLabel: String,
    val freshness: PatientDataFreshness,
)

data class HomePatientUiState(
    val bpm: Int? = null,
    val anxiety: PatientAnxietyUiState? = null,
    val connectivity: PatientConnectivityUiState = PatientConnectivityUiState(
        status = ConnectivityStatus.Unknown,
        deviceName = null,
        lastSyncLabel = "Sin lectura del reloj",
        freshness = PatientDataFreshness.Unknown,
    ),
    val episodes: List<EpisodeDto> = emptyList(),
    val streakDays: Int = 0,
    val weeklyRecordsUsed: Int = 0,
    val weeklyRecordsLimit: Int? = null,
)

object PatientFreshnessPolicy {
    /** A reading older than five minutes is shown as stale in the patient UI. */
    const val STALE_AFTER_MILLIS = 5 * 60 * 1000L
}

internal fun patientConnectivityFrom(
    watchState: WatchState,
    nowMillis: Long,
): PatientConnectivityUiState {
    val lastTelemetryAt = watchState.lastTelemetryAtMillis
    val freshness = when {
        lastTelemetryAt == null -> PatientDataFreshness.Unknown
        nowMillis - lastTelemetryAt <= PatientFreshnessPolicy.STALE_AFTER_MILLIS -> PatientDataFreshness.Recent
        else -> PatientDataFreshness.Stale
    }
    val status = when {
        watchState.connected && freshness == PatientDataFreshness.Recent -> ConnectivityStatus.ConnectedRecent
        watchState.connected && freshness == PatientDataFreshness.Stale -> ConnectivityStatus.ConnectedStale
        !watchState.connected && lastTelemetryAt != null -> ConnectivityStatus.Disconnected
        else -> ConnectivityStatus.Unknown
    }
    return PatientConnectivityUiState(
        status = status,
        deviceName = watchState.nodeName,
        lastSyncLabel = lastTelemetryAt?.let { relativeTimeLabel(it, nowMillis) } ?: "Sin lectura del reloj",
        freshness = freshness,
    )
}

internal fun homePatientUiStateFrom(
    summary: DashboardSummaryDto,
    episodes: List<EpisodeDto>,
    watchState: WatchState,
    nowMillis: Long,
): HomePatientUiState {
    return HomePatientUiState(
        bpm = watchState.latestSample?.heartRateBpm,
        anxiety = PatientAnxietyUiState(
            label = "Nivel ${summary.anxietyLevel.current}",
            detail = "Tendencia: ${summary.anxietyLevel.trend.toStatusLabel()}",
        ),
        connectivity = patientConnectivityFrom(watchState, nowMillis),
        episodes = episodes,
        streakDays = summary.streakDays,
        weeklyRecordsUsed = summary.weeklyRecords.used,
        weeklyRecordsLimit = summary.weeklyRecords.limit,
    )
}

private fun relativeTimeLabel(timestampMillis: Long, nowMillis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes((nowMillis - timestampMillis).coerceAtLeast(0L))
    return when {
        minutes == 0L -> "Ahora"
        minutes == 1L -> "Hace 1 minuto"
        else -> "Hace $minutes minutos"
    }
}

private fun String.toStatusLabel(): String = when (lowercase()) {
    "up" -> "En aumento"
    "down" -> "En descenso"
    else -> "Estable"
}

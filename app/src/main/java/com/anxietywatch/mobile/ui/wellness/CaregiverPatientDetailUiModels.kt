package com.anxietywatch.mobile.ui.wellness

import com.anxietywatch.mobile.data.caregiver.CaregiverPatientDetailSource
import com.anxietywatch.mobile.ui.common.ConnectivityStatus
import com.anxietywatch.mobile.ui.dashboard.CaregiverDataFreshness

data class CaregiverRecentEventUiModel(
    val id: String,
    val title: String,
    val description: String?,
    val occurredAt: String?,
)

data class CaregiverRecentAlertUiModel(
    val id: String,
    val title: String,
    val description: String?,
    val occurredAt: String?,
    val status: String?,
)

data class CaregiverPatientDetailUiModel(
    val id: String,
    val displayName: String,
    val bpm: Int? = null,
    val anxiety: Int? = null,
    val lastUpdated: String? = null,
    val connectivity: ConnectivityStatus? = null,
    val freshness: CaregiverDataFreshness? = null,
    val alertState: String? = null,
    val recentEvents: List<CaregiverRecentEventUiModel> = emptyList(),
    val recentAlerts: List<CaregiverRecentAlertUiModel> = emptyList(),
)

sealed interface CaregiverPatientDetailUiState {
    data object Loading : CaregiverPatientDetailUiState

    data class Content(val data: CaregiverPatientDetailUiModel) : CaregiverPatientDetailUiState

    data class Error(val message: String) : CaregiverPatientDetailUiState
}

internal fun CaregiverPatientDetailSource.toUiModel(): CaregiverPatientDetailUiModel =
    CaregiverPatientDetailUiModel(
        id = id,
        displayName = displayName,
        bpm = bpm,
        anxiety = anxiety,
        lastUpdated = lastUpdated,
        connectivity = connectivity.toConnectivityStatusOrNull(),
        freshness = freshness.toFreshnessOrNull(),
        alertState = alertState,
        recentEvents = recentEvents.map { event ->
            CaregiverRecentEventUiModel(event.id, event.title, event.description, event.occurredAt)
        },
        recentAlerts = recentAlerts.map { alert ->
            CaregiverRecentAlertUiModel(alert.id, alert.title, alert.description, alert.occurredAt, alert.status)
        },
    )

private fun String?.toConnectivityStatusOrNull(): ConnectivityStatus? = when (this?.lowercase()) {
    "connected", "conectado" -> ConnectivityStatus.ConnectedRecent
    "stale", "antiguo" -> ConnectivityStatus.ConnectedStale
    "disconnected", "desconectado" -> ConnectivityStatus.Disconnected
    "unknown", "sin información" -> ConnectivityStatus.Unknown
    else -> null
}

private fun String?.toFreshnessOrNull(): CaregiverDataFreshness? = when (this?.lowercase()) {
    "recent", "reciente" -> CaregiverDataFreshness.Recent
    "stale", "antiguo" -> CaregiverDataFreshness.Stale
    "unknown", "sin información" -> CaregiverDataFreshness.Unknown
    else -> null
}

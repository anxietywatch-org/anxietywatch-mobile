package com.anxietywatch.mobile.ui.dashboard

import com.anxietywatch.mobile.data.caregiver.CaregiverDashboardSource
import com.anxietywatch.mobile.ui.common.ConnectivityStatus

enum class CaregiverDataFreshness {
    Recent,
    Stale,
    Unknown,
}

data class CaregiverPatientUiModel(
    val id: String,
    val displayName: String,
    val bpm: Int? = null,
    val anxiety: Int? = null,
    val lastUpdated: String? = null,
    val connectivity: ConnectivityStatus? = null,
    val freshness: CaregiverDataFreshness? = null,
    val alertState: String? = null,
)

data class CaregiverDashboardUiModel(
    val patients: List<CaregiverPatientUiModel>,
)

sealed interface DashboardCaregiverUiState {
    data object Loading : DashboardCaregiverUiState

    data class Content(
        val data: CaregiverDashboardUiModel,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
    ) : DashboardCaregiverUiState

    data class Empty(
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
    ) : DashboardCaregiverUiState

    data class Error(val message: String) : DashboardCaregiverUiState
}

internal fun CaregiverDashboardSource.toUiModel(): CaregiverDashboardUiModel =
    CaregiverDashboardUiModel(
        patients = patients.map { patient ->
            CaregiverPatientUiModel(
                id = patient.id,
                displayName = patient.displayName,
                bpm = patient.bpm,
                anxiety = patient.anxiety,
                lastUpdated = patient.lastUpdated,
                connectivity = patient.connectivity.toConnectivityStatusOrNull(),
                freshness = patient.freshness.toFreshnessOrNull(),
                alertState = patient.alertState,
            )
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

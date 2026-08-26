package com.anxietywatch.mobile.ui.alerts

import com.anxietywatch.mobile.data.caregiver.CaregiverAlertSource

data class CaregiverAlertUiModel(
    val id: String,
    val patientId: String,
    val patientDisplayName: String,
    val timestamp: String? = null,
    val type: String? = null,
    val status: String? = null,
    val title: String,
    val summary: String? = null,
    val acknowledged: Boolean? = null,
    val resolved: Boolean? = null,
    val bpm: Int? = null,
    val anxiety: Int? = null,
)

typealias CaregiverAlertDetailUiModel = CaregiverAlertUiModel

sealed interface CaregiverAlertsUiState {
    data object Loading : CaregiverAlertsUiState
    data class Empty(val isRefreshing: Boolean = false, val refreshError: String? = null) : CaregiverAlertsUiState
    data class Content(
        val data: List<CaregiverAlertUiModel>,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
    ) : CaregiverAlertsUiState
    data class Error(val message: String) : CaregiverAlertsUiState
}

sealed interface CaregiverAlertDetailUiState {
    data object Loading : CaregiverAlertDetailUiState
    data class Content(val data: CaregiverAlertDetailUiModel) : CaregiverAlertDetailUiState
    data class Error(val message: String) : CaregiverAlertDetailUiState
}

internal fun CaregiverAlertSource.toUiModel() = CaregiverAlertUiModel(
    id, patientId, patientDisplayName, timestamp, type, status, title, summary,
    acknowledged, resolved, bpm, anxiety,
)

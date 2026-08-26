package com.anxietywatch.mobile.ui.profile

import com.anxietywatch.mobile.data.remote.SessionProfile

data class CaregiverProfileUiModel(
    val displayName: String?,
    val email: String?,
    val role: String,
)

sealed interface CaregiverProfileUiState {
    data object Loading : CaregiverProfileUiState
    data class Content(
        val data: CaregiverProfileUiModel,
        val isLoggingOut: Boolean = false,
        val logoutError: String? = null,
    ) : CaregiverProfileUiState
    data class Error(val message: String) : CaregiverProfileUiState
}

internal fun SessionProfile.toUiModel() = CaregiverProfileUiModel(
    displayName = displayName?.takeIf { it.isNotBlank() },
    email = email?.takeIf { it.isNotBlank() },
    role = role?.takeIf { it.isNotBlank() } ?: "family_member",
)

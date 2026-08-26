package com.anxietywatch.mobile.ui.alerts

import androidx.lifecycle.ViewModel
import com.anxietywatch.mobile.ui.common.AsyncUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class CriticalAlertUiModel(
    val patientName: String,
    val message: String,
    val location: String? = null,
    val emergencyPhone: String? = null,
)

@HiltViewModel
class CriticalAlertViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<AsyncUiState<CriticalAlertUiModel>>(AsyncUiState.Loading)
    val uiState: StateFlow<AsyncUiState<CriticalAlertUiModel>> = _uiState.asStateFlow()

    fun loadAlert(eventId: String, initialAlert: CriticalAlertUiModel? = null) {
        _uiState.value = AsyncUiState.Loading
        _uiState.value = if (eventId.isBlank() || initialAlert == null) {
            AsyncUiState.Empty
        } else {
            AsyncUiState.Success(initialAlert)
        }
    }
}

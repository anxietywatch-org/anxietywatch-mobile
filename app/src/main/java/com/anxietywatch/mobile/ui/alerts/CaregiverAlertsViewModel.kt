package com.anxietywatch.mobile.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.caregiver.CaregiverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaregiverAlertsViewModel @Inject constructor(
    private val repository: CaregiverRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CaregiverAlertsUiState>(CaregiverAlertsUiState.Loading)
    val uiState: StateFlow<CaregiverAlertsUiState> = _uiState.asStateFlow()

    init { load() }
    fun retry() = load()

    fun refresh() {
        when (val current = _uiState.value) {
            is CaregiverAlertsUiState.Content -> {
                _uiState.value = current.copy(isRefreshing = true, refreshError = null)
                refreshInBackground(current.data)
            }
            is CaregiverAlertsUiState.Empty -> {
                _uiState.value = current.copy(isRefreshing = true, refreshError = null)
                refreshInBackground(emptyList())
            }
            else -> load()
        }
    }

    private fun load() {
        _uiState.value = CaregiverAlertsUiState.Loading
        viewModelScope.launch {
            runCatching { repository.getAlerts().map { it.toUiModel() } }
                .onSuccess { updateContent(it) }
                .onFailure { _uiState.value = CaregiverAlertsUiState.Error(it.message ?: "No se pudieron cargar las alertas.") }
        }
    }

    private fun refreshInBackground(previous: List<CaregiverAlertUiModel>) {
        viewModelScope.launch {
            runCatching { repository.getAlerts().map { it.toUiModel() } }
                .onSuccess { updateContent(it) }
                .onFailure { error ->
                    val message = error.message ?: "No se pudieron actualizar las alertas."
                    _uiState.value = if (previous.isEmpty()) CaregiverAlertsUiState.Empty(refreshError = message)
                    else CaregiverAlertsUiState.Content(previous, refreshError = message)
                }
        }
    }

    private fun updateContent(alerts: List<CaregiverAlertUiModel>) {
        _uiState.value = if (alerts.isEmpty()) CaregiverAlertsUiState.Empty() else CaregiverAlertsUiState.Content(alerts)
    }
}

package com.anxietywatch.mobile.ui.dashboard

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
class DashboardCaregiverViewModel @Inject constructor(
    private val repository: CaregiverRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardCaregiverUiState>(DashboardCaregiverUiState.Loading)
    val uiState: StateFlow<DashboardCaregiverUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun retry() = loadDashboard()

    fun loadDashboard() {
        _uiState.value = DashboardCaregiverUiState.Loading
        viewModelScope.launch {
            runCatching { repository.loadDashboard().toUiModel() }
                .onSuccess { data ->
                    _uiState.value = if (data.patients.isEmpty()) {
                        DashboardCaregiverUiState.Empty()
                    } else {
                        DashboardCaregiverUiState.Content(data)
                    }
                }
                .onFailure { error ->
                    _uiState.value = DashboardCaregiverUiState.Error(
                        error.message ?: "No se pudo cargar el dashboard del cuidador.",
                    )
                }
        }
    }

    fun refresh() {
        val current = _uiState.value
        when (current) {
            is DashboardCaregiverUiState.Content -> {
                _uiState.value = current.copy(isRefreshing = true, refreshError = null)
                refreshInBackground(current.data)
            }
            is DashboardCaregiverUiState.Empty -> {
                _uiState.value = current.copy(isRefreshing = true, refreshError = null)
                refreshInBackground(null)
            }
            DashboardCaregiverUiState.Loading,
            is DashboardCaregiverUiState.Error,
            -> loadDashboard()
        }
    }

    private fun refreshInBackground(previous: CaregiverDashboardUiModel?) {
        viewModelScope.launch {
            runCatching { repository.loadDashboard().toUiModel() }
                .onSuccess { data ->
                    _uiState.value = if (data.patients.isEmpty()) {
                        DashboardCaregiverUiState.Empty()
                    } else {
                        DashboardCaregiverUiState.Content(data)
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: "No se pudo actualizar el dashboard."
                    _uiState.value = if (previous == null) {
                        DashboardCaregiverUiState.Empty(refreshError = message)
                    } else {
                        DashboardCaregiverUiState.Content(previous, refreshError = message)
                    }
                }
        }
    }
}

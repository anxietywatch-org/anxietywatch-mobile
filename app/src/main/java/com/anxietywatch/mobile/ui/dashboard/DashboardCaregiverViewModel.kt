package com.anxietywatch.mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardCaregiverData(
    val caregiverName: String,
    val patients: List<CaregiverPatientUiModel>,
)

sealed interface DashboardCaregiverUiState {
    data object Idle : DashboardCaregiverUiState
    data object Loading : DashboardCaregiverUiState
    data class Success(val data: DashboardCaregiverData) : DashboardCaregiverUiState
    data class Error(val message: String) : DashboardCaregiverUiState
}

@HiltViewModel
class DashboardCaregiverViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardCaregiverUiState>(DashboardCaregiverUiState.Idle)
    val uiState: StateFlow<DashboardCaregiverUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        _uiState.update { DashboardCaregiverUiState.Loading }
        viewModelScope.launch {
            // TODO: conectar al endpoint confirmado de pacientes asignados del backend.
            _uiState.update {
                DashboardCaregiverUiState.Success(
                    DashboardCaregiverData(
                        caregiverName = "María",
                        patients = listOf(
                            CaregiverPatientUiModel("patient-alex", "Alex", "Estado de calma", 72, "Hace 2 min"),
                            CaregiverPatientUiModel("patient-sofia", "Sofía", "Actividad elevada", 96, "Hace 5 min"),
                        ),
                    ),
                )
            }
        }
    }
}

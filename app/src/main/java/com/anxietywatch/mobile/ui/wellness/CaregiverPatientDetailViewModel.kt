package com.anxietywatch.mobile.ui.wellness

import androidx.lifecycle.SavedStateHandle
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
class CaregiverPatientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CaregiverRepository,
) : ViewModel() {
    private val patientId: String = savedStateHandle.get<String>("patientId").orEmpty()
    private val _uiState = MutableStateFlow<CaregiverPatientDetailUiState>(CaregiverPatientDetailUiState.Loading)
    val uiState: StateFlow<CaregiverPatientDetailUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun refresh() = load(isManualRefresh = true)

    private fun load(isManualRefresh: Boolean = false) {
        if (!isManualRefresh) _uiState.value = CaregiverPatientDetailUiState.Loading
        _isRefreshing.value = isManualRefresh
        viewModelScope.launch {
            if (patientId.isBlank()) {
                _uiState.value = CaregiverPatientDetailUiState.Error("Paciente no encontrado.")
                _isRefreshing.value = false
                return@launch
            }
            runCatching { repository.getPatientDetail(patientId) }
                .onSuccess { detail ->
                    _uiState.value = detail?.let {
                        CaregiverPatientDetailUiState.Content(it.toUiModel())
                    } ?: CaregiverPatientDetailUiState.Error("Paciente no encontrado.")
                }
                .onFailure { error ->
                    _uiState.value = CaregiverPatientDetailUiState.Error(
                        error.message ?: "No se pudo cargar el paciente.",
                    )
                }
            _isRefreshing.value = false
        }
    }
}

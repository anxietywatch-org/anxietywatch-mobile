package com.anxietywatch.mobile.ui.alerts

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
class CaregiverAlertDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CaregiverRepository,
) : ViewModel() {
    private val alertId = savedStateHandle.get<String>("alertId").orEmpty()
    private val _uiState = MutableStateFlow<CaregiverAlertDetailUiState>(CaregiverAlertDetailUiState.Loading)
    val uiState: StateFlow<CaregiverAlertDetailUiState> = _uiState.asStateFlow()

    init { load() }
    fun retry() = load()

    private fun load() {
        _uiState.value = CaregiverAlertDetailUiState.Loading
        viewModelScope.launch {
            if (alertId.isBlank()) {
                _uiState.value = CaregiverAlertDetailUiState.Error("Alerta no encontrada")
                return@launch
            }
            runCatching { repository.getAlertDetail(alertId) }
                .onSuccess { alert -> _uiState.value = alert?.let { CaregiverAlertDetailUiState.Content(it.toUiModel()) } ?: CaregiverAlertDetailUiState.Error("Alerta no encontrada") }
                .onFailure { _uiState.value = CaregiverAlertDetailUiState.Error(it.message ?: "No se pudo cargar la alerta.") }
        }
    }
}

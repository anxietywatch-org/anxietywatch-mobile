package com.anxietywatch.mobile.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.remote.CaregiverSessionSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaregiverProfileViewModel @Inject constructor(
    private val session: CaregiverSessionSource,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CaregiverProfileUiState>(CaregiverProfileUiState.Loading)
    val uiState: StateFlow<CaregiverProfileUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    fun logout(onSuccess: () -> Unit = {}) {
        val current = _uiState.value
        if (current !is CaregiverProfileUiState.Content || current.isLoggingOut) return
        _uiState.value = current.copy(isLoggingOut = true, logoutError = null)
        viewModelScope.launch {
            runCatching { session.clearSession() }
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        logoutError = error.message ?: "No se pudo cerrar la sesión.",
                    )
                }
        }
    }

    private fun load() {
        _uiState.value = CaregiverProfileUiState.Loading
        viewModelScope.launch {
            runCatching { session.profileFlow.first().toUiModel() }
                .onSuccess { _uiState.value = CaregiverProfileUiState.Content(it) }
                .onFailure { error -> _uiState.value = CaregiverProfileUiState.Error(error.message ?: "No se pudo cargar el perfil.") }
        }
    }
}

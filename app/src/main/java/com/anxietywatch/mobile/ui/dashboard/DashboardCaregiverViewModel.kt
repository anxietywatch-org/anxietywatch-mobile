package com.anxietywatch.mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.caregiver.CaregiverRepository
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.LinkCaregiverPatientRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

sealed interface LinkPatientUiState {
    data object Idle : LinkPatientUiState
    data object Loading : LinkPatientUiState
    data class Error(val message: String) : LinkPatientUiState
    data object Success : LinkPatientUiState
}

@HiltViewModel
class DashboardCaregiverViewModel @Inject constructor(
    private val repository: CaregiverRepository,
    private val api: AnxietyWatchApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardCaregiverUiState>(DashboardCaregiverUiState.Loading)
    val uiState: StateFlow<DashboardCaregiverUiState> = _uiState.asStateFlow()

    private val _linkPatientUiState = MutableStateFlow<LinkPatientUiState>(LinkPatientUiState.Idle)
    val linkPatientUiState: StateFlow<LinkPatientUiState> = _linkPatientUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init { loadDashboard() }

    fun retry() = loadDashboard()

    fun loadDashboard(isManualRefresh: Boolean = false) {
        val previous = _uiState.value
        if (!isManualRefresh) _uiState.value = DashboardCaregiverUiState.Loading
        _isRefreshing.value = isManualRefresh
        viewModelScope.launch {
            try {
                val data = repository.loadDashboard().toUiModel()
                _uiState.value = if (data.patients.isEmpty()) {
                    DashboardCaregiverUiState.Empty()
                } else {
                    DashboardCaregiverUiState.Content(data)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val message = error.message ?: "No se pudo cargar el dashboard del cuidador."
                _uiState.value = if (isManualRefresh) {
                    when (previous) {
                        is DashboardCaregiverUiState.Content -> previous.copy(refreshError = message)
                        is DashboardCaregiverUiState.Empty -> previous.copy(refreshError = message)
                        else -> DashboardCaregiverUiState.Error(message)
                    }
                } else {
                    DashboardCaregiverUiState.Error(message)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refresh() = loadDashboard(isManualRefresh = true)

    fun linkPatient(rawCode: String) {
        val sanitized = sanitizeCode(rawCode)
        val validationError = when {
            sanitized.isBlank() -> "Ingresa el código que te compartieron."
            sanitized.length < MIN_CODE_LENGTH -> "El código es muy corto. Revisa que lo hayas copiado completo."
            sanitized.length > MAX_CODE_LENGTH -> "Ese código es demasiado largo para ser válido."
            else -> null
        }
        if (validationError != null) {
            _linkPatientUiState.value = LinkPatientUiState.Error(validationError)
            return
        }

        _linkPatientUiState.value = LinkPatientUiState.Loading
        viewModelScope.launch {
            try {
                api.linkCaregiverPatient(LinkCaregiverPatientRequest(sanitized))
                _linkPatientUiState.value = LinkPatientUiState.Success
                loadDashboard()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val message = when {
                    error is HttpException && error.code() == 404 ->
                        "Ese código no es válido. Revisa que esté escrito tal cual te lo compartieron."
                    error is HttpException && error.code() == 409 ->
                        "Ese código ya se usó o expiró. Pide uno nuevo a quien te lo compartió."
                    error is HttpException && error.code() == 429 ->
                        "Demasiados intentos. Espera un momento antes de volver a intentar."
                    else -> "No pudimos vincular al paciente. Revisa tu conexión e intenta de nuevo."
                }
                _linkPatientUiState.value = LinkPatientUiState.Error(message)
            }
        }
    }

    fun dismissLinkPatientError() {
        if (_linkPatientUiState.value is LinkPatientUiState.Error) {
            _linkPatientUiState.value = LinkPatientUiState.Idle
        }
    }

    private fun sanitizeCode(rawCode: String): String =
        rawCode.trim().uppercase().filter { it.isLetterOrDigit() || it == '-' }

    private companion object {
        const val MIN_CODE_LENGTH = 4
        const val MAX_CODE_LENGTH = 20
    }
}

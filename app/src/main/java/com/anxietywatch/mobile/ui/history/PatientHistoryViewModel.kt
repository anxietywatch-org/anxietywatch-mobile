package com.anxietywatch.mobile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.EpisodeDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

sealed interface PatientHistoryUiState {
    data object Idle : PatientHistoryUiState
    data object Loading : PatientHistoryUiState
    data class Success(val episodes: List<EpisodeDto>) : PatientHistoryUiState
    data class Error(val message: String) : PatientHistoryUiState
}

@HiltViewModel
class PatientHistoryViewModel @Inject constructor(
    private val api: AnxietyWatchApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PatientHistoryUiState>(PatientHistoryUiState.Idle)
    val uiState: StateFlow<PatientHistoryUiState> = _uiState.asStateFlow()

    init { loadHistory() }

    fun loadHistory() {
        _uiState.update { PatientHistoryUiState.Loading }
        viewModelScope.launch {
            runCatching { api.getEpisodes(range = 7) }
                .onSuccess { episodes -> _uiState.update { PatientHistoryUiState.Success(episodes) } }
                .onFailure { error ->
                    val message = if (error is HttpException && error.code() == 401) {
                        "Tu sesión expiró. Ingresa nuevamente tu código."
                    } else {
                        "No pudimos cargar tu historial. Revisa tu conexión e inténtalo de nuevo."
                    }
                    _uiState.update { PatientHistoryUiState.Error(message) }
                }
        }
    }
}

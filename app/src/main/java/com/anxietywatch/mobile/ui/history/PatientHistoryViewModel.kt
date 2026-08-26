package com.anxietywatch.mobile.ui.history

import android.util.Log
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
    data class Success(
        val episodes: List<EpisodeDto>,
        val refreshing: Boolean = false,
        val refreshError: String? = null,
    ) : PatientHistoryUiState
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
        loadHistoryInternal(isRefresh = false)
    }

    fun refresh() {
        if (_uiState.value is PatientHistoryUiState.Success) {
            _uiState.update { state ->
                (state as PatientHistoryUiState.Success).copy(refreshing = true, refreshError = null)
            }
            loadHistoryInternal(isRefresh = true)
        } else {
            loadHistory()
        }
    }

    private fun loadHistoryInternal(isRefresh: Boolean) {
        if (!isRefresh) {
            Log.d(TAG, "PatientHistory state=LOADING refreshing=false")
            _uiState.update { PatientHistoryUiState.Loading }
        } else {
            val count = (_uiState.value as? PatientHistoryUiState.Success)?.episodes?.size ?: 0
            Log.d(TAG, "PatientHistory state=CONTENT count=$count refreshing=true")
        }
        viewModelScope.launch {
            runCatching { api.getEpisodes(range = 7) }
                .onSuccess { episodes ->
                    val state = if (episodes.isEmpty()) "EMPTY" else "CONTENT"
                    Log.d(TAG, "PatientHistory state=$state count=${episodes.size} refreshing=false")
                    _uiState.update { PatientHistoryUiState.Success(episodes) }
                }
                .onFailure { error ->
                    if (isRefresh && _uiState.value is PatientHistoryUiState.Success) {
                        val count = (_uiState.value as PatientHistoryUiState.Success).episodes.size
                        Log.d(TAG, "PatientHistory state=CONTENT count=$count refreshing=false refreshError=true")
                        _uiState.update { state ->
                            (state as PatientHistoryUiState.Success).copy(
                                refreshing = false,
                                refreshError = "No pudimos actualizar tu historial. Revisa tu conexión e inténtalo de nuevo.",
                            )
                        }
                        return@onFailure
                    }
                    val message = if (error is HttpException && error.code() == 401) {
                        "Tu sesión expiró. Ingresa nuevamente tu código."
                    } else {
                        "No pudimos cargar tu historial. Revisa tu conexión e inténtalo de nuevo."
                    }
                    Log.d(TAG, "PatientHistory state=ERROR refreshing=$isRefresh")
                    _uiState.update { PatientHistoryUiState.Error(message) }
                }
        }
    }

    private companion object {
        const val TAG = "AnxietyWatchUi"
    }
}

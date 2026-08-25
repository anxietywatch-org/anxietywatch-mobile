package com.anxietywatch.mobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.bridge.WatchStateRepository
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

sealed interface HomePatientNetworkUiState {
    data object Idle : HomePatientNetworkUiState
    data object Loading : HomePatientNetworkUiState
    data class Success(val data: HomePatientData) : HomePatientNetworkUiState
    data class Error(val message: String) : HomePatientNetworkUiState
}

data class HomePatientData(
    val state: HomePatientUiState,
)

@HiltViewModel
class HomePatientViewModel @Inject constructor(
    private val api: AnxietyWatchApi,
    private val watchStateRepository: WatchStateRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomePatientNetworkUiState>(HomePatientNetworkUiState.Idle)
    val uiState: StateFlow<HomePatientNetworkUiState> = _uiState.asStateFlow()

    init {
        loadHome()
        viewModelScope.launch {
            watchStateRepository.state.collect { watchState ->
                _uiState.update { current ->
                    if (current is HomePatientNetworkUiState.Success) {
                        current.copy(
                            data = current.data.copy(
                                state = current.data.state.copy(
                                    bpm = watchState.latestSample?.heartRateBpm,
                                    watchSampleTimestamp = watchState.latestSample?.capturedAt,
                                ),
                            ),
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun loadHome() {
        _uiState.update { HomePatientNetworkUiState.Loading }
        viewModelScope.launch {
            runCatching {
                val summary = api.getDashboardSummary()
                val episodes = api.getEpisodes(range = 7)
                HomePatientData(
                    state = HomePatientUiState(
                        bpm = watchStateRepository.state.value.latestSample?.heartRateBpm,
                        statusLabel = "Estado: ${summary.anxietyLevel.trend.toStatusLabel()}",
                        statusMessage = "Nivel de ansiedad actual: ${summary.anxietyLevel.current}.",
                        episodes = episodes,
                        streakDays = summary.streakDays,
                        weeklyRecordsUsed = summary.weeklyRecords.used,
                        weeklyRecordsLimit = summary.weeklyRecords.limit,
                    ),
                )
            }.onSuccess { data ->
                _uiState.update { HomePatientNetworkUiState.Success(data) }
            }.onFailure { error ->
                val message = if (error is HttpException && error.code() == 401) {
                    "Tu sesión expiró. Ingresa nuevamente tu código."
                } else {
                    "No pudimos cargar tu resumen. Revisa tu conexión e inténtalo de nuevo."
                }
                _uiState.update { HomePatientNetworkUiState.Error(message) }
            }
        }
    }
}

private fun String.toStatusLabel(): String = when (lowercase()) {
    "up" -> "En aumento"
    "down" -> "En descenso"
    else -> "Estable"
}

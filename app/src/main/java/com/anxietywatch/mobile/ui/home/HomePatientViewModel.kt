package com.anxietywatch.mobile.ui.home

import android.util.Log
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
    data class Success(
        val data: HomePatientData,
        val refreshing: Boolean = false,
        val refreshError: String? = null,
    ) : HomePatientNetworkUiState
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
                                    connectivity = patientConnectivityFrom(watchState, System.currentTimeMillis()),
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
        loadHomeInternal(isRefresh = false)
    }

    fun refresh() {
        if (_uiState.value is HomePatientNetworkUiState.Success) {
            _uiState.update { state ->
                (state as HomePatientNetworkUiState.Success).copy(refreshing = true, refreshError = null)
            }
            loadHomeInternal(isRefresh = true)
        } else {
            loadHome()
        }
    }

    private fun loadHomeInternal(isRefresh: Boolean) {
        if (!isRefresh) {
            Log.d(TAG, "PatientHome state=LOADING refreshing=false")
            _uiState.update { HomePatientNetworkUiState.Loading }
        } else {
            val hasBpm = (_uiState.value as? HomePatientNetworkUiState.Success)?.data?.state?.bpm != null
            Log.d(TAG, "PatientHome state=CONTENT refreshing=true hasBpm=$hasBpm")
        }
        viewModelScope.launch {
            runCatching {
                val summary = api.getDashboardSummary()
                val episodes = api.getEpisodes(range = 7)
                HomePatientData(
                    state = homePatientUiStateFrom(
                        summary = summary,
                        episodes = episodes,
                        watchState = watchStateRepository.state.value,
                        nowMillis = System.currentTimeMillis(),
                    ),
                )
            }.onSuccess { data ->
                Log.d(
                    TAG,
                    "PatientHome state=CONTENT refreshing=false hasBpm=${data.state.bpm != null}",
                )
                _uiState.update { HomePatientNetworkUiState.Success(data) }
            }.onFailure { error ->
                if (isRefresh && _uiState.value is HomePatientNetworkUiState.Success) {
                    val hasBpm = (_uiState.value as HomePatientNetworkUiState.Success).data.state.bpm != null
                    Log.d(TAG, "PatientHome state=CONTENT refreshing=false hasBpm=$hasBpm refreshError=true")
                    _uiState.update { state ->
                        (state as HomePatientNetworkUiState.Success).copy(
                            refreshing = false,
                            refreshError = "No pudimos actualizar tu resumen. Revisa tu conexión e inténtalo de nuevo.",
                        )
                    }
                    return@onFailure
                }
                val message = if (error is HttpException && error.code() == 401) {
                    "Tu sesión expiró. Ingresa nuevamente tu código."
                } else {
                    "No pudimos cargar tu resumen. Revisa tu conexión e inténtalo de nuevo."
                }
                Log.d(TAG, "PatientHome state=ERROR refreshing=$isRefresh")
                _uiState.update { HomePatientNetworkUiState.Error(message) }
            }
        }
    }

    private companion object {
        const val TAG = "AnxietyWatchUi"
    }
}

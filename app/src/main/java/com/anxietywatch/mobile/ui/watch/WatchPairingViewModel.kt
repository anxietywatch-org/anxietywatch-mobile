package com.anxietywatch.mobile.ui.watch

import androidx.lifecycle.ViewModel
import android.content.Context
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NearbyWatch(val name: String, val signal: String, val distance: String)

sealed interface WatchPairingUiState {
    data object Idle : WatchPairingUiState
    data object Loading : WatchPairingUiState
    data class Success(val devices: List<NearbyWatch>) : WatchPairingUiState
    data class Error(val message: String) : WatchPairingUiState
}

@HiltViewModel
class WatchPairingViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WatchPairingUiState>(WatchPairingUiState.Idle)
    val uiState: StateFlow<WatchPairingUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        _uiState.update { WatchPairingUiState.Loading }
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                _uiState.update {
                    WatchPairingUiState.Success(
                        nodes.map { node ->
                            NearbyWatch(
                                name = node.displayName,
                                signal = if (node.isNearby) "Señal cercana" else "Conectado recientemente",
                                distance = if (node.isNearby) "Cerca" else "Fuera de alcance",
                            )
                        },
                    )
                }
            }
            .addOnFailureListener { error ->
                _uiState.update { WatchPairingUiState.Error("No pudimos buscar relojes: ${error.message.orEmpty()}") }
            }
    }
}

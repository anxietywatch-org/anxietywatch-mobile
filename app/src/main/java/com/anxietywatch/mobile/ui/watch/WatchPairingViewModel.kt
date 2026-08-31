package com.anxietywatch.mobile.ui.watch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.bridge.MonitoringSessionContext
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class NearbyWatch(val nodeId: String, val name: String, val signal: String, val distance: String)

sealed interface WatchPairingUiState {
    data object Idle : WatchPairingUiState
    data object Discovering : WatchPairingUiState
    data class Ready(val devices: List<NearbyWatch>) : WatchPairingUiState
    data class Pairing(val nodeId: String) : WatchPairingUiState
    data class Paired(val deviceId: String, val nodeId: String) : WatchPairingUiState
    data class Error(val message: String) : WatchPairingUiState
}

@HiltViewModel
class WatchPairingViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionContext: MonitoringSessionContext,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WatchPairingUiState>(WatchPairingUiState.Idle)
    val uiState: StateFlow<WatchPairingUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            sessionContext.pairedDeviceIdFlow.collect { deviceId ->
                if (deviceId != null && _uiState.value is WatchPairingUiState.Pairing) {
                    sessionContext.lastKnownWearNodeId()?.let { nodeId ->
                        _uiState.value = WatchPairingUiState.Paired(deviceId, nodeId)
                    }
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { WatchPairingUiState.Discovering }
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                _uiState.update {
                    WatchPairingUiState.Ready(
                        nodes.map { node ->
                            NearbyWatch(
                                nodeId = node.id,
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

    fun hasExistingPairing(): Boolean = sessionContext.pairedDeviceId() != null

    fun pairSelected(device: NearbyWatch, replaceExisting: Boolean = false) {
        if (hasExistingPairing() && !replaceExisting) return
        viewModelScope.launch {
            val nonce = sessionContext.beginPairing(device.nodeId)
            _uiState.value = WatchPairingUiState.Pairing(device.nodeId)
            val payload = JSONObject()
                .put("schemaVersion", 1)
                .put("pairingNonce", nonce)
                .toString()
                .toByteArray(Charsets.UTF_8)
            Wearable.getMessageClient(context).sendMessage(
                device.nodeId,
                PAIRING_REQUEST_ROUTE,
                payload,
            ).addOnFailureListener {
                viewModelScope.launch {
                    sessionContext.clearPendingPairing()
                    _uiState.value = WatchPairingUiState.Error("No se pudo iniciar la vinculación")
                }
            }
        }
    }

    private companion object {
        const val PAIRING_REQUEST_ROUTE = "/fog/v1/pairing/request"
    }
}

package com.anxietywatch.mobile.ui.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.anxietywatch.mobile.data.bridge.MonitoringSessionContext
import com.anxietywatch.mobile.data.bridge.WatchStateRepository
import com.anxietywatch.mobile.data.bridge.pairingUnpairPayload
import com.anxietywatch.mobile.service.MonitoringForegroundService
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageWatchUiState(
    val hapticNotifications: Boolean = true,
    val pairingStored: Boolean = false,
    val deviceName: String? = null,
    val lastSync: String = "Sin datos del reloj aún",
    val connected: Boolean = false,
    val refreshing: Boolean = false,
)

@HiltViewModel
class ManageWatchViewModel @Inject constructor(
    private val watchStateRepository: WatchStateRepository,
    private val sessionContext: MonitoringSessionContext,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManageWatchUiState())
    val uiState: StateFlow<ManageWatchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionContext.pairedDeviceIdFlow.collect { deviceId ->
                _uiState.update { it.copy(pairingStored = deviceId != null) }
            }
        }
        viewModelScope.launch {
            watchStateRepository.state.collect { watchState ->
                _uiState.update {
                    it.copy(
                        connected = watchState.connected,
                        deviceName = watchState.nodeName,
                        lastSync = watchState.lastTelemetryAtMillis
                            ?.let(::formatLastSync)
                            ?: "Sin datos del reloj aún",
                    )
                }
            }
        }
        watchStateRepository.refresh()
    }

    fun setHapticNotifications(enabled: Boolean) {
        _uiState.update { it.copy(hapticNotifications = enabled) }
    }

    fun forceSync() {
        _uiState.update { it.copy(refreshing = true) }
        watchStateRepository.refresh()
        viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(refreshing = false) }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            val nodeId = sessionContext.lastKnownWearNodeId()
            if (nodeId != null) {
                Wearable.getMessageClient(context).sendMessage(
                    nodeId,
                    PAIRING_UNPAIR_ROUTE,
                    pairingUnpairPayload(),
                )
            }
            sessionContext.clearPairing()
            MonitoringForegroundService.stop(context)
            watchStateRepository.refresh()
        }
    }

    private companion object {
        const val PAIRING_UNPAIR_ROUTE = "/fog/v1/pairing/unpair"
    }
}

private fun formatLastSync(createdAtMillis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(
        (System.currentTimeMillis() - createdAtMillis).coerceAtLeast(0L),
    )
    return when (minutes) {
        0L -> "Ahora"
        1L -> "Hace 1 minuto"
        else -> "Hace $minutes minutos"
    }
}

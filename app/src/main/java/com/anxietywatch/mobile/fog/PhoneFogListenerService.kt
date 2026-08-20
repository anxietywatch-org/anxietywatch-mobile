package com.anxietywatch.mobile.fog

import android.util.Log
import com.anxietywatch.mobile.network.NetworkModule
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

private object FogPaths {
    const val TELEMETRY_PREFIX = "/fog/v1/telemetry/"
    const val SOS_PREFIX = "/fog/v1/sos/"
    const val SOS_CANCEL_PREFIX = "/fog/v1/sos/cancel/"
    const val SUSPECTED_PREFIX = "/fog/v1/events/suspected/"
    const val CAPABILITIES = "/fog/v1/capabilities"

    const val ACK_TELEMETRY_PREFIX = "/fog/v1/ack/telemetry/"
    const val ACK_SOS_PREFIX = "/fog/v1/ack/sos/"
    const val ACK_SOS_CANCEL_PREFIX = "/fog/v1/ack/sos-cancel/"
}
private const val ANOMALY_BPM_THRESHOLD = 120.0

class PhoneFogListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            if (!path.startsWith(FogPaths.TELEMETRY_PREFIX)) continue

            val batchId = path.removePrefix(FogPaths.TELEMETRY_PREFIX)
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val payloadBytes = dataMap.getByteArray("payload") ?: continue
            val json = String(payloadBytes, Charsets.UTF_8)
            val nodeId = event.dataItem.uri.host ?: continue

            Log.i(TAG, "Telemetría recibida real del reloj, batchId=$batchId")
            handleTelemetry(batchId, json, nodeId)
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val json = String(messageEvent.data, Charsets.UTF_8)
        when {
            path.startsWith(FogPaths.SOS_CANCEL_PREFIX) -> {
                val eventId = path.removePrefix(FogPaths.SOS_CANCEL_PREFIX)
                handleSos(eventId, json, isCancel = true, nodeId = messageEvent.sourceNodeId)
            }
            path.startsWith(FogPaths.SOS_PREFIX) -> {
                val eventId = path.removePrefix(FogPaths.SOS_PREFIX)
                handleSos(eventId, json, isCancel = false, nodeId = messageEvent.sourceNodeId)
            }
        }
    }

    private fun handleTelemetry(batchId: String, json: String, nodeId: String) {
        scope.launch {
            try {
                val envelope = JSONObject(json)
                val records = envelope.optJSONArray("records") ?: return@launch
                var latestHeartRate: Double? = null

                for (i in 0 until records.length()) {
                    val record = records.getJSONObject(i)
                    val type = record.optString("type")
                    val payload = record.optJSONObject("payload") ?: continue
                    if (type == "heart_rate") {
                        val bpm = payload.optDouble("bpm", Double.NaN)
                        if (!bpm.isNaN()) latestHeartRate = bpm
                    }
                }

                latestHeartRate?.let { bpm ->
                    NetworkModule.getSessionManager().saveLatestHeartRate(bpm.toInt())
                    // Umbral real de anomalía: BPM elevado en reposo.
                    // Este valor es un punto de partida razonable, no una decisión clínica --
                    // se puede ajustar cuando haya más datos reales de referencia.
                    if (bpm >= ANOMALY_BPM_THRESHOLD) {
                        NetworkModule.getSessionManager().setAnomalyPending(true)
                    }
                }

                sendAck(nodeId, FogPaths.ACK_TELEMETRY_PREFIX + batchId)
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando telemetría real: ${e.message}")
            }
        }
    }

    private fun handleSos(eventId: String, json: String, isCancel: Boolean, nodeId: String) {
        scope.launch {
            try {
                Log.i(TAG, "Evento SOS real recibido del reloj: eventId=$eventId cancel=$isCancel json=$json")
                NetworkModule.getSessionManager().saveLastSosEvent(eventId, isCancel)

                val ackPath = if (isCancel) FogPaths.ACK_SOS_CANCEL_PREFIX + eventId else FogPaths.ACK_SOS_PREFIX + eventId
                sendAck(nodeId, ackPath)
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando SOS real: ${e.message}")
            }
        }
    }

    private fun sendAck(nodeId: String, path: String) {
        try {
            Wearable.getMessageClient(applicationContext)
                .sendMessage(nodeId, path, ByteArray(0))
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo enviar ACK real: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "PhoneFogListener"
    }
}
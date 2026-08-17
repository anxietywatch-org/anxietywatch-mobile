package com.anxietywatch.mobile.data.bridge

import android.util.Log
import com.anxietywatch.mobile.data.local.AppDatabase
import com.anxietywatch.mobile.data.local.PendingSosEventEntity
import com.anxietywatch.mobile.data.local.PendingTelemetryBatchEntity
import com.anxietywatch.mobile.data.local.SyncStatus
import com.anxietywatch.mobile.data.remote.AccelerometerSampleDto
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.CreateTelemetryBatchRequest
import com.anxietywatch.mobile.data.remote.SampleQualityDto
import com.anxietywatch.mobile.data.remote.TelemetrySampleDto
import com.anxietywatch.mobile.data.remote.TriggerSosRequest
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.time.Instant
import javax.inject.Inject

/**
 * Contraparte exacta, del lado del telefono, de `WatchDataListenerService.kt` en apps/wear.
 *
 * OFFLINE-FIRST DE VERDAD (no solo en el nombre): cada lote de telemetria y cada evento SOS
 * se INSERTA en Room ANTES de intentar la red -- si el POST falla o no hay conexion, el dato
 * ya esta guardado con syncStatus=PENDING y BackupSyncWorker lo reintentara despues. El ACK
 * al reloj solo se manda cuando la API confirma, nunca antes.
 */
@AndroidEntryPoint
class PhoneDataLayerListenerService : WearableListenerService() {

    @Inject
    lateinit var api: AnxietyWatchApi

    @Inject
    lateinit var sessionContext: MonitoringSessionContext

    @Inject
    lateinit var database: AppDatabase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != EVENT_ROUTE) return
        val payload = String(messageEvent.data)
        scope.launch { handleEvent(payload, messageEvent.sourceNodeId) }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val uri = event.dataItem.uri
            if (uri.path?.startsWith(TELEMETRY_ROUTE_PREFIX) != true) continue
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val payloadBytes = dataMap.getByteArray("payload") ?: continue
            val payload = String(payloadBytes)
            val sourceNodeId = event.dataItem.uri.host ?: continue
            scope.launch { handleTelemetryBatch(payload, sourceNodeId) }
        }
        dataEvents.release()
    }

    // ---- Eventos (OBSERVING / crisis / SOS) --------------------------------------------

    private suspend fun handleEvent(rawJson: String, sourceNodeId: String) {
        val obj = runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull() ?: return
        val eventId = obj["eventId"]?.jsonPrimitive?.contentOrNull ?: return
        val state = obj["state"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val sosState = obj["sosState"]?.jsonPrimitive?.contentOrNull

        Log.i(TAG, "Evento del reloj: eventId=$eventId state=$state sosState=$sosState")

        val isCrisis = state.equals("INTERVENTION", ignoreCase = true) ||
            state.equals("SOS_REQUESTED", ignoreCase = true) ||
            state.equals("SOS_ACTIVE", ignoreCase = true)

        if (!isCrisis) {
            sendAck(sourceNodeId, eventId = eventId)
            return
        }

        val request = TriggerSosRequest(
            eventId = eventId,
            deviceId = sessionContext.pairedDeviceId(),
            userId = null,
            triggeredAt = Instant.now().toString(),
            source = "WATCH",
            reason = "Deteccion automatica del reloj: $state",
        )

        // 1) Persiste PRIMERO, pase lo que pase con la red.
        database.pendingUploadDao().insertSosEvent(
            PendingSosEventEntity(
                eventId = eventId,
                requestJson = json.encodeToString(request),
                createdAtMillis = System.currentTimeMillis(),
            ),
        )

        // 2) Intenta subir inmediato -- si falla, queda PENDING y BackupSyncWorker reintenta.
        runCatching { api.triggerSos(request) }
            .onSuccess {
                database.pendingUploadDao().updateSosEventStatus(eventId, SyncStatus.SYNCED)
                sendAck(sourceNodeId, eventId = eventId)
            }
            .onFailure { error ->
                Log.w(TAG, "No se pudo subir el SOS, queda en cola local: ${error.message}")
            }
    }

    // ---- Telemetria (lotes cada ~30s) --------------------------------------------------

    private suspend fun handleTelemetryBatch(rawJson: String, sourceNodeId: String) {
        val obj = runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull() ?: return
        val batchId = obj["batchId"]?.jsonPrimitive?.contentOrNull ?: return
        val records = obj["records"]?.jsonArray ?: JsonArray(emptyList())

        val samplesByTimestamp = linkedMapOf<Long, MutableTelemetrySample>()
        for (recordElement in records) {
            val record = recordElement.jsonObject
            val capturedAt = record["capturedAt"]?.jsonPrimitive?.long ?: continue
            val type = record["type"]?.jsonPrimitive?.contentOrNull ?: continue
            val payload = record["payload"] as? JsonObject ?: continue
            val sample = samplesByTimestamp.getOrPut(capturedAt) { MutableTelemetrySample() }
            applyReading(sample, type, payload)
        }

        if (samplesByTimestamp.isEmpty()) {
            sendAck(sourceNodeId, batchId = batchId)
            return
        }

        val orderedTimestamps = samplesByTimestamp.keys.sorted()
        val samples = orderedTimestamps.map { ts -> samplesByTimestamp.getValue(ts).toDto(ts) }

        val request = CreateTelemetryBatchRequest(
            batchId = batchId,
            deviceId = sessionContext.pairedDeviceId(),
            userId = null,
            sessionId = sessionContext.currentSessionId(),
            startedAt = Instant.ofEpochMilli(orderedTimestamps.first()).toString(),
            endedAt = Instant.ofEpochMilli(orderedTimestamps.last()).toString(),
            sequence = sessionContext.nextSequence(),
            samples = samples,
        )

        database.pendingUploadDao().insertTelemetryBatch(
            PendingTelemetryBatchEntity(
                batchId = batchId,
                requestJson = json.encodeToString(request),
                createdAtMillis = System.currentTimeMillis(),
            ),
        )

        runCatching { api.sendTelemetryBatch(request) }
            .onSuccess {
                database.pendingUploadDao().updateTelemetryBatchStatus(batchId, SyncStatus.SYNCED)
                sendAck(sourceNodeId, batchId = batchId)
            }
            .onFailure { error ->
                Log.w(TAG, "No se pudo subir la telemetria, queda en cola local: ${error.message}")
            }
    }

    private fun applyReading(sample: MutableTelemetrySample, type: String, payload: JsonObject) {
        when (type) {
            "HEART_RATE" -> {
                sample.heartRateBpm = payload["bpm"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                sample.ibiMs = (payload["ibiMillis"] as? JsonArray)
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.toDoubleOrNull() }
                sample.heartRateQuality = qualityFrom(payload)
                sample.ibiQuality = sample.heartRateQuality
            }

            "ACCELEROMETER" -> {
                val magnitude = payload["magnitudeG"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                if (magnitude != null) {
                    sample.accelerometer = AccelerometerSampleDto(x = magnitude, y = 0.0, z = 0.0)
                }
            }

            "SKIN_TEMPERATURE" -> {
                sample.skinTemperatureCelsius = payload["celsius"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            }

            "STEPS" -> {
                // Pasos acumulados del dia -- se usa en el calculo de Delta antes de mandar el lote.
            }
        }
    }

    private fun qualityFrom(payload: JsonObject): String {
        val quality = payload["signalQuality"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: return "unknown"
        return when {
            quality >= 0.8 -> "good"
            quality >= 0.5 -> "fair"
            quality > 0 -> "poor"
            else -> "unknown"
        }
    }

    // ---- ACK hacia el reloj -------------------------------------------------------------

    private fun sendAck(nodeId: String, batchId: String? = null, eventId: String? = null) {
        val payload = buildString {
            append("{")
            var wroteField = false
            if (batchId != null) {
                append("\"batchId\":\"$batchId\"")
                wroteField = true
            }
            if (eventId != null) {
                if (wroteField) append(",")
                append("\"eventId\":\"$eventId\"")
            }
            append("}")
        }
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, ACK_ROUTE, payload.toByteArray())
            .addOnFailureListener { error -> Log.w(TAG, "No se pudo enviar el ACK al reloj: ${error.message}") }
    }

    private data class MutableTelemetrySample(
        var heartRateBpm: Double? = null,
        var ibiMs: List<Double>? = null,
        var accelerometer: AccelerometerSampleDto? = null,
        var skinTemperatureCelsius: Double? = null,
        var heartRateQuality: String = "unknown",
        var ibiQuality: String = "unknown",
    ) {
        fun toDto(timestampMillis: Long) = TelemetrySampleDto(
            timestamp = Instant.ofEpochMilli(timestampMillis).toString(),
            heartRateBpm = heartRateBpm,
            ibiMs = ibiMs,
            accelerometer = accelerometer,
            skinTemperatureCelsius = skinTemperatureCelsius,
            quality = SampleQualityDto(
                heartRate = heartRateQuality,
                ibi = ibiQuality,
                wearingState = "onBody",
            ),
        )
    }

    companion object {
        private const val TAG = "PhoneDataLayerBridge"
        private const val EVENT_ROUTE = "/anxietywatch/event"
        private const val TELEMETRY_ROUTE_PREFIX = "/anxietywatch/telemetry"
        private const val ACK_ROUTE = "/anxietywatch/ack"
    }
}

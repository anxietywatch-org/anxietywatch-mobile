package com.anxietywatch.mobile.data.bridge

import android.net.Uri
import android.util.Log
import com.anxietywatch.mobile.BuildConfig
import com.anxietywatch.mobile.data.local.AppDatabase
import com.anxietywatch.mobile.data.local.PendingEventDecisionEntity
import com.anxietywatch.mobile.data.local.PendingSosCancelEventEntity
import com.anxietywatch.mobile.data.local.PendingSosEventEntity
import com.anxietywatch.mobile.data.local.PendingSuspectedEventEntity
import com.anxietywatch.mobile.data.local.PendingTelemetryBatchEntity
import com.anxietywatch.mobile.data.local.SyncStatus
import com.anxietywatch.mobile.data.remote.AccelerometerSampleDto
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.CreateTelemetryBatchRequest
import com.anxietywatch.mobile.data.remote.EventDecisionRequest
import com.anxietywatch.mobile.data.remote.SampleQualityDto
import com.anxietywatch.mobile.data.remote.SosCancelRequest
import com.anxietywatch.mobile.data.remote.SuspectedEventBaselineRequest
import com.anxietywatch.mobile.data.remote.SuspectedEventFeaturesRequest
import com.anxietywatch.mobile.data.remote.SuspectedEventRequest
import com.anxietywatch.mobile.data.remote.TelemetrySampleDto
import com.anxietywatch.mobile.data.remote.TriggerSosRequest
import com.anxietywatch.mobile.data.remote.isWearableSubmissionDelivered
import com.anxietywatch.mobile.data.remote.responseIdMatches
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
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
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject

/** Fog bridge for the current `/fog/v1` Wear Data Layer protocol. */
@AndroidEntryPoint
class PhoneDataLayerListenerService : WearableListenerService() {

    @Inject lateinit var api: AnxietyWatchApi
    @Inject lateinit var sessionContext: MonitoringSessionContext
    @Inject lateinit var database: AppDatabase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val eventId = when {
            path.startsWith(SOS_CANCEL_ROUTE_PREFIX) -> path.removePrefix(SOS_CANCEL_ROUTE_PREFIX)
            path.startsWith(SOS_ROUTE_PREFIX) -> path.removePrefix(SOS_ROUTE_PREFIX)
            path.startsWith(SUSPECTED_ROUTE_PREFIX) -> path.removePrefix(SUSPECTED_ROUTE_PREFIX)
            path.startsWith(DECISION_ROUTE_PREFIX) -> path.removePrefix(DECISION_ROUTE_PREFIX)
            else -> return
        }.takeIf { it.isNotBlank() } ?: return

        val payload = String(messageEvent.data, Charsets.UTF_8)
        scope.launch {
            when {
                path.startsWith(SOS_CANCEL_ROUTE_PREFIX) -> handleSosCancel(payload, eventId, messageEvent.sourceNodeId)
                path.startsWith(SOS_ROUTE_PREFIX) -> handleSos(payload, eventId, messageEvent.sourceNodeId)
                path.startsWith(SUSPECTED_ROUTE_PREFIX) -> handleSuspected(payload, eventId, messageEvent.sourceNodeId)
                else -> handleDecision(payload, eventId, messageEvent.sourceNodeId)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            val path = item.uri.path.orEmpty()
            val dataMap = DataMapItem.fromDataItem(item).dataMap
            val payload = dataMap.getByteArray(PAYLOAD_KEY)?.toString(Charsets.UTF_8) ?: continue
            val sourceNodeId = item.uri.host ?: continue

            when {
                path.startsWith(TELEMETRY_ROUTE_PREFIX) -> {
                    val batchId = path.removePrefix("$TELEMETRY_ROUTE_PREFIX/").takeIf { it.isNotBlank() }
                        ?: continue
                    scope.launch { handleTelemetryBatch(payload, batchId, sourceNodeId) }
                }
                path == CAPABILITIES_ROUTE -> handleCapabilities(payload, sourceNodeId)
            }
        }
        dataEvents.release()
    }

    private suspend fun handleSos(rawJson: String, routeEventId: String, sourceNodeId: String) {
        val obj = parseEnvelope(rawJson, SOS_SCHEMA) ?: return
        val payloadEventId = obj.string("eventId") ?: return
        if (!responseIdMatches(routeEventId, payloadEventId)) {
            logRouteIdMismatch("SOS")
            return
        }
        val eventId = payloadEventId
        val request = TriggerSosRequest(
            eventId = eventId,
            deviceId = sessionContext.pairedDeviceId(),
            userId = null,
            triggeredAt = obj.string("triggeredAt") ?: return,
            source = obj.string("source") ?: "WATCH",
            reason = obj.string("reason") ?: "SOS desde el reloj",
        )
        database.pendingUploadDao().insertSosEvent(
            PendingSosEventEntity(eventId, json.encodeToString(request), System.currentTimeMillis()),
        )
        runCatching { api.triggerSos(request) }
            .onSuccess { response ->
                if (!isWearableSubmissionDelivered(request.eventId, response.eventId, response.accepted, response.duplicate)) {
                    Log.w(TAG, "Respuesta SOS no entregada; se conserva pendiente")
                    return@onSuccess
                }
                database.pendingUploadDao().updateSosEventStatus(eventId, SyncStatus.SYNCED)
                sendEventAck(sourceNodeId, "$ACK_SOS_PREFIX$eventId")
            }
            .onFailure { Log.w(TAG, "No se pudo sincronizar el evento SOS") }
    }

    private suspend fun handleSosCancel(rawJson: String, routeEventId: String, sourceNodeId: String) {
        val obj = parseEnvelope(rawJson, SOS_SCHEMA) ?: return
        val payloadEventId = obj.string("eventId") ?: return
        if (!responseIdMatches(routeEventId, payloadEventId)) {
            logRouteIdMismatch("SOS_CANCEL")
            return
        }
        val eventId = payloadEventId
        val request = SosCancelRequest(
            eventId = eventId,
            deviceId = sessionContext.pairedDeviceId(),
            userId = null,
            cancelledAt = obj.string("cancelledAt") ?: return,
            reason = obj.string("reason"),
        )
        database.pendingUploadDao().insertSosCancelEvent(
            PendingSosCancelEventEntity(eventId, json.encodeToString(request), System.currentTimeMillis()),
        )
        runCatching { api.cancelSos(request) }
            .onSuccess { response ->
                if (!isWearableSubmissionDelivered(request.eventId, response.eventId, response.accepted, response.duplicate)) {
                    Log.w(TAG, "Respuesta de cancelación no entregada; se conserva pendiente")
                    return@onSuccess
                }
                database.pendingUploadDao().updateSosCancelEventStatus(eventId, SyncStatus.SYNCED)
                sendEventAck(sourceNodeId, "$ACK_SOS_CANCEL_PREFIX$eventId")
            }
            .onFailure { Log.w(TAG, "No se pudo sincronizar la cancelación SOS") }
    }

    private suspend fun handleSuspected(rawJson: String, routeEventId: String, sourceNodeId: String) {
        val obj = parseEnvelope(rawJson, SUSPECTED_SCHEMA) ?: return
        val payloadEventId = obj.string("eventId") ?: return
        if (!responseIdMatches(routeEventId, payloadEventId)) {
            logRouteIdMismatch("SUSPECTED")
            return
        }
        val eventId = payloadEventId
        val features = obj["features"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: return
        val baseline = obj["baseline"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: return
        val request = SuspectedEventRequest(
            eventId = eventId,
            deviceId = sessionContext.pairedDeviceId(),
            userId = null,
            sessionId = sessionContext.currentSessionId(),
            sequence = sessionContext.nextSequence(),
            detectedAt = obj.string("detectedAt") ?: return,
            state = obj.string("state") ?: return,
            score = obj.number("score") ?: return,
            rulesVersion = obj.string("rulesVersion") ?: return,
            features = SuspectedEventFeaturesRequest(
                heartRateMean = features.number("heartRateMean"),
                heartRateMax = features.number("heartRateMax"),
                heartRateSlopeBpmPerMinute = features.number("heartRateSlopeBpmPerMinute"),
                heartRateDeltaFromBaseline = features.number("heartRateDeltaFromBaseline"),
                rmssdMillis = features.number("rmssdMillis"),
                sdnnMillis = features.number("sdnnMillis"),
                movementMagnitudeMean = features.number("movementMagnitudeMean"),
                movementVariance = features.number("movementVariance"),
                validSampleRatio = features.number("validSampleRatio") ?: return,
                lastSampleAgeSeconds = features.long("lastSampleAgeSeconds") ?: return,
                sampleCount = features.long("sampleCount")?.toInt() ?: return,
            ),
            baseline = SuspectedEventBaselineRequest(
                sampleCount = baseline.long("sampleCount") ?: return,
                meanHeartRate = baseline.number("meanHeartRate") ?: return,
                heartRateM2 = baseline.number("heartRateM2") ?: return,
                updatedAtEpochMillis = baseline.long("updatedAtEpochMillis") ?: return,
            ),
        )
        database.pendingUploadDao().insertSuspectedEvent(
            PendingSuspectedEventEntity(eventId, json.encodeToString(request), System.currentTimeMillis()),
        )
        runCatching { api.submitSuspectedEvent(request) }
            .onSuccess { response ->
                if (!isWearableSubmissionDelivered(request.eventId, response.eventId, response.accepted, response.duplicate)) {
                    Log.w(TAG, "Respuesta de evento no entregada; se conserva pendiente")
                    return@onSuccess
                }
                database.pendingUploadDao().updateSuspectedEventStatus(eventId, SyncStatus.SYNCED)
                sendEventAck(sourceNodeId, "$ACK_SUSPECTED_PREFIX$eventId")
            }
            .onFailure { Log.w(TAG, "No se pudo sincronizar el evento detectado") }
    }

    private suspend fun handleDecision(rawJson: String, routeEventId: String, sourceNodeId: String) {
        val obj = parseEnvelope(rawJson, DECISION_SCHEMA) ?: return
        val payloadEventId = obj.string("eventId") ?: return
        if (!responseIdMatches(routeEventId, payloadEventId)) {
            logRouteIdMismatch("DECISION")
            return
        }
        val eventId = payloadEventId
        val request = EventDecisionRequest(
            eventId = eventId,
            deviceId = sessionContext.pairedDeviceId(),
            userId = null,
            sessionId = sessionContext.currentSessionId(),
            sequence = sessionContext.nextSequence(),
            detectedAt = obj.string("detectedAt") ?: return,
            respondedAt = obj.string("respondedAt") ?: return,
            response = obj.string("response") ?: return,
        )
        database.pendingUploadDao().insertEventDecision(
            PendingEventDecisionEntity(eventId, json.encodeToString(request), System.currentTimeMillis()),
        )
        runCatching { api.submitEventDecision(request) }
            .onSuccess { response ->
                if (!isWearableSubmissionDelivered(request.eventId, response.eventId, response.accepted, response.duplicate)) {
                    Log.w(TAG, "Respuesta de decisión no entregada; se conserva pendiente")
                    return@onSuccess
                }
                database.pendingUploadDao().updateEventDecisionStatus(eventId, SyncStatus.SYNCED)
                sendEventAck(sourceNodeId, "$ACK_DECISION_PREFIX$eventId")
            }
            .onFailure { Log.w(TAG, "No se pudo sincronizar la decisión del evento") }
    }

    private suspend fun handleTelemetryBatch(rawJson: String, routeBatchId: String, sourceNodeId: String) {
        val obj = parseEnvelope(rawJson, TELEMETRY_SCHEMA) ?: return
        val payloadBatchId = obj.string("batchId") ?: return
        if (!responseIdMatches(routeBatchId, payloadBatchId)) {
            logRouteIdMismatch("TELEMETRY")
            return
        }
        val batchId = payloadBatchId
        val records = obj["records"]?.jsonArray ?: JsonArray(emptyList())
        val samplesByTimestamp = linkedMapOf<Long, MutableTelemetrySample>()

        for (recordElement in records) {
            val record = recordElement.jsonObject
            val capturedAt = try {
                Instant.parse(record.string("capturedAt") ?: continue).toEpochMilli()
            } catch (_: DateTimeParseException) {
                Log.w(TAG, "Se descartó una muestra con timestamp inválido")
                continue
            }
            val type = record.string("type") ?: continue
            val payload = record["payload"] as? JsonObject ?: continue
            val sample = samplesByTimestamp.getOrPut(capturedAt) { MutableTelemetrySample() }
            applyReading(sample, type, payload)
        }

        if (samplesByTimestamp.isEmpty()) {
            Log.w(TAG, "Se descartó un lote de telemetría vacío sin enviar ACK")
            return
        }

        val timestamps = samplesByTimestamp.keys.sorted()
        val request = CreateTelemetryBatchRequest(
            batchId = batchId,
            deviceId = sessionContext.pairedDeviceId(),
            userId = null,
            sessionId = sessionContext.currentSessionId(),
            startedAt = Instant.ofEpochMilli(timestamps.first()).toString(),
            endedAt = Instant.ofEpochMilli(timestamps.last()).toString(),
            sequence = sessionContext.nextSequence(),
            samples = timestamps.map { samplesByTimestamp.getValue(it).toDto(it) },
        )
        database.pendingUploadDao().insertTelemetryBatch(
            PendingTelemetryBatchEntity(batchId, json.encodeToString(request), System.currentTimeMillis()),
        )
        runCatching { api.sendTelemetryBatch(request) }
            .onSuccess { response ->
                if (!isWearableSubmissionDelivered(request.batchId, response.batchId, response.accepted, response.duplicate)) {
                    Log.w(TAG, "Respuesta de telemetría no entregada; se conserva pendiente")
                    return@onSuccess
                }
                database.pendingUploadDao().updateTelemetryBatchStatus(batchId, SyncStatus.SYNCED)
                sendTelemetryAck(sourceNodeId, batchId)
            }
            .onFailure { Log.w(TAG, "No se pudo sincronizar la telemetría") }
    }

    private fun handleCapabilities(rawJson: String, sourceNodeId: String) {
        val obj = runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull() ?: return
        if (obj.string("fogProtocol") == FOG_PROTOCOL) {
            Log.i(TAG, "Reloj compatible detectado")
        } else {
            Log.w(TAG, "Capabilities de reloj no compatibles")
        }
    }

    private fun logRouteIdMismatch(kind: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Rejected $kind envelope: route/payload ID mismatch")
        }
    }

    private fun parseEnvelope(rawJson: String, schema: String): JsonObject? {
        val obj = runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull() ?: return null
        if (obj.string("schemaVersion") != schema) {
            Log.w(TAG, "Schema de mensaje inesperado")
            return null
        }
        return obj
    }

    private fun sendTelemetryAck(nodeId: String, batchId: String) {
        sendEventAck(nodeId, "$ACK_TELEMETRY_PREFIX$batchId")
        Wearable.getDataClient(this).deleteDataItems(
            Uri.parse("wear://*$TELEMETRY_ROUTE_PREFIX/$batchId"),
            DataClient.FILTER_LITERAL,
        ).addOnFailureListener { Log.w(TAG, "No se pudo borrar el DataItem") }
    }

    private fun sendEventAck(nodeId: String, path: String) {
        Wearable.getMessageClient(this).sendMessage(nodeId, path, ByteArray(0))
            .addOnFailureListener { Log.w(TAG, "No se pudo enviar el ACK") }
    }

    private fun applyReading(sample: MutableTelemetrySample, type: String, payload: JsonObject) {
        when (type) {
            "HEART_RATE" -> {
                sample.heartRateBpm = payload.number("bpm")
                sample.ibiMs = (payload["ibiMillis"] as? JsonArray)
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.toDoubleOrNull() }
                sample.heartRateQuality = qualityFrom(payload)
                sample.ibiQuality = sample.heartRateQuality
            }
            "ACCELEROMETER" -> payload.number("magnitudeG")?.let {
                sample.accelerometer = AccelerometerSampleDto(x = it, y = 0.0, z = 0.0)
            }
            "SKIN_TEMPERATURE" -> sample.skinTemperatureCelsius = payload.number("celsius")
        }
    }

    private fun qualityFrom(payload: JsonObject): String = when (val quality = payload.number("signalQuality")) {
        null -> "unknown"
        in 0.8..Double.MAX_VALUE -> "good"
        in 0.5..0.799999999 -> "fair"
        in 0.000000001..0.499999999 -> "poor"
        else -> "unknown"
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
            quality = SampleQualityDto(heartRateQuality, ibiQuality, "onBody"),
        )
    }

    private companion object {
        const val TAG = "PhoneDataLayerBridge"
        const val PAYLOAD_KEY = "payload"
        const val FOG_PROTOCOL = "fog_watch_v1"
        const val TELEMETRY_SCHEMA = "wear-telemetry-records-v2"
        const val SOS_SCHEMA = "wear-sos-trigger-v1"
        const val SUSPECTED_SCHEMA = "wear-suspected-event-v1"
        const val DECISION_SCHEMA = "wear-event-decision-v1"
        const val TELEMETRY_ROUTE_PREFIX = "/fog/v1/telemetry"
        const val SOS_ROUTE_PREFIX = "/fog/v1/sos/"
        const val SOS_CANCEL_ROUTE_PREFIX = "/fog/v1/sos/cancel/"
        const val SUSPECTED_ROUTE_PREFIX = "/fog/v1/events/suspected/"
        const val DECISION_ROUTE_PREFIX = "/fog/v1/events/decision/"
        const val CAPABILITIES_ROUTE = "/fog/v1/capabilities"
        const val ACK_TELEMETRY_PREFIX = "/fog/v1/ack/telemetry/"
        const val ACK_SOS_PREFIX = "/fog/v1/ack/sos/"
        const val ACK_SOS_CANCEL_PREFIX = "/fog/v1/ack/sos-cancel/"
        const val ACK_SUSPECTED_PREFIX = "/fog/v1/ack/events/suspected/"
        const val ACK_DECISION_PREFIX = "/fog/v1/ack/events/decision/"
    }
}

private fun JsonObject.string(key: String): String? =
    this[key]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }

private fun JsonObject.number(key: String): Double? =
    this[key]?.let { runCatching { it.jsonPrimitive.doubleOrNull }.getOrNull() }
private fun JsonObject.long(key: String): Long? = string(key)?.toLongOrNull()

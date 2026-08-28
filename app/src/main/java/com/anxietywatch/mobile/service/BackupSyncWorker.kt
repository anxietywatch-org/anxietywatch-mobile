package com.anxietywatch.mobile.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.anxietywatch.mobile.data.bridge.DeliveryCoordinator
import com.anxietywatch.mobile.data.bridge.BackendDeliveryResponse
import com.anxietywatch.mobile.data.bridge.DeliveryReason
import com.anxietywatch.mobile.data.bridge.MonitoringSessionContext
import com.anxietywatch.mobile.data.bridge.TERMINAL_NACK_TELEMETRY_PREFIX
import com.anxietywatch.mobile.data.bridge.terminalTelemetryNackPayload
import com.anxietywatch.mobile.data.local.AppDatabase
import com.anxietywatch.mobile.data.local.SyncStatus
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.CreateTelemetryBatchRequest
import com.anxietywatch.mobile.data.remote.EventDecisionRequest
import com.anxietywatch.mobile.data.remote.SessionRepository
import com.anxietywatch.mobile.data.remote.SosCancelRequest
import com.anxietywatch.mobile.data.remote.SuspectedEventRequest
import com.anxietywatch.mobile.data.remote.TriggerSosRequest
import com.anxietywatch.mobile.data.remote.isValidWearableDeviceId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@HiltWorker
class BackupSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sessionRepository: SessionRepository,
    private val database: AppDatabase,
    private val api: AnxietyWatchApi,
    private val deliveryCoordinator: DeliveryCoordinator,
    private val sessionContext: MonitoringSessionContext,
) : CoroutineWorker(context, params) {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result = BackupSyncCoordinator.mutex.withLock {
        doWorkExclusive()
    }

    private suspend fun doWorkExclusive(): Result {
        if (!sessionRepository.hasValidSession()) return Result.success()
        val dao = database.pendingUploadDao()

        dao.getSosEventsByStatus(SyncStatus.PENDING_HTTP).forEach { pending ->
            val request = decodeOrTerminal(pending.requestJson, TriggerSosRequest.serializer()) {
                dao.updateSosEventStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.INVALID_PAYLOAD)
            } ?: return@forEach
            val wearableId = ownership(request.deviceId, pending.wearableDeviceId)
            if (!prepareOwnership(pending.wearableDeviceId, wearableId) { dao.setSosOwnership(pending.eventId, wearableId) } || !isValidWearableDeviceId(wearableId)) {
                dao.updateSosEventStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.LEGACY_MISSING_WEARABLE_IDENTITY)
                return@forEach
            }
            deliveryCoordinator.deliver(
                pending.syncStatus, pending.attemptCount, pending.lastError, wearableId, pending.eventId,
                "/fog/v1/ack/sos/${pending.eventId}",
                { api.triggerSos(request).let { BackendDeliveryResponse(it.eventId, it.accepted, it.duplicate) } },
                { status, reason -> dao.updateSosEventStatus(pending.eventId, status, reason) },
                { reason -> dao.incrementSosAttempt(pending.eventId, reason) },
            )
        }

        dao.getSosEventsByStatus(SyncStatus.BACKEND_DELIVERED_ACK_PENDING).forEach { pending ->
            val request = decodeOrTerminal(pending.requestJson, TriggerSosRequest.serializer()) {
                dao.updateSosEventStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.INVALID_PAYLOAD)
            } ?: return@forEach
            processAckOnly(pending.wearableDeviceId.ifBlank { request.deviceId }, pending.eventId, pending.syncStatus,
                pending.attemptCount, pending.lastError, "/fog/v1/ack/sos/${pending.eventId}",
                { status, reason -> dao.updateSosEventStatus(pending.eventId, status, reason) },
                { reason -> dao.incrementSosAttempt(pending.eventId, reason) })
        }

        dao.getSosCancelEventsByStatus(SyncStatus.PENDING_HTTP).forEach { pending ->
            val request = decodeOrTerminal(pending.requestJson, SosCancelRequest.serializer()) {
                dao.updateSosCancelEventStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.INVALID_PAYLOAD)
            } ?: return@forEach
            val wearableId = ownership(request.deviceId, pending.wearableDeviceId)
            if (!prepareOwnership(pending.wearableDeviceId, wearableId) { dao.setSosCancelOwnership(pending.eventId, wearableId) } || !isValidWearableDeviceId(wearableId)) {
                dao.updateSosCancelEventStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.LEGACY_MISSING_WEARABLE_IDENTITY)
                return@forEach
            }
            deliveryCoordinator.deliver(
                pending.syncStatus, pending.attemptCount, pending.lastError, wearableId, pending.eventId,
                "/fog/v1/ack/sos-cancel/${pending.eventId}",
                { api.cancelSos(request).let { BackendDeliveryResponse(it.eventId, it.accepted, it.duplicate) } },
                { status, reason -> dao.updateSosCancelEventStatus(pending.eventId, status, reason) },
                { reason -> dao.incrementSosCancelAttempt(pending.eventId, reason) },
            )
        }
        dao.getSosCancelEventsByStatus(SyncStatus.BACKEND_DELIVERED_ACK_PENDING).forEach { pending ->
            val request = decodeOrTerminal(pending.requestJson, SosCancelRequest.serializer()) {
                dao.updateSosCancelEventStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.INVALID_PAYLOAD)
            } ?: return@forEach
            processAckOnly(pending.wearableDeviceId.ifBlank { request.deviceId }, pending.eventId, pending.syncStatus,
                pending.attemptCount, pending.lastError, "/fog/v1/ack/sos-cancel/${pending.eventId}",
                { status, reason -> dao.updateSosCancelEventStatus(pending.eventId, status, reason) },
                { reason -> dao.incrementSosCancelAttempt(pending.eventId, reason) })
        }

        dao.getTelemetryBatchesByStatus(SyncStatus.PENDING_HTTP).forEach { pending ->
            val request = decodeOrTerminal(pending.requestJson, CreateTelemetryBatchRequest.serializer()) {
                dao.updateTelemetryBatchStatus(pending.batchId, SyncStatus.TERMINAL_FAILED, DeliveryReason.INVALID_PAYLOAD)
            } ?: return@forEach
            val wearableId = ownership(request.deviceId, pending.wearableDeviceId)
            if (!prepareOwnership(pending.wearableDeviceId, wearableId) { dao.setTelemetryOwnership(pending.batchId, wearableId) } || !isValidWearableDeviceId(wearableId)) {
                dao.updateTelemetryBatchStatus(pending.batchId, SyncStatus.TERMINAL_FAILED, DeliveryReason.LEGACY_MISSING_WEARABLE_IDENTITY)
                return@forEach
            }
            deliveryCoordinator.deliver(
                pending.syncStatus, pending.attemptCount, pending.lastError, wearableId, pending.batchId,
                "/fog/v1/ack/telemetry/${pending.batchId}",
                { api.sendTelemetryBatch(request).let { BackendDeliveryResponse(it.batchId, it.accepted, it.duplicate) } },
                { status, reason -> dao.updateTelemetryBatchStatus(pending.batchId, status, reason) },
                { reason -> dao.incrementTelemetryAttempt(pending.batchId, reason) },
                terminalNackPath = "$TERMINAL_NACK_TELEMETRY_PREFIX${pending.batchId}",
                terminalNackPayload = terminalTelemetryNackPayload(pending.batchId),
            )
        }
        dao.getTelemetryBatchesByStatus(SyncStatus.BACKEND_DELIVERED_ACK_PENDING).forEach { pending ->
            val request = decodeOrTerminal(pending.requestJson, CreateTelemetryBatchRequest.serializer()) {
                dao.updateTelemetryBatchStatus(pending.batchId, SyncStatus.TERMINAL_FAILED, DeliveryReason.INVALID_PAYLOAD)
            } ?: return@forEach
            processAckOnly(pending.wearableDeviceId.ifBlank { request.deviceId }, pending.batchId, pending.syncStatus,
                pending.attemptCount, pending.lastError, "/fog/v1/ack/telemetry/${pending.batchId}",
                { status, reason ->
                    dao.updateTelemetryBatchStatus(pending.batchId, status, reason)
                },
                { reason -> dao.incrementTelemetryAttempt(pending.batchId, reason) })
        }

        processSuspected(dao)
        processDecisions(dao)
        return Result.success()
    }

    private suspend fun processSuspected(dao: com.anxietywatch.mobile.data.local.PendingUploadDao) {
        dao.getSuspectedEventsByStatus(SyncStatus.PENDING_HTTP).forEach { pending ->
            val request = decodeOrTerminal(pending.requestJson, SuspectedEventRequest.serializer()) {
                dao.updateSuspectedEventStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.INVALID_PAYLOAD)
            } ?: return@forEach
            val wearableId = ownership(request.deviceId, pending.wearableDeviceId)
            if (!prepareOwnership(pending.wearableDeviceId, wearableId) { dao.setSuspectedOwnership(pending.eventId, wearableId) } || !isValidWearableDeviceId(wearableId)) {
                dao.updateSuspectedEventStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.LEGACY_MISSING_WEARABLE_IDENTITY)
                return@forEach
            }
            deliveryCoordinator.deliver(pending.syncStatus, pending.attemptCount, pending.lastError, wearableId, pending.eventId,
                "/fog/v1/ack/events/suspected/${pending.eventId}",
                { api.submitSuspectedEvent(request).let { BackendDeliveryResponse(it.eventId, it.accepted, it.duplicate) } },
                { status, reason -> dao.updateSuspectedEventStatus(pending.eventId, status, reason) },
                { reason -> dao.incrementSuspectedAttempt(pending.eventId, reason) })
        }
        dao.getSuspectedEventsByStatus(SyncStatus.BACKEND_DELIVERED_ACK_PENDING).forEach { pending ->
            val request = decodeOrTerminal(pending.requestJson, SuspectedEventRequest.serializer()) {
                dao.updateSuspectedEventStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.INVALID_PAYLOAD)
            } ?: return@forEach
            processAckOnly(pending.wearableDeviceId.ifBlank { request.deviceId }, pending.eventId, pending.syncStatus, pending.attemptCount,
                pending.lastError, "/fog/v1/ack/events/suspected/${pending.eventId}",
                { status, reason -> dao.updateSuspectedEventStatus(pending.eventId, status, reason) },
                { reason -> dao.incrementSuspectedAttempt(pending.eventId, reason) })
        }
    }

    private suspend fun processDecisions(dao: com.anxietywatch.mobile.data.local.PendingUploadDao) {
        dao.getEventDecisionsByStatus(SyncStatus.PENDING_HTTP).forEach { pending ->
            val request = decodeOrTerminal(pending.requestJson, EventDecisionRequest.serializer()) {
                dao.updateEventDecisionStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.INVALID_PAYLOAD)
            } ?: return@forEach
            val wearableId = ownership(request.deviceId, pending.wearableDeviceId)
            if (!prepareOwnership(pending.wearableDeviceId, wearableId) { dao.setDecisionOwnership(pending.eventId, wearableId) } || !isValidWearableDeviceId(wearableId)) {
                dao.updateEventDecisionStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.LEGACY_MISSING_WEARABLE_IDENTITY)
                return@forEach
            }
            deliveryCoordinator.deliver(pending.syncStatus, pending.attemptCount, pending.lastError, wearableId, pending.eventId,
                "/fog/v1/ack/events/decision/${pending.eventId}",
                { api.submitEventDecision(request).let { BackendDeliveryResponse(it.eventId, it.accepted, it.duplicate) } },
                { status, reason -> dao.updateEventDecisionStatus(pending.eventId, status, reason) },
                { reason -> dao.incrementEventDecisionAttempt(pending.eventId, reason) })
        }
        dao.getEventDecisionsByStatus(SyncStatus.BACKEND_DELIVERED_ACK_PENDING).forEach { pending ->
            val request = decodeOrTerminal(pending.requestJson, EventDecisionRequest.serializer()) {
                dao.updateEventDecisionStatus(pending.eventId, SyncStatus.TERMINAL_FAILED, DeliveryReason.INVALID_PAYLOAD)
            } ?: return@forEach
            processAckOnly(pending.wearableDeviceId.ifBlank { request.deviceId }, pending.eventId, pending.syncStatus, pending.attemptCount,
                pending.lastError, "/fog/v1/ack/events/decision/${pending.eventId}",
                { status, reason -> dao.updateEventDecisionStatus(pending.eventId, status, reason) },
                { reason -> dao.incrementEventDecisionAttempt(pending.eventId, reason) })
        }
    }

    private suspend fun processAckOnly(
        wearableId: String, id: String, status: String, attempts: Int, lastError: String?, ackPath: String,
        persist: suspend (String, String?) -> Unit, increment: suspend (String?) -> Unit,
    ) = deliveryCoordinator.deliver(status, attempts, lastError, wearableId, id, ackPath,
        http = { error("HTTP must not run for ACK_PENDING") }, persist = persist, incrementAttempt = increment)

    private suspend fun prepareOwnership(
        existing: String,
        resolved: String,
        persist: suspend () -> Unit,
    ): Boolean {
        if (existing.isNotBlank()) return isValidWearableDeviceId(existing)
        if (!isValidWearableDeviceId(resolved)) return false
        persist()
        return true
    }

    private fun ownership(requestDeviceId: String, storedDeviceId: String): String =
        storedDeviceId.ifBlank { requestDeviceId }

    private suspend fun <T> decodeOrTerminal(
        raw: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        onFailure: suspend () -> Unit,
    ): T? = runCatching { json.decodeFromString(serializer, raw) }.getOrElse {
        onFailure()
        null
    }

    companion object {
        const val UNIQUE_WORK_NAME = "backup_sync_worker"
        private const val SEVEN_DAYS_MILLIS = 7 * 24 * 60 * 60 * 1000L

        fun constraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}

private object BackupSyncCoordinator {
    val mutex = Mutex()
}

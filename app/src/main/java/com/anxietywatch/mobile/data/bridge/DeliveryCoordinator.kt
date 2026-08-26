package com.anxietywatch.mobile.data.bridge

import com.anxietywatch.mobile.data.local.SyncStatus
import com.anxietywatch.mobile.data.remote.isValidWearableDeviceId
import javax.inject.Inject

interface CurrentWearablePairing {
    fun pairedDeviceId(): String?
    fun lastKnownWearNodeId(): String?
}

class DeliveryCoordinator @Inject constructor(
    private val pairing: CurrentWearablePairing,
    private val ackSender: WearAckSender,
) {
    suspend fun deliver(
        status: String,
        attemptCount: Int,
        lastError: String?,
        wearableDeviceId: String,
        expectedId: String,
        ackPath: String,
        http: suspend () -> BackendDeliveryResponse,
        persist: suspend (status: String, reason: String?) -> Unit,
        incrementAttempt: suspend (reason: String?) -> Unit,
    ) {
        if (!isCurrentlyPaired(wearableDeviceId)) return

        if (status == SyncStatus.BACKEND_DELIVERED_ACK_PENDING) {
            sendAckIfPossible(wearableDeviceId, ackPath, persist)
            return
        }
        if (status != SyncStatus.PENDING_HTTP || lastError == DeliveryReason.WAIT_FOR_AUTH) return

        val response = runCatching { http() }.getOrElse { error ->
            when (DeliveryPolicy.classifyFailure(error)) {
                RetryClass.TRANSIENT -> {
                    if (DeliveryPolicy.shouldTerminalize(attemptCount + 1)) {
                        persist(SyncStatus.TERMINAL_FAILED, DeliveryReason.RETRY_EXHAUSTED)
                    } else {
                        incrementAttempt(DeliveryReason.NETWORK_FAILURE)
                    }
                }
                RetryClass.WAIT_FOR_AUTH -> persist(SyncStatus.PENDING_HTTP, DeliveryReason.WAIT_FOR_AUTH)
                RetryClass.TERMINAL -> persist(SyncStatus.TERMINAL_FAILED, DeliveryReason.HTTP_PERMANENT)
            }
            return
        }

        if (!DeliveryPolicy.backendDelivered(expectedId, response)) {
            persist(SyncStatus.TERMINAL_FAILED, DeliveryReason.RESPONSE_MISMATCH_OR_NOT_DELIVERED)
            return
        }

        // Durable ordering: mark backend success before attempting the ACK.
        persist(SyncStatus.BACKEND_DELIVERED_ACK_PENDING, DeliveryReason.ACK_PENDING)
        sendAckIfPossible(wearableDeviceId, ackPath, persist)
    }

    private suspend fun sendAckIfPossible(
        wearableDeviceId: String,
        ackPath: String,
        persist: suspend (status: String, reason: String?) -> Unit,
    ) {
        val nodeId = pairing.lastKnownWearNodeId() ?: return
        if (!isCurrentlyPaired(wearableDeviceId)) return
        if (ackSender.sendAck(nodeId, ackPath)) {
            persist(SyncStatus.DELIVERED, null)
        }
    }

    private fun isCurrentlyPaired(wearableDeviceId: String): Boolean =
        isValidWearableDeviceId(wearableDeviceId) && pairing.pairedDeviceId() == wearableDeviceId
}

object DeliveryReason {
    const val ACK_PENDING = "ACK_PENDING"
    const val NETWORK_FAILURE = "NETWORK_FAILURE"
    const val WAIT_FOR_AUTH = "WAIT_FOR_AUTH"
    const val HTTP_PERMANENT = "HTTP_PERMANENT"
    const val RESPONSE_MISMATCH_OR_NOT_DELIVERED = "RESPONSE_MISMATCH_OR_NOT_DELIVERED"
    const val RETRY_EXHAUSTED = "RETRY_EXHAUSTED"
    const val LEGACY_MISSING_WEARABLE_IDENTITY = "LEGACY_MISSING_WEARABLE_IDENTITY"
    const val INVALID_PAYLOAD = "INVALID_PAYLOAD"
}

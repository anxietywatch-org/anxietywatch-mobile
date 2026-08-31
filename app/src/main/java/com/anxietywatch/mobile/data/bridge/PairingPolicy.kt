package com.anxietywatch.mobile.data.bridge

import com.anxietywatch.mobile.data.remote.isValidWearableDeviceId
import java.util.UUID

data class PendingPairingSnapshot(
    val nonce: String?,
    val nodeId: String?,
    val startedAtMillis: Long?,
)

object PairingPolicy {
    const val TIMEOUT_MILLIS = 5 * 60 * 1000L

    fun isValidUuid(value: String?): Boolean = value
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?.let { it != UUID(0L, 0L) }
        ?: false

    fun isFresh(startedAtMillis: Long?, nowMillis: Long): Boolean =
        startedAtMillis != null &&
            nowMillis >= startedAtMillis &&
            nowMillis - startedAtMillis <= TIMEOUT_MILLIS

    fun acceptsIdentity(
        pending: PendingPairingSnapshot,
        sourceNodeId: String,
        nonce: String,
        wearableDeviceId: String,
        nowMillis: Long,
    ): Boolean =
        isValidUuid(nonce) &&
            isValidWearableDeviceId(wearableDeviceId) &&
            pending.nonce == nonce &&
            pending.nodeId == sourceNodeId &&
            isFresh(pending.startedAtMillis, nowMillis)
}

package com.anxietywatch.mobile.data.bridge

import android.content.Context
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface WearAckSender {
    suspend fun sendAck(nodeId: String, path: String): Boolean

    suspend fun sendTerminalNack(nodeId: String, path: String, payload: ByteArray): Boolean
}

class DataLayerWearAckSender @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : WearAckSender {
    override suspend fun sendAck(nodeId: String, path: String): Boolean = suspendCoroutine { continuation ->
        Wearable.getMessageClient(context)
            .sendMessage(nodeId, path, ByteArray(0))
            .addOnCompleteListener { task -> continuation.resume(task.isSuccessful) }
    }

    override suspend fun sendTerminalNack(nodeId: String, path: String, payload: ByteArray): Boolean =
        suspendCoroutine { continuation ->
            Wearable.getMessageClient(context)
                .sendMessage(nodeId, path, payload)
                .addOnCompleteListener { task -> continuation.resume(task.isSuccessful) }
        }
}

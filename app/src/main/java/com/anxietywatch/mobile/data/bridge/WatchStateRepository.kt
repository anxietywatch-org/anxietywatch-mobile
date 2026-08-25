package com.anxietywatch.mobile.data.bridge

import android.content.Context
import com.anxietywatch.mobile.data.local.AppDatabase
import com.anxietywatch.mobile.data.remote.CreateTelemetryBatchRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class LatestWatchSample(
    val heartRateBpm: Int?,
    val capturedAt: String?,
    val batchCreatedAtMillis: Long,
)

data class WatchState(
    val latestSample: LatestWatchSample? = null,
    val lastTelemetryAtMillis: Long? = null,
    val connected: Boolean = false,
    val nodeName: String? = null,
)

@Singleton
class WatchStateRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    database: AppDatabase,
) {
    private val dao = database.pendingUploadDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(WatchState())
    val state: StateFlow<WatchState> = _state.asStateFlow()

    init {
        scope.launch {
            dao.observeMostRecentTelemetryBatch().collect { batch ->
                _state.emit(
                    _state.value.copy(
                        latestSample = batch?.let(::parseLatestSample),
                        lastTelemetryAtMillis = batch?.createdAtMillis,
                    ),
                )
            }
        }
        refresh()
    }

    fun refreshConnection() {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                val node = nodes.firstOrNull()
                _state.update { it.copy(connected = node != null, nodeName = node?.displayName) }
            }
            .addOnFailureListener {
                _state.update { it.copy(connected = false, nodeName = null) }
            }
    }

    fun refresh() {
        refreshConnection()
        scope.launch {
            val batch = dao.observeMostRecentTelemetryBatch().first()
            _state.update {
                it.copy(
                    latestSample = batch?.let(::parseLatestSample),
                    lastTelemetryAtMillis = batch?.createdAtMillis,
                )
            }
        }
    }

    private fun parseLatestSample(batch: com.anxietywatch.mobile.data.local.PendingTelemetryBatchEntity): LatestWatchSample? {
        val request = runCatching {
            json.decodeFromString<CreateTelemetryBatchRequest>(batch.requestJson)
        }.getOrNull() ?: return null

        val sample = request.samples
            .mapNotNull { telemetry ->
                runCatching { Instant.parse(telemetry.timestamp) to telemetry }.getOrNull()
            }
            .filter { it.second.heartRateBpm != null }
            .maxByOrNull { it.first }
            ?.second
            ?: return null

        return LatestWatchSample(
            heartRateBpm = sample.heartRateBpm?.toInt(),
            capturedAt = sample.timestamp,
            batchCreatedAtMillis = batch.createdAtMillis,
        )
    }
}

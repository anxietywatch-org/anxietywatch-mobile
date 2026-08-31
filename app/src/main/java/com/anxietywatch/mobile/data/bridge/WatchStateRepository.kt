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

private const val HR_BATCH_PAGE_SIZE = 50

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
            dao.observeMostRecentTelemetryBatch().collect { latestBatch ->
                _state.emit(
                    _state.value.copy(
                        latestSample = findLatestPersistedHeartRateSample(),
                        lastTelemetryAtMillis = latestBatch?.createdAtMillis,
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
            val latestBatch = dao.observeMostRecentTelemetryBatch().first()
            _state.update {
                it.copy(
                    latestSample = findLatestPersistedHeartRateSample(),
                    lastTelemetryAtMillis = latestBatch?.createdAtMillis,
                )
            }
        }
    }

    private suspend fun findLatestPersistedHeartRateSample(): LatestWatchSample? {
        var offset = 0
        var latest: LatestWatchSample? = null

        while (true) {
            val page = dao.getTelemetryBatchPage(HR_BATCH_PAGE_SIZE, offset)
            if (page.isEmpty()) return latest

            val pageLatest = selectLatestHeartRateSample(page, json)
            if (pageLatest != null && (latest == null || isLater(pageLatest, latest))) {
                latest = pageLatest
            }
            offset += page.size
        }
    }

}

internal fun selectLatestHeartRateSample(
    batches: List<com.anxietywatch.mobile.data.local.PendingTelemetryBatchEntity>,
    json: Json,
): LatestWatchSample? = batches
    .mapNotNull { batch ->
        val request = runCatching {
            json.decodeFromString<CreateTelemetryBatchRequest>(batch.requestJson)
        }.getOrNull() ?: return@mapNotNull null

        request.samples
            .asSequence()
            .mapNotNull { telemetry ->
                val timestamp = runCatching { Instant.parse(telemetry.timestamp) }.getOrNull()
                if (timestamp != null && telemetry.heartRateBpm != null) {
                    timestamp to LatestWatchSample(
                        heartRateBpm = telemetry.heartRateBpm.toInt(),
                        capturedAt = telemetry.timestamp,
                        batchCreatedAtMillis = batch.createdAtMillis,
                    )
                } else {
                    null
                }
            }
            .maxByOrNull { it.first }
            ?.second
    }
    .maxByOrNull { Instant.parse(requireNotNull(it.capturedAt)) }

private fun isLater(candidate: LatestWatchSample, current: LatestWatchSample): Boolean =
    Instant.parse(requireNotNull(candidate.capturedAt)) > Instant.parse(requireNotNull(current.capturedAt))

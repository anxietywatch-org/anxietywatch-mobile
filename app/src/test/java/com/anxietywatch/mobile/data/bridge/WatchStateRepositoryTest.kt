package com.anxietywatch.mobile.data.bridge

import com.anxietywatch.mobile.data.local.PendingTelemetryBatchEntity
import com.anxietywatch.mobile.data.remote.CreateTelemetryBatchRequest
import com.anxietywatch.mobile.data.remote.TelemetrySampleDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchStateRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun latestBatchWithoutHeartRateUsesPreviousValidHeartRate() {
        val result = selectLatestHeartRateSample(
            listOf(batch(2_000L, sample("2026-08-30T10:01:00Z", null)), batch(1_000L, sample("2026-08-30T10:00:00Z", 78.0))),
            json,
        )

        assertEquals(78, result?.heartRateBpm)
        assertEquals("2026-08-30T10:00:00Z", result?.capturedAt)
    }

    @Test
    fun multipleBatchesWithoutHeartRateUseOlderValidHeartRate() {
        val result = selectLatestHeartRateSample(
            listOf(
                batch(3_000L, sample("2026-08-30T10:02:00Z", null)),
                batch(2_000L, sample("2026-08-30T10:01:00Z", null)),
                batch(1_000L, sample("2026-08-30T10:00:00Z", 72.0)),
            ),
            json,
        )

        assertEquals(72, result?.heartRateBpm)
    }

    @Test
    fun noHeartRateReturnsNull() {
        val result = selectLatestHeartRateSample(
            listOf(batch(2_000L, sample("2026-08-30T10:01:00Z", null)), batch(1_000L, sample("2026-08-30T10:00:00Z", null))),
            json,
        )

        assertNull(result)
    }

    @Test
    fun newerHeartRateReplacesOlderHeartRate() {
        val result = selectLatestHeartRateSample(
            listOf(batch(2_000L, sample("2026-08-30T10:01:00Z", 81.0)), batch(1_000L, sample("2026-08-30T10:00:00Z", 72.0))),
            json,
        )

        assertEquals(81, result?.heartRateBpm)
        assertEquals("2026-08-30T10:01:00Z", result?.capturedAt)
    }

    @Test
    fun sampleTimestampWinsOverBatchCreationOrder() {
        val result = selectLatestHeartRateSample(
            listOf(
                batch(2_000L, sample("2026-08-30T10:00:00Z", 72.0)),
                batch(1_000L, sample("2026-08-30T10:01:00Z", 81.0)),
            ),
            json,
        )

        assertEquals(81, result?.heartRateBpm)
        assertEquals("2026-08-30T10:01:00Z", result?.capturedAt)
    }

    @Test
    fun newerBatchWithoutHeartRateDoesNotClearPreviousHeartRate() {
        val result = selectLatestHeartRateSample(
            listOf(batch(2_000L, sample("2026-08-30T10:01:00Z", null)), batch(1_000L, sample("2026-08-30T10:00:00Z", 81.0))),
            json,
        )

        assertEquals(81, result?.heartRateBpm)
        assertEquals("2026-08-30T10:00:00Z", result?.capturedAt)
    }

    private fun batch(createdAtMillis: Long, vararg samples: TelemetrySampleDto) =
        PendingTelemetryBatchEntity(
            batchId = "batch-$createdAtMillis",
            requestJson = json.encodeToString(
                CreateTelemetryBatchRequest(
                    batchId = "batch-$createdAtMillis",
                    deviceId = "device",
                    sessionId = "session",
                    startedAt = samples.first().timestamp,
                    endedAt = samples.last().timestamp,
                    sequence = createdAtMillis.toInt(),
                    samples = samples.toList(),
                ),
            ),
            createdAtMillis = createdAtMillis,
        )

    private fun sample(timestamp: String, heartRateBpm: Double?) = TelemetrySampleDto(
        timestamp = timestamp,
        heartRateBpm = heartRateBpm,
        ibiMs = emptyList(),
    )
}

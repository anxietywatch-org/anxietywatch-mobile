package com.anxietywatch.mobile.data.bridge

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneTelemetryMappingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun heartRateTypePopulatesHttpSample() {
        val sample = MutableTelemetrySample()

        applyTelemetryReading(sample, " heart_rate ", jsonObject("bpm" to 72.0, "signalQuality" to 0.9))

        val dto = sample.toDto(1_000L)
        assertEquals(72.0, dto.heartRateBpm)
        assertEquals("unknown", dto.quality.wearingState)
    }

    @Test
    fun heartRateWearingStateIsPassedThrough() {
        val onBody = MutableTelemetrySample()
        applyTelemetryReading(onBody, "heart_rate", buildJsonObject { put("bpm", 72.0); put("wearingState", "onBody") })
        assertEquals("onBody", onBody.toDto(1_000L).quality.wearingState)

        val offBody = MutableTelemetrySample()
        applyTelemetryReading(offBody, "heart_rate", buildJsonObject { put("bpm", 0.0); put("wearingState", "OFFBODY") })
        assertEquals("offBody", offBody.toDto(1_000L).quality.wearingState)

        val garbage = MutableTelemetrySample()
        applyTelemetryReading(garbage, "heart_rate", buildJsonObject { put("bpm", 72.0); put("wearingState", "wat") })
        assertEquals("unknown", garbage.toDto(1_000L).quality.wearingState)
    }

    @Test
    fun accelerometerTypePopulatesThreeAxes() {
        val sample = MutableTelemetrySample()

        applyTelemetryReading(sample, "ACCELEROMETER", jsonObject("x" to 0.02, "y" to -0.98, "z" to 0.15))

        val dto = sample.toDto(1_000L)
        assertEquals(0.02, dto.accelerometer?.x)
        assertEquals(-0.98, dto.accelerometer?.y)
        assertEquals(0.15, dto.accelerometer?.z)
    }

    @Test
    fun accelerometerWithMissingAxisIsIgnored() {
        val sample = MutableTelemetrySample()

        applyTelemetryReading(sample, "accelerometer", jsonObject("x" to 0.02, "y" to -0.98))

        assertNull(sample.toDto(1_000L).accelerometer)
    }

    @Test
    fun skinTemperatureTypePopulatesHttpSample() {
        val sample = MutableTelemetrySample()

        applyTelemetryReading(sample, "skin_temperature", jsonObject("celsius" to 35.0))

        val dto = sample.toDto(1_000L)
        assertEquals(35.0, dto.skinTemperatureCelsius)
        assertNull(dto.ambientTemperatureCelsius)
    }

    @Test
    fun unknownTypeDoesNotCrashOrPopulateOtherFields() {
        val sample = MutableTelemetrySample()

        applyTelemetryReading(sample, "unknown_sensor", jsonObject("bpm" to 72.0, "celsius" to 35.0))

        val dto = sample.toDto(1_000L)
        assertNull(dto.heartRateBpm)
        assertNull(dto.skinTemperatureCelsius)
        assertEquals(emptyList<Double>(), dto.ibiMs)
    }

    private fun jsonObject(vararg values: Pair<String, Double>) = buildJsonObject {
        values.forEach { (key, value) -> put(key, value) }
    }
}

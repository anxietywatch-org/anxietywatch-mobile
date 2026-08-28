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

package com.anxietywatch.mobile.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiDtosTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun sessionResponseDeserializesBackendShape() {
        val response = json.decodeFromString<AuthenticationResponseDto>(sessionJson)

        assertEquals("test-token", response.token)
        assertEquals("2026-08-30T12:00:00Z", response.expiresAt)
        assertEquals("user-1", response.user.id)
        assertEquals("Paciente de prueba", response.user.fullName)
        assertEquals("patient@example.test", response.user.email)
        assertEquals("free", response.user.planId)
        assertEquals(false, response.user.emailVerified)
        assertNull(response.user.avatarUrl)
        assertEquals("patient", response.user.role)
    }

    @Test
    fun authenticationResponseShapeIsSharedByRegisterAndLogin() {
        val response = json.decodeFromString<AuthenticationResponseDto>(sessionJson)

        assertEquals("test-token", response.token)
        assertEquals("user-1", response.user.id)
        assertEquals("patient", response.user.role)
    }

    @Test
    fun tokenRedeemResponseDeserializesRoleAndUser() {
        val response = json.decodeFromString<TokenRedeemResponseDto>(tokenRedeemJson)

        assertEquals("redeemed-token", response.token)
        assertEquals("2026-08-30T12:00:00Z", response.expiresAt)
        assertEquals("family_member", response.role)
        assertEquals("caregiver-1", response.user.id)
        assertEquals("family_member", response.user.role)
    }

    @Test
    fun wearableResponsesUseAcceptedAndDuplicateContract() {
        val telemetry = json.decodeFromString<TelemetryBatchAckResponse>("""
            { "batchId": "batch-1", "accepted": true, "duplicate": false }
        """)
        val sos = json.decodeFromString<SosTriggerResponse>("""
            { "eventId": "event-1", "accepted": false, "duplicate": true }
        """)
        val cancel = json.decodeFromString<SosCancelResponse>("""
            { "eventId": "event-1", "accepted": true, "duplicate": false }
        """)
        val suspected = json.decodeFromString<WearableEventResponse>("""
            { "eventId": "event-2", "accepted": true, "duplicate": false }
        """)
        val decision = json.decodeFromString<WearableEventResponse>("""
            { "eventId": "event-3", "accepted": false, "duplicate": true }
        """)

        assertEquals("batch-1", telemetry.batchId)
        assertEquals(true, telemetry.accepted)
        assertEquals(false, telemetry.duplicate)
        assertEquals("event-1", sos.eventId)
        assertEquals(false, sos.accepted)
        assertEquals(true, sos.duplicate)
        assertEquals("event-1", cancel.eventId)
        assertEquals("event-2", suspected.eventId)
        assertEquals("event-3", decision.eventId)
    }

    @Test
    fun duplicateSubmissionIsDeliveredOnlyWhenResponseIdMatches() {
        assertEquals(true, isWearableSubmissionDelivered("event-1", "event-1", false, true))
        assertEquals(true, isWearableSubmissionDelivered("event-1", "event-1", true, false))
        assertEquals(false, isWearableSubmissionDelivered("event-1", "event-1", false, false))
        assertEquals(false, isWearableSubmissionDelivered("event-1", "event-2", true, false))
    }

    @Test
    fun routeAndPayloadIdsMustMatchForEveryWearOperation() {
        assertEquals(true, responseIdMatches("batch-1", "batch-1"))
        assertEquals(false, responseIdMatches("batch-1", "batch-2"))
        assertEquals(false, responseIdMatches("sos-1", "sos-2"))
        assertEquals(false, responseIdMatches("cancel-1", "cancel-2"))
        assertEquals(false, responseIdMatches("suspected-1", "suspected-2"))
        assertEquals(false, responseIdMatches("decision-1", "decision-2"))
    }

    @Test
    fun wearableRequestsKeepNullableUserIdAndIbi() {
        val sample = TelemetrySampleDto(timestamp = "2026-08-26T00:00:00Z", ibiMs = emptyList())
        val request = CreateTelemetryBatchRequest(
            batchId = "batch-1",
            deviceId = "device-1",
            sessionId = "session-1",
            startedAt = sample.timestamp,
            endedAt = sample.timestamp,
            sequence = 1,
            samples = listOf(sample),
        )

        assertNull(request.userId)
        assertEquals(emptyList<Double>(), request.samples.single().ibiMs)
    }

    @Test
    fun nullableSensorIbiMapsToEmptyHttpListAndRealIbiIsPreserved() {
        assertEquals(emptyList<Double>(), httpIbiMs(null))
        assertEquals(listOf(810.0, 820.0), httpIbiMs(listOf(810.0, 820.0)))
    }

    @Test
    fun emptyIbiIsSerializedAsAnExplicitEmptyArray() {
        val sample = TelemetrySampleDto(
            timestamp = "2026-08-26T00:00:00Z",
            ibiMs = emptyList(),
        )

        val serialized = json.encodeToString(sample)

        assertTrue(serialized.contains("\"ibiMs\":[]"))
    }

    @Test
    fun failedBatchShapeSerializesRequiredQualityFields() {
        val request = CreateTelemetryBatchRequest(
            batchId = "123e4567-e89b-12d3-a456-426614174000",
            deviceId = "123e4567-e89b-12d3-a456-426614174001",
            sessionId = "123e4567-e89b-12d3-a456-426614174002",
            startedAt = "2026-08-30T02:49:36.003Z",
            endedAt = "2026-08-30T02:49:44.089Z",
            sequence = 3,
            samples = listOf(
                TelemetrySampleDto(
                    timestamp = "2026-08-30T02:49:36.003Z",
                    ibiMs = emptyList(),
                    quality = SampleQualityDto(wearingState = "onBody"),
                ),
            ),
        )

        val serialized = json.encodeToString(request)

        assertTrue(serialized.contains("\"ibiMs\":[]"))
        assertTrue(serialized.contains("\"heartRate\":\"unknown\""))
        assertTrue(serialized.contains("\"ibi\":\"unknown\""))
        assertTrue(serialized.contains("\"wearingState\":\"onBody\""))
        assertTrue(serialized.contains("\"batchId\":\"123e4567-e89b-12d3-a456-426614174000\""))
        assertTrue(serialized.contains("\"startedAt\":\"2026-08-30T02:49:36.003Z\""))
        assertTrue(serialized.contains("\"endedAt\":\"2026-08-30T02:49:44.089Z\""))
    }

    @Test
    fun nonEmptyIbiIsSerializedWithoutChangingValues() {
        val sample = TelemetrySampleDto(
            timestamp = "2026-08-26T00:00:00Z",
            ibiMs = listOf(810.0, 820.0),
        )

        val serialized = json.encodeToString(sample)

        assertTrue(serialized.contains("\"ibiMs\":[810.0,820.0]"))
    }

    @Test
    fun allWearableUploadsRejectMissingOrEmptyDeviceId() {
        listOf("telemetry", "sos", "sos-cancel", "suspected", "decision").forEach { _ ->
            assertEquals(false, isValidWearableDeviceId(null))
            assertEquals(false, isValidWearableDeviceId("00000000-0000-0000-0000-000000000000"))
        }
        assertEquals(true, isValidWearableDeviceId("123e4567-e89b-12d3-a456-426614174000"))
    }

    private companion object {
        const val sessionJson = """
            {
              "token": "test-token",
              "expiresAt": "2026-08-30T12:00:00Z",
              "user": {
                "id": "user-1",
                "fullName": "Paciente de prueba",
                "email": "patient@example.test",
                "planId": "free",
                "emailVerified": false,
                "avatarUrl": null,
                "role": "patient"
              }
            }
        """

        const val tokenRedeemJson = """
            {
              "token": "redeemed-token",
              "expiresAt": "2026-08-30T12:00:00Z",
              "role": "family_member",
              "user": {
                "id": "caregiver-1",
                "fullName": "Cuidador",
                "email": "caregiver@example.test",
                "planId": "free",
                "emailVerified": false,
                "avatarUrl": null,
                "role": "family_member"
              }
            }
        """
    }
}

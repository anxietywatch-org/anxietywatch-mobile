package com.anxietywatch.mobile.data.remote

import kotlinx.serialization.Serializable

// Contrato CONFIRMADO en producción (https://api.mangoon.xyz) por el equipo de backend,
// 11/ago/2026. Es distinto del que yo había diseñado originalmente en el .NET — este es
// el real, así que es el que manda. Si algo cambia del lado del backend, avisa para
// actualizar este archivo también.

// --- Telemetría ---

@Serializable
data class AccelerometerSampleDto(
    val x: Double,
    val y: Double,
    val z: Double,
)

@Serializable
data class SampleQualityDto(
    val heartRate: String = "unknown", // good | fair | poor | unknown
    val ibi: String = "unknown",
    val wearingState: String = "unknown", // onBody | offBody | unknown
)

@Serializable
data class TelemetrySampleDto(
    val timestamp: String, // ISO-8601 UTC, ej. "2026-08-11T23:50:00Z"
    val heartRateBpm: Double? = null,
    val ibiMs: List<Double>? = null,
    val accelerometer: AccelerometerSampleDto? = null,
    val skinTemperatureCelsius: Double? = null,
    val ambientTemperatureCelsius: Double? = null,
    val quality: SampleQualityDto = SampleQualityDto(),
)

@Serializable
data class CreateTelemetryBatchRequest(
    val batchId: String, // Guid — mismo batchId que generó apps/wear en OutboxSyncer
    val deviceId: String, // Guid del reloj vinculado
    val userId: String? = null, // el backend lo infiere del JWT — mandar null desde el móvil
    val sessionId: String, // Guid de la sesión de monitoreo actual del móvil
    val startedAt: String,
    val endedAt: String,
    val sequence: Int,
    val samples: List<TelemetrySampleDto>,
)

// El backend responde 202 (lote nuevo) o 200 (duplicado, idempotente) — Retrofit trata
// ambos como éxito por igual, así que no hace falta distinguirlos en el DTO.
@Serializable
data class TelemetryBatchAckResponse(
    val batchId: String? = null,
    val status: String? = null,
)

// --- SOS ---

@Serializable
data class TriggerSosRequest(
    val eventId: String, // Guid — mismo eventId que generó BackgroundEngine en el reloj
    val deviceId: String,
    val userId: String? = null, // el backend lo infiere del JWT — mandar null desde el móvil
    val triggeredAt: String,
    val source: String, // "WATCH" | "MOBILE"
    val reason: String, // texto libre, ej. "Boton SOS presionado" o el motivo de la detección
)

@Serializable
data class SosTriggerResponse(
    val eventId: String? = null,
    val status: String? = null,
)

// --- Auth (register/login/session — ya viven en producción) ---

@Serializable
data class UserResponseDto(
    val id: String,
    val fullName: String,
    val email: String,
    val planId: String,
    val emailVerified: Boolean,
    val avatarUrl: String? = null,
    val role: String? = null,
)

@Serializable
data class AuthenticationResponseDto(
    val token: String,
    val expiresAt: String,
    val user: UserResponseDto,
)

/** Respuesta de accept-by-code -- distinta de login/register: además del JWT trae el
 * rol que traía el token (self | family_member | patient), necesario para saber a
 * qué pantalla mandar al usuario justo después de activarse. */
@Serializable
data class TokenRedeemResponseDto(
    val token: String,
    val expiresAt: String,
    val role: String,
    val user: UserResponseDto,
)

@Serializable
data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val planId: String = "free",
    val billingCycle: String = "monthly",
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

// --- Tokens de vinculación (Red_Apoyo) ---

@Serializable
data class TokenResponseDto(
    val id: String,
    val code: String, // formato AW-XXXX-XXXX-XXXX
    val role: String, // self | family_member | patient
    val expiresAt: String,
    val status: String,
)

@Serializable
data class CreateTokenRequest(
    val role: String,
)

/**
 * Endpoint AUN NO DESPLEGADO -- lo construyo yo en el backend .NET. Por codigo en vez
 * de por id interno del token, y SIN requerir sesion previa (activa la cuenta con el
 * mismo request). Ver conversacion: "Opcion A".
 */
@Serializable
data class AcceptByCodeRequest(
    val code: String,
    val deviceId: String,
)

@Serializable
data class SosCancelRequest(
    val eventId: String,
    val deviceId: String,
    val userId: String? = null,
    val cancelledAt: String,
    val reason: String? = null,
)

@Serializable
data class SosCancelResponse(
    val eventId: String? = null,
    val status: String? = null,
)

@Serializable
data class SuspectedEventFeaturesRequest(
    val heartRateMean: Double? = null,
    val heartRateMax: Double? = null,
    val heartRateSlopeBpmPerMinute: Double? = null,
    val heartRateDeltaFromBaseline: Double? = null,
    val rmssdMillis: Double? = null,
    val sdnnMillis: Double? = null,
    val movementMagnitudeMean: Double? = null,
    val movementVariance: Double? = null,
    val validSampleRatio: Double,
    val lastSampleAgeSeconds: Long,
    val sampleCount: Int,
)

@Serializable
data class SuspectedEventBaselineRequest(
    val sampleCount: Long,
    val meanHeartRate: Double,
    val heartRateM2: Double,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class SuspectedEventRequest(
    val eventId: String,
    val deviceId: String,
    val userId: String? = null,
    val sessionId: String,
    val sequence: Int,
    val detectedAt: String,
    val state: String,
    val score: Double,
    val rulesVersion: String,
    val features: SuspectedEventFeaturesRequest,
    val baseline: SuspectedEventBaselineRequest,
)

@Serializable
data class EventDecisionRequest(
    val eventId: String,
    val deviceId: String,
    val userId: String? = null,
    val sessionId: String,
    val sequence: Int,
    val detectedAt: String,
    val respondedAt: String,
    val response: String,
)

@Serializable
data class WearableEventResponse(
    val eventId: String? = null,
    val status: String? = null,
)

// --- Perfil, dashboard y episodios (contrato confirmado en backend) ---

@Serializable
data class ProfileResponseDto(
    val fullName: String,
    val avatarUrl: String? = null,
    val allergies: String? = null,
    val currentMedications: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val previousAnxietyDiagnosis: Boolean? = null,
    val treatingProfessional: String? = null,
)

@Serializable
data class ProfileUpdateRequest(
    val fullName: String,
    val avatarUrl: String? = null,
    val allergies: String? = null,
    val currentMedications: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val previousAnxietyDiagnosis: Boolean? = null,
    val treatingProfessional: String? = null,
)

@Serializable
data class DashboardSummaryDto(
    val anxietyLevel: AnxietyLevelDto,
    val weeklyRecords: WeeklyRecordsDto,
    val streakDays: Int,
    val exercisesCompleted: Int,
)

@Serializable
data class AnxietyLevelDto(
    val current: Int,
    val trend: String,
)

@Serializable
data class WeeklyRecordsDto(
    val used: Int,
    val limit: Int? = null,
)

@Serializable
data class EpisodeDto(
    val id: String,
    val date: String,
    val intensity: Int,
    val symptoms: List<String>,
    val notes: String? = null,
)

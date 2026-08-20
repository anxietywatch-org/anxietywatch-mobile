package com.anxietywatch.mobile.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthUser(
    val id: String,
    val fullName: String,
    val email: String,
    val planId: String,
    val emailVerified: Boolean,
    val avatarUrl: String? = null,
    val role: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val expiresAt: String,
    val user: AuthUser
)

@Serializable
data class AcceptCodeRequest(
    val code: String,
    val deviceId: String
)

@Serializable
data class AcceptCodeResponse(
    val token: String,
    val expiresAt: String,
    val role: String,
    val user: AuthUser
)

@Serializable
data class AnxietyLevel(
    val current: Int,
    val trend: String
)

@Serializable
data class WeeklyRecords(
    val used: Int,
    val limit: Int
)

@Serializable
data class DashboardSummary(
    val anxietyLevel: AnxietyLevel,
    val weeklyRecords: WeeklyRecords,
    val streakDays: Int,
    val exercisesCompleted: Int
)
@Serializable
data class EpisodeSummary(
    val id: String? = null,
    val date: String? = null,
    val severity: String? = null,
    val durationMinutes: Int? = null,
    val notes: String? = null
)
@Serializable
data class MedicalProfileUpdate(
    val fullName: String,
    val allergies: String? = null,
    val currentMedications: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val previousAnxietyDiagnosis: Boolean? = null,
    val treatingProfessional: String? = null
)
@Serializable
data class ProfileResponse(
    val fullName: String,
    val avatarUrl: String? = null,
    val allergies: String? = null,
    val currentMedications: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val previousAnxietyDiagnosis: Boolean? = null,
    val treatingProfessional: String? = null
)

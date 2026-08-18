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
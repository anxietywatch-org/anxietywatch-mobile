package com.anxietywatch.mobile.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AnxietyWatchApi {

    @GET("health")
    suspend fun getHealth(): HealthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/tokens/accept-by-code")
    suspend fun acceptByCode(@Body request: AcceptCodeRequest): AcceptCodeResponse
}
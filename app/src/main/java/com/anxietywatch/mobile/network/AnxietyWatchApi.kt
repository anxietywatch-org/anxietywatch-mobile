package com.anxietywatch.mobile.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AnxietyWatchApi {

    @GET("health")
    suspend fun getHealth(): HealthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/tokens/accept-by-code")
    suspend fun acceptByCode(@Body request: AcceptCodeRequest): AcceptCodeResponse

    @GET("api/dashboard/summary")
    suspend fun getDashboardSummary(): DashboardSummary

    @GET("api/episodes")
    suspend fun getEpisodes(): List<EpisodeSummary>

    @GET("api/profile")
    suspend fun getProfile(): ProfileResponse

    @PATCH("api/profile")
    suspend fun updateProfile(@Body request: MedicalProfileUpdate): okhttp3.ResponseBody

    @GET("api/tokens")
    suspend fun getTokensRaw(): okhttp3.ResponseBody

    @POST("api/tokens")
    suspend fun generateTokenRaw(@Body request: GenerateTokenRequest): okhttp3.ResponseBody

    @POST("api/v1/sos/trigger")
    suspend fun triggerSos(@Body request: SosTriggerRequest): okhttp3.ResponseBody

    @POST("api/v1/sos/cancel")
    suspend fun cancelSos(@Body request: SosCancelRequest): okhttp3.ResponseBody

    @POST("api/devices/register")
    suspend fun registerDevice(@Body request: DeviceRegisterRequest): okhttp3.ResponseBody
}
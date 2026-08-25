package com.anxietywatch.mobile.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Cliente de https://api.mangoon.xyz -- contrato confirmado en produccion por el equipo
 * de backend el 11/ago/2026. Todas las rutas protegidas requieren
 * Authorization: Bearer <jwt> -- lo anade AuthInterceptor automaticamente.
 */
interface AnxietyWatchApi {

    // --- Auth ---
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthenticationResponseDto

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthenticationResponseDto

    @GET("api/auth/session")
    suspend fun session(): UserResponseDto

    // --- Mobile / wearable (las 2 que construimos, ya en produccion) ---
    @POST("api/v1/telemetry/batch")
    suspend fun sendTelemetryBatch(@Body request: CreateTelemetryBatchRequest): TelemetryBatchAckResponse

    @POST("api/v1/sos/trigger")
    suspend fun triggerSos(@Body request: TriggerSosRequest): SosTriggerResponse

    @POST("api/v1/sos/cancel")
    suspend fun cancelSos(@Body request: SosCancelRequest): SosCancelResponse

    @POST("api/v1/events/suspected")
    suspend fun submitSuspectedEvent(@Body request: SuspectedEventRequest): WearableEventResponse

    @POST("api/v1/events/decision")
    suspend fun submitEventDecision(@Body request: EventDecisionRequest): WearableEventResponse

    // --- Tokens de vinculacion ---
    @GET("api/tokens")
    suspend fun listTokens(): List<TokenResponseDto>

    @POST("api/tokens")
    suspend fun createToken(@Body request: CreateTokenRequest): TokenResponseDto

    /**
     * AUN NO DESPLEGADO en el backend -- lo agrego en el mismo commit que el resto de
     * "Opcion A". Es la unica pieza que falta para que el flujo de "Ingreso por Token"
     * (E02) funcione sin pantalla de login tradicional.
     */
    @POST("api/tokens/accept-by-code")
    suspend fun acceptByCode(@Body request: AcceptByCodeRequest): TokenRedeemResponseDto

    // --- Perfil, dashboard y episodios (contrato confirmado en backend) ---
    @GET("api/profile")
    suspend fun getProfile(): ProfileResponseDto

    @PATCH("api/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): ProfileResponseDto

    @GET("api/dashboard/summary")
    suspend fun getDashboardSummary(): DashboardSummaryDto

    @GET("api/episodes")
    suspend fun getEpisodes(@Query("range") range: Int = 7): List<EpisodeDto>
}

package com.anxietywatch.mobile.network

import retrofit2.http.GET

interface AnxietyWatchApi {

    @GET("health")
    suspend fun getHealth(): HealthResponse
}
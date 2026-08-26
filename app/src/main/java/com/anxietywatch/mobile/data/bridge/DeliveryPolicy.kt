package com.anxietywatch.mobile.data.bridge

import retrofit2.HttpException
import java.io.IOException

enum class RetryClass {
    TRANSIENT,
    WAIT_FOR_AUTH,
    TERMINAL,
}

data class BackendDeliveryResponse(
    val responseId: String,
    val accepted: Boolean,
    val duplicate: Boolean,
)

object DeliveryPolicy {
    const val MAX_TRANSIENT_ATTEMPTS = 5

    fun backendDelivered(expectedId: String, response: BackendDeliveryResponse): Boolean =
        expectedId == response.responseId && (response.accepted || response.duplicate)

    fun classifyFailure(error: Throwable): RetryClass = when (error) {
        is IOException -> RetryClass.TRANSIENT
        is HttpException -> when (error.code()) {
            401 -> RetryClass.WAIT_FOR_AUTH
            400, 403, 404, 409, 422 -> RetryClass.TERMINAL
            408, 429 -> RetryClass.TRANSIENT
            in 500..599 -> RetryClass.TRANSIENT
            else -> RetryClass.TERMINAL
        }
        else -> RetryClass.TRANSIENT
    }

    fun shouldTerminalize(attemptCount: Int): Boolean =
        attemptCount >= MAX_TRANSIENT_ATTEMPTS
}

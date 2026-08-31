package com.anxietywatch.mobile.push

import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.RegisterDeviceRequest
import com.anxietywatch.mobile.data.remote.UnregisterDeviceRequest

object PushTokenRegistrar {
    suspend fun register(api: AnxietyWatchApi, token: String) {
        api.registerDevice(RegisterDeviceRequest(platform = "android", token = token))
    }

    suspend fun unregister(api: AnxietyWatchApi, token: String) {
        api.unregisterDevice(UnregisterDeviceRequest(token = token))
    }
}

/** Clears local authentication even when best-effort device cleanup cannot reach the API. */
internal suspend fun logoutWithPushCleanup(
    tokenProvider: suspend () -> String?,
    unregister: suspend (String) -> Unit,
    clearSession: suspend () -> Unit,
    onUnregisterFailure: (Throwable) -> Unit,
) {
    val token = runCatching { tokenProvider() }
        .onFailure(onUnregisterFailure)
        .getOrNull()
    if (!token.isNullOrBlank()) {
        runCatching { unregister(token) }
            .onFailure(onUnregisterFailure)
    }
    clearSession()
}

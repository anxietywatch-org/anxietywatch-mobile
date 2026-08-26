package com.anxietywatch.mobile.push

import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.RegisterDeviceRequest

object PushTokenRegistrar {
    suspend fun register(api: AnxietyWatchApi, token: String) {
        api.registerDevice(RegisterDeviceRequest(platform = "android", token = token))
    }
}

package com.anxietywatch.mobile.network

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

object DeviceIdProvider {

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown-device"
    }
}
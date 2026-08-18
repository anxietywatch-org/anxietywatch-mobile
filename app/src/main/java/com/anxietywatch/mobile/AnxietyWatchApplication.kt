package com.anxietywatch.mobile

import android.app.Application
import com.anxietywatch.mobile.network.NetworkModule

class AnxietyWatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkModule.init(this)
    }
}
package com.anxietywatch.mobile.observability

import android.util.Log
import com.anxietywatch.mobile.BuildConfig

object DebugTrace {
    private const val TAG = "AWTRACE"

    fun telemetry(event: String, batchId: String, details: String = "") {
        if (!BuildConfig.DEBUG) return
        val suffix = batchId.takeLast(8)
        Log.d(TAG, "AWTRACE|TELEMETRY|$event|batch=$suffix|$details")
    }
}

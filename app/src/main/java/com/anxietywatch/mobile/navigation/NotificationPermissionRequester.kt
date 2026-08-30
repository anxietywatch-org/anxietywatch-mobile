package com.anxietywatch.mobile.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

internal fun shouldRequestNotificationPermission(sdkInt: Int, permissionGranted: Boolean): Boolean =
    sdkInt >= Build.VERSION_CODES.TIRAMISU && !permissionGranted

@Composable
internal fun NotificationPermissionRequester(isAuthenticated: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var requestAttempted by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // A denial must not block the authenticated app or trigger a request loop.
        requestAttempted = true
    }

    LaunchedEffect(isAuthenticated) {
        if (requestAttempted || !isAuthenticated) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!shouldRequestNotificationPermission(Build.VERSION.SDK_INT, granted)) {
            requestAttempted = true
            return@LaunchedEffect
        }
        requestAttempted = true
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

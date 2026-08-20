package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.anxietywatch.mobile.network.NetworkModule
import kotlinx.coroutines.delay

private enum class CaregiverTab { HOME, GUIDE, SETTINGS }
private enum class CaregiverOverlay { NONE, PROFILE, SECURITY, HELP, TERMS, ABOUT, SHARE_CODE, PATIENT_DETAIL, PENDING_CODE, CRITICAL_ALERT }

@Composable
fun CaregiverRootScreen(onLogout: () -> Unit, onOpenNotifications: () -> Unit) {
    var selectedTab by remember { mutableStateOf(CaregiverTab.HOME) }
    var overlay by remember { mutableStateOf(CaregiverOverlay.NONE) }
    var avatarUri by remember { mutableStateOf(NetworkModule.getSessionManager().getAvatarUri()) }
    var selectedPatientName by remember { mutableStateOf("") }
    var selectedPendingCode by remember { mutableStateOf<String?>(null) }
    var alertPatientName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            if (NetworkModule.getSessionManager().hasPendingCriticalAlert() && overlay == CaregiverOverlay.NONE) {
                alertPatientName = NetworkModule.getSessionManager().getPendingCriticalAlertPatientName() ?: "Tu paciente"
                NetworkModule.getSessionManager().clearPendingCriticalAlert()
                overlay = CaregiverOverlay.CRITICAL_ALERT
            }
        }
    }

    when (overlay) {
        CaregiverOverlay.PROFILE -> { CaregiverProfileScreen(avatarUri = avatarUri, onAvatarChanged = { avatarUri = it }, onBack = { overlay = CaregiverOverlay.NONE }); return }
        CaregiverOverlay.SECURITY -> { SecurityScreen(onBack = { overlay = CaregiverOverlay.NONE }); return }
        CaregiverOverlay.HELP -> { HelpScreen(onBack = { overlay = CaregiverOverlay.NONE }); return }
        CaregiverOverlay.TERMS -> { TermsScreen(onBack = { overlay = CaregiverOverlay.NONE }); return }
        CaregiverOverlay.ABOUT -> { AboutScreen(onBack = { overlay = CaregiverOverlay.NONE }); return }
        CaregiverOverlay.SHARE_CODE -> { ShareCodeScreen(onBack = { overlay = CaregiverOverlay.NONE }); return }
        CaregiverOverlay.PATIENT_DETAIL -> { PatientDetailScreen(patientName = selectedPatientName, onBack = { overlay = CaregiverOverlay.NONE }); return }
        CaregiverOverlay.PENDING_CODE -> { PendingCodeScreen(code = selectedPendingCode, onBack = { overlay = CaregiverOverlay.NONE }); return }
        CaregiverOverlay.CRITICAL_ALERT -> {
            CriticalAlertScreen(
                patientName = alertPatientName,
                onCallPatient = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:"))
                    // El contexto aquí requiere LocalContext; se resuelve dentro de CriticalAlertScreen.
                },
                onOpenGuide = { selectedTab = CaregiverTab.GUIDE; overlay = CaregiverOverlay.NONE },
                onDismiss = {
                    NetworkModule.getSessionManager().recordCriticalAlertHistory(alertPatientName)
                    overlay = CaregiverOverlay.NONE
                }
            )
            return
        }
        CaregiverOverlay.NONE -> {}
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == CaregiverTab.HOME,
                    onClick = { selectedTab = CaregiverTab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == CaregiverTab.GUIDE,
                    onClick = { selectedTab = CaregiverTab.GUIDE },
                    icon = { Icon(Icons.Filled.MenuBook, contentDescription = "Guía") },
                    label = { Text("Guía") }
                )
                NavigationBarItem(
                    selected = selectedTab == CaregiverTab.SETTINGS,
                    onClick = { selectedTab = CaregiverTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            CaregiverTab.HOME -> CaregiverHomeScreen(
                modifier = Modifier.padding(innerPadding),
                avatarUri = avatarUri,
                onAvatarClick = { overlay = CaregiverOverlay.PROFILE },
                onOpenNotifications = onOpenNotifications,
                onGenerateCode = { overlay = CaregiverOverlay.SHARE_CODE },
                onOpenPatientDetail = { name ->
                    selectedPatientName = name
                    overlay = CaregiverOverlay.PATIENT_DETAIL
                },
                onViewPendingCode = { token ->
                    selectedPendingCode = token.code
                    overlay = CaregiverOverlay.PENDING_CODE
                }
            )
            CaregiverTab.GUIDE -> CaregiverGuideScreenBody(
                modifier = Modifier.padding(innerPadding),
                onFinished = { selectedTab = CaregiverTab.HOME }
            )
            CaregiverTab.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(innerPadding),
                isCaregiver = true,
                onLogout = onLogout,
                onOpenProfile = { overlay = CaregiverOverlay.PROFILE },
                onOpenSecurity = { overlay = CaregiverOverlay.SECURITY },
                onOpenHelp = { overlay = CaregiverOverlay.HELP },
                onOpenTerms = { overlay = CaregiverOverlay.TERMS },
                onOpenAbout = { overlay = CaregiverOverlay.ABOUT }
            )
        }
    }
}
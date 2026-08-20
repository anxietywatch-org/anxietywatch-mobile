package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.anxietywatch.mobile.network.NetworkModule

private enum class PatientTab { HOME, HISTORY, SETTINGS }
private enum class Overlay { NONE, PROFILE, WATCH, RELAXATION, BREATHING, MUSIC, SECURITY, HELP, TERMS, ABOUT }

@Composable
fun PatientRootScreen(onLogout: () -> Unit, onOpenNotifications: () -> Unit) {
    var selectedTab by remember { mutableStateOf(PatientTab.HOME) }
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var avatarUri by remember { mutableStateOf(NetworkModule.getSessionManager().getAvatarUri()) }

    when (overlay) {
        Overlay.PROFILE -> { ProfileScreen(avatarUri = avatarUri, onAvatarChanged = { avatarUri = it }, onBack = { overlay = Overlay.NONE }); return }
        Overlay.WATCH -> { WatchLinkScreen(onFinished = { overlay = Overlay.NONE }); return }
        Overlay.RELAXATION -> { RelaxationScreen(onOpenBreathing = { overlay = Overlay.BREATHING }, onOpenMusic = { overlay = Overlay.MUSIC }, onBack = { overlay = Overlay.NONE }); return }
        Overlay.BREATHING -> { BreathingExerciseScreen(onBack = { overlay = Overlay.RELAXATION }); return }
        Overlay.MUSIC -> { MusicScreen(onBack = { overlay = Overlay.RELAXATION }); return }
        Overlay.SECURITY -> { SecurityScreen(onBack = { overlay = Overlay.NONE }); return }
        Overlay.HELP -> { HelpScreen(onBack = { overlay = Overlay.NONE }); return }
        Overlay.TERMS -> { TermsScreen(onBack = { overlay = Overlay.NONE }); return }
        Overlay.ABOUT -> { AboutScreen(onBack = { overlay = Overlay.NONE }); return }
        Overlay.NONE -> {}
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == PatientTab.HOME,
                    onClick = { selectedTab = PatientTab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == PatientTab.HISTORY,
                    onClick = { selectedTab = PatientTab.HISTORY },
                    icon = { Icon(Icons.Filled.History, contentDescription = "Historial") },
                    label = { Text("Historial") }
                )
                NavigationBarItem(
                    selected = selectedTab == PatientTab.SETTINGS,
                    onClick = { selectedTab = PatientTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            PatientTab.HOME -> PatientHomeScreen(
                modifier = Modifier.padding(innerPadding),
                onOpenSettings = { selectedTab = PatientTab.SETTINGS },
                onOpenHistory = { selectedTab = PatientTab.HISTORY },
                onOpenNotifications = onOpenNotifications,
                onOpenRelaxation = { overlay = Overlay.RELAXATION },
                avatarUri = avatarUri,
                onAvatarClick = { overlay = Overlay.PROFILE }
            )
            PatientTab.HISTORY -> HistoryScreen(modifier = Modifier.padding(innerPadding))
            PatientTab.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(innerPadding),
                onLogout = onLogout,
                onOpenProfile = { overlay = Overlay.PROFILE },
                onOpenWatch = { overlay = Overlay.WATCH },
                onOpenSecurity = { overlay = Overlay.SECURITY },
                onOpenHelp = { overlay = Overlay.HELP },
                onOpenTerms = { overlay = Overlay.TERMS },
                onOpenAbout = { overlay = Overlay.ABOUT }
            )
        }
    }
}
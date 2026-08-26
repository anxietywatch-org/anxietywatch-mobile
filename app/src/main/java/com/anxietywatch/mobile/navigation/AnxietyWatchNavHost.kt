package com.anxietywatch.mobile.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anxietywatch.mobile.data.remote.SessionExpiryNotifier
import com.anxietywatch.mobile.data.remote.SessionRepository
import com.anxietywatch.mobile.service.MonitoringForegroundService
import com.anxietywatch.mobile.ui.dashboard.DashboardCaregiverScreen
import com.anxietywatch.mobile.ui.dashboard.CaregiverPatientsScreen
import com.anxietywatch.mobile.ui.alerts.CriticalAlertScreen
import com.anxietywatch.mobile.ui.crisis.CrisisActiveScreen
import com.anxietywatch.mobile.ui.events.EventDetailScreen
import com.anxietywatch.mobile.ui.grounding.GroundingScreen
import com.anxietywatch.mobile.ui.relax.RelaxScreen
import com.anxietywatch.mobile.ui.relax.GuidedBreathingScreen
import com.anxietywatch.mobile.ui.home.HomePatientScreen
import com.anxietywatch.mobile.ui.home.HomeBottomNavBar
import com.anxietywatch.mobile.ui.home.HomeBottomTab
import com.anxietywatch.mobile.ui.history.PatientHistoryScreen
import com.anxietywatch.mobile.ui.onboarding.TokenEntryScreen
import com.anxietywatch.mobile.ui.permissions.PermissionsScreen
import com.anxietywatch.mobile.ui.profile.PatientProfileScreen
import com.anxietywatch.mobile.ui.splash.SplashScreen
import com.anxietywatch.mobile.ui.support.SupportGuideScreen
import com.anxietywatch.mobile.ui.settings.SettingsPatientScreen
import com.anxietywatch.mobile.ui.sounds.RelaxingSoundsScreen
import com.anxietywatch.mobile.ui.watch.ManageWatchScreen
import com.anxietywatch.mobile.ui.watch.WatchPairingScreen
import com.anxietywatch.mobile.ui.wellness.PatientDetailScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AnxietyWatchNavHost(
    sessionRepository: SessionRepository,
    sessionExpiryNotifier: SessionExpiryNotifier,
    darkModeEnabled: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showExpiredBanner by remember { mutableStateOf(false) }

    LaunchedEffect(sessionExpiryNotifier, navController) {
        sessionExpiryNotifier.events.collect {
            MonitoringForegroundService.stop(context)
            showExpiredBanner = true
            navController.navigate(Routes.TokenEntry.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.Splash.route) {
        composable(Routes.Splash.route) {
            SplashScreen {
                scope.launch {
                    val destination = if (sessionRepository.hasValidSession()) {
                        MonitoringForegroundService.start(context)
                        roleDestination(sessionRepository.roleFlow.first())
                    } else {
                        Routes.TokenEntry.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.TokenEntry.route) {
            TokenEntryScreen(
                showExpiredBanner = showExpiredBanner,
                onActivated = { role ->
                    showExpiredBanner = false
                    MonitoringForegroundService.start(context)
                    navController.navigate(permissionDestination(role)) {
                        popUpTo(Routes.TokenEntry.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.PermissionsPatient.route) {
            PermissionsScreen(
                roleLabel = "Paciente",
                onContinue = { navController.navigate(Routes.PatientProfile.route) },
            )
        }
        composable(Routes.PermissionsCaregiver.route) {
            PermissionsScreen(
                roleLabel = "Cuidador",
                onContinue = { navController.navigate(Routes.DashboardCaregiver.route) },
            )
        }

        composable(Routes.PatientProfile.route) {
            PatientProfileScreen(
                onCompleted = {
                    navController.navigate(Routes.HomePatient.route) {
                        popUpTo(Routes.PatientProfile.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HomePatient.route) {
            var selectedTab by remember { mutableStateOf(HomeBottomTab.Home) }
            Column(modifier = Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        HomeBottomTab.Home -> HomePatientScreen(
                            onRelajarmeClick = { navController.navigate(PatientExtraRoutes.Relax) },
                        )
                        HomeBottomTab.Historial -> PatientHistoryScreen()
                        HomeBottomTab.Ajustes -> SettingsPatientScreen(
                            onPersonalProfile = { navController.navigate(Routes.PatientProfile.route) },
                            onManageWatch = { navController.navigate(Routes.ManageWatch.route) },
                            darkModeEnabled = darkModeEnabled,
                            onDarkModeChange = onDarkModeChange,
                            onLogout = {
                                scope.launch {
                                    sessionRepository.clearSession()
                                    navController.navigate(Routes.TokenEntry.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            onGrounding = { navController.navigate(PatientExtraRoutes.Grounding) },
                            onRelaxingSounds = { navController.navigate(PatientExtraRoutes.RelaxingSounds) },
                        )
                    }
                }
                HomeBottomNavBar(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                )
            }
        }
        composable(Routes.DashboardCaregiver.route) {
            DashboardCaregiverScreen(
                onPatientClick = { patientId ->
                    navController.navigate(Routes.PatientDetail.build(patientId))
                },
                onViewAllPatientsClick = { navController.navigate(Routes.CaregiverPatients.route) },
            )
        }
        composable(Routes.CaregiverPatients.route) {
            CaregiverPatientsScreen(
                onPatientClick = { patientId ->
                    navController.navigate(Routes.PatientDetail.build(patientId))
                },
            )
        }

        composable(Routes.Interruption.route) { NavigationPlaceholderScreen("Validando estado") }
        composable(Routes.CrisisActive.route) {
            CrisisActiveScreen(
                onFeelingBetter = { navController.popBackStack() },
                onEndSession = {
                    navController.navigate(Routes.TokenEntry.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.History.route) { PatientHistoryScreen() }
        composable(Routes.SettingsPatient.route) {
            SettingsPatientScreen(
                onPersonalProfile = { navController.navigate(Routes.PatientProfile.route) },
                onManageWatch = { navController.navigate(Routes.ManageWatch.route) },
                darkModeEnabled = darkModeEnabled,
                onDarkModeChange = onDarkModeChange,
                onLogout = {
                    scope.launch {
                        sessionRepository.clearSession()
                        navController.navigate(Routes.TokenEntry.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onGrounding = { navController.navigate(PatientExtraRoutes.Grounding) },
                onRelaxingSounds = { navController.navigate(PatientExtraRoutes.RelaxingSounds) },
            )
        }
        composable(PatientExtraRoutes.Relax) {
            RelaxScreen(
                onBreathing = { navController.navigate(PatientExtraRoutes.Breathing) },
                onGrounding = { navController.navigate(PatientExtraRoutes.Grounding) },
                onSounds = { navController.navigate(PatientExtraRoutes.RelaxingSounds) },
            )
        }
        composable(PatientExtraRoutes.Breathing) { GuidedBreathingScreen() }
        composable(PatientExtraRoutes.Grounding) { GroundingScreen(onFinished = { navController.popBackStack() }) }
        composable(PatientExtraRoutes.RelaxingSounds) { RelaxingSoundsScreen() }
        composable(Routes.WatchPairing.route) {
            WatchPairingScreen(
                onConnected = { navController.navigate(Routes.HomePatient.route) },
                onSkip = { navController.navigate(Routes.HomePatient.route) },
            )
        }
        composable(Routes.ManageWatch.route) { ManageWatchScreen() }
        composable(Routes.PatientDetail.route) { backStackEntry ->
            PatientDetailScreen(
                patientId = backStackEntry.arguments?.getString("patientId").orEmpty(),
                onEventClick = { eventId -> navController.navigate(Routes.EventDetail.build(eventId)) },
            )
        }
        composable(Routes.CriticalAlert.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId").orEmpty()
            CriticalAlertScreen(
                eventId = eventId,
                onViewGuide = { navController.navigate(Routes.SupportGuide.build(eventId)) },
                onDismiss = { navController.popBackStack() },
            )
        }
        composable(Routes.EventDetail.route) { backStackEntry ->
            EventDetailScreen(eventId = backStackEntry.arguments?.getString("eventId").orEmpty())
        }
        composable(Routes.SupportGuide.route) { backStackEntry ->
            SupportGuideScreen(
                eventId = backStackEntry.arguments?.getString("eventId").orEmpty(),
                onFinished = { navController.popBackStack() },
            )
        }
    }
}

internal fun permissionDestination(role: String): String = when (role.lowercase()) {
    "self", "patient" -> Routes.PermissionsPatient.route
    "family_member" -> Routes.PermissionsCaregiver.route
    else -> Routes.TokenEntry.route
}

internal fun roleDestination(role: String?): String = when (role?.lowercase()) {
    "self", "patient" -> Routes.HomePatient.route
    "family_member" -> Routes.DashboardCaregiver.route
    else -> Routes.TokenEntry.route
}

@Composable
private fun NavigationPlaceholderScreen(title: String, onContinue: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (onContinue != null) {
            Button(onClick = onContinue, modifier = Modifier.padding(top = 24.dp)) {
                Text("Continuar")
            }
        }
    }
}

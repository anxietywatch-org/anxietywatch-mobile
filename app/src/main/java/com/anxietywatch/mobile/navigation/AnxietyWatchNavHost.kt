package com.anxietywatch.mobile.navigation

import android.util.Log
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.service.MonitoringForegroundService
import com.anxietywatch.mobile.push.CaregiverAlertPayload
import com.anxietywatch.mobile.push.PushTokenRegistrar
import com.anxietywatch.mobile.ui.alerts.CriticalAlertUiModel
import com.anxietywatch.mobile.ui.dashboard.DashboardCaregiverScreen
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun AnxietyWatchNavHost(
    sessionRepository: SessionRepository,
    sessionExpiryNotifier: SessionExpiryNotifier,
    api: AnxietyWatchApi,
    criticalAlertPayload: CaregiverAlertPayload? = null,
    onCriticalAlertConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showExpiredBanner by remember { mutableStateOf(false) }
    var activeCriticalAlert by remember { mutableStateOf<CaregiverAlertPayload?>(null) }

    LaunchedEffect(sessionExpiryNotifier, navController) {
        sessionExpiryNotifier.events.collect {
            MonitoringForegroundService.stop(context)
            showExpiredBanner = true
            navController.navigate(Routes.TokenEntry.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(criticalAlertPayload, navController) {
        val payload = criticalAlertPayload ?: return@LaunchedEffect
        val currentRoute = navController.currentDestination?.route ?: return@LaunchedEffect
        if (currentRoute == Routes.Splash.route) return@LaunchedEffect

        val canOpenAlert = sessionRepository.hasValidSession() &&
            sessionRepository.roleFlow.first().equals("family_member", ignoreCase = true)
        if (canOpenAlert) {
            activeCriticalAlert = payload
            navController.navigate(Routes.CriticalAlert.build(payload.eventId))
        }
        onCriticalAlertConsumed()
    }

    NavHost(navController = navController, startDestination = Routes.Splash.route) {
        composable(Routes.Splash.route) {
            SplashScreen {
                scope.launch {
                    val destination = if (sessionRepository.hasValidSession()) {
                        MonitoringForegroundService.start(context)
                        registerActivePushToken(api, scope)
                        val role = sessionRepository.roleFlow.first()
                        if (role.equals("family_member", ignoreCase = true) && criticalAlertPayload != null) {
                            activeCriticalAlert = criticalAlertPayload
                            Routes.CriticalAlert.build(criticalAlertPayload.eventId)
                        } else {
                            roleDestination(role)
                        }
                    } else {
                        Routes.TokenEntry.route
                    }
                    if (criticalAlertPayload != null) onCriticalAlertConsumed()
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
                    registerActivePushToken(api, scope)
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
            var selectedTab by rememberSaveable { mutableStateOf(HomeBottomTab.Home) }
            Column(modifier = Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        HomeBottomTab.Home -> HomePatientScreen(
                            onRelajarmeClick = { navController.navigate(PatientExtraRoutes.Relax) },
                        )
                        HomeBottomTab.Historial -> PatientHistoryScreen()
                        HomeBottomTab.Ajustes -> SettingsPatientScreen(
                            onPersonalProfile = { navController.navigate(Routes.PatientProfile.route) },
                            onManageWatch = {
                                // Restore the Settings tab when Manage Watch is popped.
                                selectedTab = HomeBottomTab.Ajustes
                                navController.navigate(Routes.ManageWatch.route)
                            },
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
                onConnected = { navController.popBackStack() },
                onSkip = { navController.popBackStack() },
            )
        }
        composable(Routes.ManageWatch.route) {
            ManageWatchScreen(
                onPairWatch = { navController.navigate(Routes.WatchPairing.route) },
            )
        }
        composable(Routes.PatientDetail.route) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId").orEmpty()
            PatientDetailScreen(
                patientId = patientId,
                onEventClick = { eventId ->
                    navController.navigate(Routes.EventDetail.build(patientId, eventId))
                },
            )
        }
        composable(Routes.CriticalAlert.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId").orEmpty()
            CriticalAlertScreen(
                eventId = eventId,
                initialAlert = activeCriticalAlert
                    ?.takeIf { it.eventId == eventId }
                    ?.let {
                        CriticalAlertUiModel(
                            patientName = it.patientName,
                            message = it.alertMessage,
                            location = it.location,
                            emergencyPhone = it.emergencyPhone,
                        )
                    },
                onViewGuide = { navController.navigate(Routes.SupportGuide.route) },
                onDismiss = {
                    activeCriticalAlert = null
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.EventDetail.route) { backStackEntry ->
            EventDetailScreen(
                patientId = backStackEntry.arguments?.getString("patientId").orEmpty(),
                eventId = backStackEntry.arguments?.getString("eventId").orEmpty(),
            )
        }
        composable(Routes.SupportGuide.route) {
            SupportGuideScreen(
                onFinished = { navController.popBackStack() },
            )
        }
    }
}

private fun registerActivePushToken(api: AnxietyWatchApi, scope: CoroutineScope) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        val token = if (task.isSuccessful) task.result?.takeIf(String::isNotBlank) else null
        if (token == null) {
            Log.e("PushRegistration", "No se pudo obtener el token FCM activo.", task.exception)
            return@addOnCompleteListener
        }
        scope.launch {
            runCatching { PushTokenRegistrar.register(api, token) }
                .onSuccess {
                    // TODO: quitar este log de diagnóstico temporal.
                    Log.d("PushRegistration", "Token activo registrado correctamente: $token")
                }
                .onFailure { error ->
                    Log.e("PushRegistration", "No se pudo registrar el token FCM activo.", error)
                }
        }
    }
}

private fun permissionDestination(role: String): String = when (role.lowercase()) {
    "self", "patient" -> Routes.PermissionsPatient.route
    "family_member" -> Routes.PermissionsCaregiver.route
    else -> Routes.TokenEntry.route
}

private fun roleDestination(role: String?): String = when (role?.lowercase()) {
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

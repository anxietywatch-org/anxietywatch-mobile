package com.anxietywatch.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anxietywatch.mobile.network.NetworkModule
import com.anxietywatch.mobile.ui.screens.CaregiverHomeScreen
import com.anxietywatch.mobile.ui.screens.MedicalInfoScreen
import com.anxietywatch.mobile.ui.screens.NotificationsScreen
import com.anxietywatch.mobile.ui.screens.PatientRootScreen
import com.anxietywatch.mobile.ui.screens.PermissionsScreen
import com.anxietywatch.mobile.ui.screens.SettingsScreen
import com.anxietywatch.mobile.ui.screens.SplashScreen
import com.anxietywatch.mobile.ui.screens.TokenEntryScreen
import com.anxietywatch.mobile.ui.screens.WelcomeScreen

object Routes {
    const val SPLASH = "splash"
    const val TOKEN_ENTRY = "token_entry"
    const val WELCOME = "welcome"
    const val PERMISSIONS = "permissions"
    const val MEDICAL_INFO = "medical_info"
    const val PATIENT_ROOT = "patient_root"
    const val CAREGIVER_HOME = "caregiver_home"
    const val CAREGIVER_SETTINGS = "caregiver_settings"
    const val NOTIFICATIONS = "notifications"
    const val WATCH_LINK = "watch_link"
    const val RELAXATION = "relaxation"
    const val BREATHING = "breathing"
    const val SECURITY = "security"
    const val HELP = "help"
    const val TERMS = "terms"
    const val ABOUT = "about"
    const val MUSIC = "music"
}

private const val ROLE_FAMILY_MEMBER = "family_member"

private fun homeRouteForRole(role: String?): String =
    if (role == ROLE_FAMILY_MEMBER) Routes.CAREGIVER_HOME else Routes.PATIENT_ROOT

private fun resumeDestination(): String {
    val session = NetworkModule.getSessionManager()
    return when {
        !session.isLoggedIn() -> Routes.TOKEN_ENTRY
        !session.hasSeenWelcome() -> Routes.WELCOME
        !session.hasGrantedPermissions() -> Routes.PERMISSIONS
        !session.hasCompletedMedicalInfo() -> Routes.MEDICAL_INFO
        !session.hasCompletedWatchStep() -> Routes.WATCH_LINK
        else -> homeRouteForRole(session.getUserRole())
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinished = { navController.navigateAndClear(resumeDestination()) })
        }

        composable(Routes.TOKEN_ENTRY) {
            TokenEntryScreen(modifier = modifier, onLinkSuccess = { navController.navigateAndClear(Routes.WELCOME) })
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                modifier = modifier,
                onContinue = {
                    NetworkModule.getSessionManager().setWelcomeSeen()
                    navController.navigateAndClear(Routes.PERMISSIONS)
                }
            )
        }

        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                modifier = modifier,
                onFinished = {
                    NetworkModule.getSessionManager().setPermissionsGranted()
                    navController.navigateAndClear(Routes.MEDICAL_INFO)
                }
            )
        }

        composable(Routes.MEDICAL_INFO) {
            MedicalInfoScreen(
                modifier = modifier,
                onFinished = { navController.navigateAndClear(Routes.WATCH_LINK) }
            )
        }

        composable(Routes.WATCH_LINK) {
            com.anxietywatch.mobile.ui.screens.WatchLinkScreen(
                modifier = modifier,
                onFinished = { navController.navigateAndClear(homeRouteForRole(NetworkModule.getSessionManager().getUserRole())) }
            )
        }

        composable(Routes.PATIENT_ROOT) {
            PatientRootScreen(
                onLogout = {
                    NetworkModule.getSessionManager().clearSession()
                    navController.navigateAndClear(Routes.TOKEN_ENTRY)
                },
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) }
            )
        }

        composable(Routes.CAREGIVER_HOME) {
            CaregiverHomeScreen(modifier = modifier, onOpenSettings = { navController.navigate(Routes.CAREGIVER_SETTINGS) })
        }

        composable(Routes.CAREGIVER_SETTINGS) {
            SettingsScreen(
                modifier = modifier,
                onLogout = {
                    NetworkModule.getSessionManager().clearSession()
                    navController.navigateAndClear(Routes.TOKEN_ENTRY)
                }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(modifier = modifier)
        }

        composable(Routes.RELAXATION) {
            com.anxietywatch.mobile.ui.screens.RelaxationScreen(
                modifier = modifier,
                onOpenBreathing = { navController.navigate(Routes.BREATHING) },
                onOpenMusic = { navController.navigate(Routes.MUSIC) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MUSIC) {
            com.anxietywatch.mobile.ui.screens.MusicScreen(
                modifier = modifier,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BREATHING) {
            com.anxietywatch.mobile.ui.screens.BreathingExerciseScreen(
                modifier = modifier,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SECURITY) {
            com.anxietywatch.mobile.ui.screens.SecurityScreen(modifier = modifier, onBack = { navController.popBackStack() })
        }
        composable(Routes.HELP) {
            com.anxietywatch.mobile.ui.screens.HelpScreen(modifier = modifier, onBack = { navController.popBackStack() })
        }
        composable(Routes.TERMS) {
            com.anxietywatch.mobile.ui.screens.TermsScreen(modifier = modifier, onBack = { navController.popBackStack() })
        }
        composable(Routes.ABOUT) {
            com.anxietywatch.mobile.ui.screens.AboutScreen(modifier = modifier, onBack = { navController.popBackStack() })
        }
    }
}

private fun NavHostController.navigateAndClear(route: String) {
    navigate(route) { popUpTo(0) { inclusive = true } }
}
package com.anxietywatch.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anxietywatch.mobile.network.NetworkModule
import com.anxietywatch.mobile.ui.screens.CaregiverHomeScreen
import com.anxietywatch.mobile.ui.screens.CaregiverWelcomeScreen
import com.anxietywatch.mobile.ui.screens.MedicalInfoScreen
import com.anxietywatch.mobile.ui.screens.NotificationsScreen
import com.anxietywatch.mobile.ui.screens.PatientRootScreen
import com.anxietywatch.mobile.ui.screens.PermissionsScreen
import com.anxietywatch.mobile.ui.screens.SettingsScreen
import com.anxietywatch.mobile.ui.screens.SplashScreen
import com.anxietywatch.mobile.ui.screens.TokenEntryScreen
import com.anxietywatch.mobile.ui.screens.WatchLinkScreen
import com.anxietywatch.mobile.ui.screens.WelcomeScreen

object Routes {
    const val SPLASH = "splash"
    const val TOKEN_ENTRY = "token_entry"
    const val WELCOME = "welcome"
    const val CAREGIVER_WELCOME = "caregiver_welcome"
    const val PERMISSIONS = "permissions"
    const val MEDICAL_INFO = "medical_info"
    const val WATCH_LINK = "watch_link"
    const val PATIENT_ROOT = "patient_root"
    const val CAREGIVER_HOME = "caregiver_home"
    const val CAREGIVER_SETTINGS = "caregiver_settings"
    const val NOTIFICATIONS = "notifications"
    const val CAREGIVER_GUIDE = "caregiver_guide"
    const val SECURITY = "security"
    const val HELP = "help"
    const val TERMS = "terms"
    const val ABOUT = "about"
    const val SESSION_EXPIRED = "session_expired"
    const val ROLE_CONFIRMATION = "role_confirmation"
}

private const val ROLE_FAMILY_MEMBER = "family_member"

private fun isCaregiverRole(role: String?): Boolean = role == ROLE_FAMILY_MEMBER
private fun homeRouteForRole(role: String?): String = if (isCaregiverRole(role)) Routes.CAREGIVER_HOME else Routes.PATIENT_ROOT

private fun resumeDestination(): String {
    val session = NetworkModule.getSessionManager()
    val caregiver = isCaregiverRole(session.getUserRole())
    if (!session.isLoggedIn() && session.consumeSessionExpiredFlag()) {
        return Routes.SESSION_EXPIRED
    }
    return when {
        !session.isLoggedIn() -> Routes.TOKEN_ENTRY
        !session.hasSeenWelcome() -> if (caregiver) Routes.CAREGIVER_WELCOME else Routes.WELCOME
        !session.hasGrantedPermissions() -> Routes.PERMISSIONS
        !caregiver && !session.hasCompletedMedicalInfo() -> Routes.MEDICAL_INFO
        !caregiver && !session.hasCompletedWatchStep() -> Routes.WATCH_LINK
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
            TokenEntryScreen(
                modifier = modifier,
                onLinkSuccess = { navController.navigateAndClear(Routes.ROLE_CONFIRMATION) }
            )
        }

        composable(Routes.ROLE_CONFIRMATION) {
            val caregiver = isCaregiverRole(NetworkModule.getSessionManager().getUserRole())
            com.anxietywatch.mobile.ui.screens.RoleConfirmationScreen(
                modifier = modifier,
                isCaregiver = caregiver,
                onContinue = { navController.navigateAndClear(if (caregiver) Routes.CAREGIVER_WELCOME else Routes.WELCOME) }
            )
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

        composable(Routes.CAREGIVER_WELCOME) {
            CaregiverWelcomeScreen(
                modifier = modifier,
                onContinue = {
                    NetworkModule.getSessionManager().setWelcomeSeen()
                    navController.navigateAndClear(Routes.PERMISSIONS)
                }
            )
        }

        composable(Routes.PERMISSIONS) {
            val caregiver = isCaregiverRole(NetworkModule.getSessionManager().getUserRole())
            PermissionsScreen(
                modifier = modifier,
                isCaregiver = caregiver,
                onFinished = {
                    NetworkModule.getSessionManager().setPermissionsGranted()
                    val destination = if (caregiver) Routes.CAREGIVER_HOME else Routes.MEDICAL_INFO
                    navController.navigateAndClear(destination)
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
            WatchLinkScreen(
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
            com.anxietywatch.mobile.ui.screens.CaregiverRootScreen(
                onLogout = {
                    NetworkModule.getSessionManager().clearSession()
                    navController.navigateAndClear(Routes.TOKEN_ENTRY)
                },
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) }
            )
        }


        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(modifier = modifier)
        }

        composable(Routes.SESSION_EXPIRED) {
            com.anxietywatch.mobile.ui.screens.SessionExpiredScreen(
                modifier = modifier,
                onContinue = { navController.navigateAndClear(Routes.TOKEN_ENTRY) }
            )
        }
    }
}

private fun NavHostController.navigateAndClear(route: String) {
    navigate(route) { popUpTo(0) { inclusive = true } }
}
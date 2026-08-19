package com.anxietywatch.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anxietywatch.mobile.network.NetworkModule
import com.anxietywatch.mobile.ui.screens.CaregiverHomeScreen
import com.anxietywatch.mobile.ui.screens.PatientHomeScreen
import com.anxietywatch.mobile.ui.screens.SplashScreen
import com.anxietywatch.mobile.ui.screens.TokenEntryScreen

object Routes {
    const val SPLASH = "splash"
    const val TOKEN_ENTRY = "token_entry"
    const val PATIENT_HOME = "patient_home"
    const val CAREGIVER_HOME = "caregiver_home"
}

private const val ROLE_PATIENT = "patient"
private const val ROLE_FAMILY_MEMBER = "family_member"

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    val sessionManager = NetworkModule.getSessionManager()
                    val destination = when {
                        !sessionManager.isLoggedIn() -> Routes.TOKEN_ENTRY
                        sessionManager.getUserRole() == ROLE_PATIENT -> Routes.PATIENT_HOME
                        sessionManager.getUserRole() == ROLE_FAMILY_MEMBER -> Routes.CAREGIVER_HOME
                        else -> Routes.TOKEN_ENTRY
                    }
                    navController.navigateAndClear(destination)
                }
            )
        }

        composable(Routes.TOKEN_ENTRY) {
            TokenEntryScreen(
                modifier = modifier,
                onLinkSuccess = {
                    val role = NetworkModule.getSessionManager().getUserRole()
                    val destination = if (role == ROLE_FAMILY_MEMBER) Routes.CAREGIVER_HOME else Routes.PATIENT_HOME
                    navController.navigateAndClear(destination)
                }
            )
        }

        composable(Routes.PATIENT_HOME) {
            PatientHomeScreen(
                modifier = modifier,
                onLogout = {
                    NetworkModule.getSessionManager().clearSession()
                    navController.navigateAndClear(Routes.TOKEN_ENTRY)
                }
            )
        }

        composable(Routes.CAREGIVER_HOME) {
            CaregiverHomeScreen(
                modifier = modifier,
                onLogout = {
                    NetworkModule.getSessionManager().clearSession()
                    navController.navigateAndClear(Routes.TOKEN_ENTRY)
                }
            )
        }
    }
}

private fun NavHostController.navigateAndClear(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
    }
}
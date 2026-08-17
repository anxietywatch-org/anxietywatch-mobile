package com.anxietywatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import com.anxietywatch.mobile.data.remote.SessionExpiryNotifier
import com.anxietywatch.mobile.data.remote.SessionRepository
import com.anxietywatch.mobile.service.MonitoringForegroundService
import com.anxietywatch.mobile.ui.home.HomeBottomNavBar
import com.anxietywatch.mobile.ui.home.HomeBottomTab
import com.anxietywatch.mobile.ui.home.HomePatientScreen
import com.anxietywatch.mobile.ui.onboarding.TokenEntryScreen
import com.anxietywatch.mobile.ui.splash.SplashScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private enum class Screen { SPLASH, TOKEN_ENTRY, HOME }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var sessionExpiryNotifier: SessionExpiryNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnxietyWatchTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf(Screen.SPLASH) }
                    var role by remember { mutableStateOf("") }
                    var showExpiredBanner by remember { mutableStateOf(false) }
                    var bottomTab by remember { mutableStateOf(HomeBottomTab.Home) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        sessionExpiryNotifier.events.collect {
                            MonitoringForegroundService.stop(this@MainActivity)
                            showExpiredBanner = true
                            screen = Screen.TOKEN_ENTRY
                        }
                    }

                    when (screen) {
                        Screen.SPLASH -> SplashScreen(
                            onFinished = {
                                scope.launch {
                                    screen = if (sessionRepository.hasValidSession()) {
                                        MonitoringForegroundService.start(this@MainActivity)
                                        Screen.HOME
                                    } else {
                                        Screen.TOKEN_ENTRY
                                    }
                                }
                            },
                        )

                        Screen.TOKEN_ENTRY -> TokenEntryScreen(
                            showExpiredBanner = showExpiredBanner,
                            onActivated = { activatedRole ->
                                role = activatedRole
                                showExpiredBanner = false
                                MonitoringForegroundService.start(this@MainActivity)
                                screen = Screen.HOME
                            },
                        )

                        // TODO: cuando bottomTab cambie a Historial/Ajustes, mostrar esas
                        // pantallas reales -- hoy solo Home tiene diseño real portado.
                        Screen.HOME -> Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                HomePatientScreen()
                            }
                            HomeBottomNavBar(selected = bottomTab, onSelect = { bottomTab = it })
                        }
                    }
                }
            }
        }
    }
}

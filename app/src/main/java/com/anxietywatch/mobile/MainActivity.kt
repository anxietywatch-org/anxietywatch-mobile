package com.anxietywatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import com.anxietywatch.mobile.data.remote.SessionExpiryNotifier
import com.anxietywatch.mobile.data.remote.SessionRepository
import com.anxietywatch.mobile.data.local.FrontendPreferencesStore
import com.anxietywatch.mobile.navigation.AnxietyWatchNavHost
import com.anxietywatch.mobile.ui.common.ScreenScaffold
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var sessionExpiryNotifier: SessionExpiryNotifier

    @Inject
    lateinit var frontendPreferences: FrontendPreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            val storedDarkMode by frontendPreferences.darkModeFlow.collectAsState(initial = null)
            var forceDarkTheme by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val darkMode = forceDarkTheme ?: storedDarkMode ?: isSystemInDarkTheme()
            AnxietyWatchTheme(forceDarkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScreenScaffold {
                    AnxietyWatchNavHost(
                        sessionRepository = sessionRepository,
                        sessionExpiryNotifier = sessionExpiryNotifier,
                        darkModeEnabled = darkMode,
                        onDarkModeChange = {
                            forceDarkTheme = it
                            scope.launch { frontendPreferences.setDarkMode(it) }
                        },
                    )
                    }
                }
            }
        }
    }
}

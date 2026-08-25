package com.anxietywatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.anxietywatch.mobile.core.theme.AnxietyWatchTheme
import com.anxietywatch.mobile.data.remote.SessionExpiryNotifier
import com.anxietywatch.mobile.data.remote.SessionRepository
import com.anxietywatch.mobile.navigation.AnxietyWatchNavHost
import com.anxietywatch.mobile.ui.common.ScreenScaffold
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
                    ScreenScaffold {
                    AnxietyWatchNavHost(
                        sessionRepository = sessionRepository,
                        sessionExpiryNotifier = sessionExpiryNotifier,
                    )
                    }
                }
            }
        }
    }
}

package com.anxietywatch.mobile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.NetworkModule
import kotlinx.coroutines.delay

private enum class BreathPhase(val label: String, val seconds: Int) {
    INHALE("Inhala", 4),
    HOLD("Mantén", 4),
    EXHALE("Exhala", 4)
}

@Composable
fun BreathingExerciseScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var isRunning by remember { mutableStateOf(false) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(BreathPhase.INHALE.seconds) }
    var cyclesCompleted by remember { mutableIntStateOf(0) }
    var sessionSaved by remember { mutableStateOf(false) }

    val phase = BreathPhase.entries[phaseIndex]
    val scale by animateFloatAsState(
        targetValue = if (phase == BreathPhase.EXHALE) 0.7f else 1.1f,
        animationSpec = tween(durationMillis = 800),
        label = "breath_scale"
    )

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            secondsLeft -= 1
            if (secondsLeft <= 0) {
                val nextIndex = (phaseIndex + 1) % BreathPhase.entries.size
                if (nextIndex == 0) cyclesCompleted += 1
                phaseIndex = nextIndex
                secondsLeft = BreathPhase.entries[nextIndex].seconds
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver")
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Respiración guiada", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
            Text(
                text = "Ciclos completados: $cyclesCompleted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 40.dp)
            )

            Box(
                modifier = Modifier
                    .size((180 * scale).dp)
                    .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = phase.label, style = MaterialTheme.typography.headlineMedium)
                    Text(text = "${secondsLeft}s", style = MaterialTheme.typography.displaySmall)
                }
            }

            Button(
                onClick = { isRunning = !isRunning },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 60.dp)
            ) {
                Text(if (isRunning) "Pausar" else "Comenzar")
            }

            if (cyclesCompleted > 0) {
                Button(
                    onClick = {
                        isRunning = false
                        NetworkModule.getSessionManager().recordBreathingSessionCompleted()
                        sessionSaved = true
                    },
                    enabled = !sessionSaved,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text(if (sessionSaved) "Sesión guardada ✓" else "Finalizar y guardar sesión")
                }
            }
        }
    }
}
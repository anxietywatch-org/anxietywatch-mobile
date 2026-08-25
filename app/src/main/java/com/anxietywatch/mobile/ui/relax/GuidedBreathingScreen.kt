package com.anxietywatch.mobile.ui.relax

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private enum class BreathPhase(val label: String, val seconds: Int) {
    Inhale("Inhala", 4),
    Hold("Sostén", 2),
    Exhale("Exhala", 6),
}

@Composable
fun GuidedBreathingScreen() {
    var running by remember { mutableStateOf(false) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(BreathPhase.Inhale.seconds) }
    var cycles by remember { mutableIntStateOf(0) }
    val phase = BreathPhase.entries[phaseIndex]
    val targetScale = when (phase) {
        BreathPhase.Inhale -> 1.12f
        BreathPhase.Hold -> 1.12f
        BreathPhase.Exhale -> 0.82f
    }
    val circleScale by animateFloatAsState(
        targetValue = if (running) targetScale else 1f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "breathing-circle",
    )

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            if (remaining > 1) {
                remaining--
            } else {
                if (phaseIndex == BreathPhase.Exhale.ordinal) cycles++
                phaseIndex = (phaseIndex + 1) % BreathPhase.entries.size
                remaining = BreathPhase.entries[phaseIndex].seconds
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Respiración guiada", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Sigue el círculo sin forzar la respiración.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .scale(circleScale)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (running) phase.label else "Listo", style = MaterialTheme.typography.headlineSmall)
                if (running) Text("$remaining s", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Ciclos completados: $cycles", style = MaterialTheme.typography.titleMedium)
        Text(
            if (running) "Siguiente fase en $remaining segundos" else "Inhala 4 · sostén 2 · exhala 6",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (running) {
                OutlinedButton(onClick = { running = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Detener")
                }
            } else {
                Button(
                    onClick = {
                        phaseIndex = 0
                        remaining = BreathPhase.Inhale.seconds
                        running = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Comenzar") }
            }
        }
    }
}

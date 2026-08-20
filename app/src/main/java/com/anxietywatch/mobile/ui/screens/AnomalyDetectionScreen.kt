package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.NetworkModule
import kotlinx.coroutines.delay

private const val ESCALATION_SECONDS = 30

@Composable
fun AnomalyDetectionScreen(
    modifier: Modifier = Modifier,
    onConfirmActive: () -> Unit,
    onNeedHelp: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(ESCALATION_SECONDS) }
    var resolved by remember { mutableIntStateOf(0) } // 0 = pendiente, evita doble navegación

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && resolved == 0) {
            delay(1000)
            secondsLeft -= 1
        }
        if (resolved == 0) {
            resolved = 1
            onNeedHelp()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Detectamos algo inusual",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tu ritmo cardíaco está más alto de lo habitual. ¿Cómo te encuentras?",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = "Si no respondes en $secondsLeft s, activaremos el apoyo de crisis automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }

        Button(
            onClick = {
                if (resolved == 0) {
                    resolved = 1
                    NetworkModule.getSessionManager().recordFalsePositive()
                    NetworkModule.getSessionManager().setAnomalyPending(false)
                    onConfirmActive()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
        ) {
            Text("Sí, estoy activo")
        }

        Button(
            onClick = {
                if (resolved == 0) {
                    resolved = 1
                    onNeedHelp()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("No, necesito ayuda")
        }
    }
}
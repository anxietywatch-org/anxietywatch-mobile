package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.NetworkModule

private data class GroundingStep(val count: String, val sense: String, val instruction: String)

private val GROUNDING_STEPS = listOf(
    GroundingStep("5", "cosas que puedes ver", "Mira a tu alrededor y nombra 5 cosas que veas, en silencio o en voz alta."),
    GroundingStep("4", "cosas que puedes tocar", "Nota 4 cosas que puedas tocar: la textura de tu ropa, una superficie, tu propia piel."),
    GroundingStep("3", "cosas que puedes oír", "Identifica 3 sonidos a tu alrededor, cercanos o lejanos."),
    GroundingStep("2", "cosas que puedes oler", "Nota 2 olores presentes, o recuerda 2 olores que te resulten familiares."),
    GroundingStep("1", "cosa que puedes saborear", "Nota 1 sabor en tu boca, o recuerda uno que te guste.")
)

@Composable
fun GroundingScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    val step = GROUNDING_STEPS.getOrNull(stepIndex)

    Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver") }
            Text(text = "Grounding", style = MaterialTheme.typography.headlineSmall)
        }

        if (finished || step == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Text(text = "¡Bien hecho!", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
                Text(
                    text = "Completaste el ejercicio de grounding. Tómate un momento antes de continuar.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                ) {
                    Text("Volver a Relajarme")
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                    for (i in GROUNDING_STEPS.indices) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (i == stepIndex) 10.dp else 8.dp)
                                .background(
                                    if (i <= stepIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    CircleShape
                                )
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = step.count, style = MaterialTheme.typography.displaySmall)
                        Text(text = step.sense, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            text = step.instruction,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                    if (stepIndex > 0) {
                        TextButton(
                            onClick = { stepIndex -= 1 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Atrás")
                        }
                    }
                    Button(
                        onClick = {
                            if (stepIndex < GROUNDING_STEPS.lastIndex) {
                                stepIndex += 1
                            } else {
                                NetworkModule.getSessionManager().recordGroundingSessionCompleted()
                                finished = true
                            }
                        },
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (stepIndex < GROUNDING_STEPS.lastIndex) "Siguiente" else "Finalizar")
                    }
                }
            }
        }
    }
}
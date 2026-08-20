package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.NetworkModule

@Composable
fun DetectionSettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val session = NetworkModule.getSessionManager()

    var thresholdValue by remember {
        mutableFloatStateOf(
            session.getAnomalyThresholdBpm().toFloat()
        )
    }

    var isPaused by remember {
        mutableStateOf(
            session.isDetectionCurrentlyPaused()
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {

        // Barra superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Volver"
                )
            }

            Text(
                text = "Sensibilidad de detección",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

            // ==========================================
            // UMBRAL DE BPM
            // ==========================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Umbral de BPM para alertar",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Se activará una detección si tu ritmo cardíaco supera ${thresholdValue.toInt()} BPM en reposo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            top = 4.dp,
                            bottom = 12.dp
                        )
                    )

                    Slider(
                        value = thresholdValue,
                        onValueChange = {
                            thresholdValue = it
                            session.setAnomalyThresholdBpm(it.toInt())
                        },
                        valueRange = 90f..160f,
                        steps = 13
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "90 (más sensible)",
                            style = MaterialTheme.typography.labelSmall
                        )

                        Text(
                            text = "160 (menos sensible)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // ==========================================
            // PAUSAR DETECCIÓN
            // ==========================================

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Pausar detección temporalmente",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = if (isPaused) {
                            "La detección está pausada por 2 horas. Útil durante ejercicio físico intenso."
                        } else {
                            "Pausa la detección por 2 horas, por ejemplo durante ejercicio, para evitar falsas alertas."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            top = 4.dp,
                            bottom = 12.dp
                        )
                    )

                    Button(
                        onClick = {
                            if (isPaused) {

                                // Reanudar detección
                                session.setDetectionPaused(
                                    false,
                                    0
                                )

                                isPaused = false

                            } else {

                                // Pausar durante 2 horas
                                val until =
                                    System.currentTimeMillis() +
                                            (2 * 60 * 60 * 1000L)

                                session.setDetectionPaused(
                                    true,
                                    until
                                )

                                isPaused = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaused) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isPaused) {
                                "Reanudar detección ahora"
                            } else {
                                "Pausar por 2 horas"
                            }
                        )
                    }
                }
            }
        }
    }
}
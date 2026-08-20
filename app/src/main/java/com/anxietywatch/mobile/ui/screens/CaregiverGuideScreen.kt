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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class GuideStep(val title: String, val body: String)

private val GUIDE_STEPS = listOf(
    GuideStep("Mantén la calma", "Tu tranquilidad es la mejor herramienta. Respira profundo antes de intervenir — la persona percibe tu estado emocional, y tu calma ayuda a regular la suya."),
    GuideStep("Qué decir", "\"Estoy aquí contigo. Estás a salvo. Esto va a pasar.\" Evita decir \"cálmate\" — no ayuda y puede aumentar la ansiedad. Habla con voz suave y constante."),
    GuideStep("Asegura el entorno", "Retira objetos que puedan representar un riesgo, reduce ruidos fuertes y luces intensas. Crea un espacio de \"baja presión\" a su alrededor."),
    GuideStep("Técnica de respiración guiada", "Respira tú también, en voz alta, para que la persona pueda imitarte: inhala 4 segundos, mantén 4 segundos, exhala 4 segundos. Repite el ciclo con ella."),
    GuideStep("Cuándo buscar ayuda profesional", "Si la crisis no cede después de varios minutos, si hay riesgo físico, o si la persona lo solicita, no dudes en contactar servicios de emergencia de tu localidad."),
    GuideStep("Cuida también tu propio bienestar", "Acompañar a alguien con ansiedad puede ser agotador. Está bien pedir apoyo, tomarte descansos, y hablar con alguien de confianza sobre cómo te sientes tú.")
)

@Composable
fun CaregiverGuideScreenBody(modifier: Modifier = Modifier, onFinished: () -> Unit = {}) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = GUIDE_STEPS[stepIndex]

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Guía de Apoyo", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Paso ${stepIndex + 1} de ${GUIDE_STEPS.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            for (i in GUIDE_STEPS.indices) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .size(6.dp)
                        .background(
                            if (i <= stepIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = step.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = step.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            if (stepIndex > 0) {
                TextButton(onClick = { stepIndex -= 1 }, modifier = Modifier.weight(1f)) {
                    Text("Atrás")
                }
            }
            Button(
                onClick = {
                    if (stepIndex < GUIDE_STEPS.lastIndex) stepIndex += 1 else onFinished()
                },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (stepIndex < GUIDE_STEPS.lastIndex) "Siguiente" else "Finalizar")
            }
        }
    }
}
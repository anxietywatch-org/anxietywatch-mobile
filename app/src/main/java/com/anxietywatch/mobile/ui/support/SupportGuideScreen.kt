package com.anxietywatch.mobile.ui.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class SupportStep(
    val title: String,
    val message: String,
    val suggestedPhrase: String,
)

@Composable
fun SupportGuideScreen(eventId: String, onFinished: () -> Unit) {
    val steps = remember {
        listOf(
            SupportStep(
                "Mantén la calma",
                "Tu tranquilidad es la mejor herramienta. Respira profundo antes de intervenir.",
                "Estoy aquí, estás seguro, vamos a superar esto juntos.",
            ),
            SupportStep(
                "Acércate con suavidad",
                "Habla despacio y evita hacer muchas preguntas. Dale espacio para responder.",
                "No tienes que resolverlo todo ahora. Estoy contigo.",
            ),
            SupportStep(
                "Acompaña la respiración",
                "Invítale a seguir un ritmo lento y permanece cerca hasta que se sienta estable.",
                "Vamos paso a paso. Respiremos juntos.",
            ),
        )
    }
    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps[currentStep]

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Guía de apoyo", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Acompañamiento para este momento",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 24.dp)) {
            steps.indices.forEach { index ->
                Surface(
                    color = if (index == currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(if (index == currentStep) 28.dp else 8.dp, 8.dp),
                ) {}
            }
        }
        Card(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 24.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.size(112.dp),
                ) {
                    Icon(
                        Icons.Default.SelfImprovement,
                        contentDescription = "Persona meditando",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(28.dp),
                    )
                }
                Text(step.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 24.dp))
                Text(
                    step.message,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                ) {
                    Text(
                        "\"${step.suggestedPhrase}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        }
        Button(
            onClick = {
                if (currentStep == steps.lastIndex) onFinished() else currentStep++
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (currentStep == steps.lastIndex) "Entendido" else "Siguiente")
        }
        Text("Evento: $eventId", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
    }
}

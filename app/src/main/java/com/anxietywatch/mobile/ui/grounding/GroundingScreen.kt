package com.anxietywatch.mobile.ui.grounding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class GroundingStep(val sense: String, val prompt: String, val icon: ImageVector)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GroundingScreen(onFinished: () -> Unit) {
    val steps = remember {
        listOf(
            GroundingStep("5 cosas que ves", "Mira a tu alrededor y nombra cinco cosas que puedas ver.", Icons.Default.Visibility),
            GroundingStep("4 cosas que oyes", "Escucha con atención y reconoce cuatro sonidos.", Icons.Default.Visibility),
            GroundingStep("3 cosas que tocas", "Nota tres texturas o sensaciones en tus manos.", Icons.Default.Visibility),
            GroundingStep("2 cosas que hueles", "Identifica dos aromas a tu alrededor.", Icons.Default.Visibility),
            GroundingStep("1 cosa que saboreas", "Concéntrate en un sabor y en tu respiración.", Icons.Default.Visibility),
        )
    }
    var index by remember { mutableIntStateOf(0) }
    var finished by remember { mutableIntStateOf(0) }
    val pulse by rememberInfiniteTransition(label = "grounding").animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "grounding-pulse",
    )

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ejercicio de grounding", style = MaterialTheme.typography.headlineLarge)
        Text("5-4-3-2-1", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 24.dp)) {
            steps.indices.forEach { step ->
                androidx.compose.material3.Surface(
                    color = if (step <= index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                    modifier = Modifier.size(10.dp),
                ) {}
            }
        }
        if (finished == 1) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("¡Bien hecho!", style = MaterialTheme.typography.headlineLarge)
                Text("Has completado el ejercicio. Quédate con este momento de calma.", modifier = Modifier.padding(top = 12.dp))
            }
        } else AnimatedContent(targetState = index, label = "grounding-step", modifier = Modifier.weight(1f)) { current ->
            val step = steps[current]
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(132.dp).scale(pulse),
                ) {
                    androidx.compose.material3.Icon(step.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(42.dp))
                }
                Text(step.sense, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 28.dp))
                Text(step.prompt, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { if (index > 0) index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("Atrás") }
            Button(
                onClick = { if (index == steps.lastIndex) finished = 1 else index++ },
                modifier = Modifier.weight(1f),
            ) { Text(if (index == steps.lastIndex) "Finalizar" else "Siguiente") }
        }
        if (finished == 1) Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
    }
}

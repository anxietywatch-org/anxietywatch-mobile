package com.anxietywatch.mobile.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Splash con fundido suave (nada de animaciones bruscas -- coherente con los principios
 * de UX/UI del PDF: "reducir la sobrecarga sensorial"). [onFinished] navega a la siguiente
 * pantalla segun DataStore: sesion activa -> Home; sin sesion -> Ingreso por Token (E01/E02).
 *
 * Fondo con primaryContainer (mas claro que primary) para que la marca se note sin
 * ambiguedad, en vez de depender solo del color del texto sobre blanco/negro -- el tono
 * cafe real de la paleta (#6A5C46) es bastante oscuro y a tamano pequeno se puede
 * confundir con negro puro.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var alpha by remember { mutableFloatStateOf(0f) }
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "splash-fade",
    )

    LaunchedEffect(Unit) {
        alpha = 1f
        delay(1200) // tiempo minimo de marca; el chequeo real de sesion ocurre en el ViewModel
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(animatedAlpha),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = "AnxietyWatch",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .width(280.dp),
            )
        }
    }
}

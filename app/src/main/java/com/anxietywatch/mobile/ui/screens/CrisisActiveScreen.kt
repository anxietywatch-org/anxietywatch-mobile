package com.anxietywatch.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.EmergencyNotifier
import com.anxietywatch.mobile.network.NetworkModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MIN_SECONDS_BEFORE_END = 15

@Composable
fun CrisisActiveScreen(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showConfirmEnd by remember { mutableStateOf(false) }
    var emergencyPhone by remember { mutableStateOf<String?>(null) }
    var smsSent by remember { mutableStateOf(false) }
    var sosSent by remember { mutableStateOf(false) }

    val canEnd = elapsedSeconds >= MIN_SECONDS_BEFORE_END

    LaunchedEffect(Unit) {
        // Aviso real al servidor de que hay una crisis (dispara CaregiverAlertDispatcher real).
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-device"
        sosSent = EmergencyNotifier.triggerRealSos(deviceId)

        // Aviso real por SMS al contacto de emergencia, si hay permiso y número guardado.
        val fullName = NetworkModule.getSessionManager().getFullName() ?: "Un paciente"
        smsSent = EmergencyNotifier.notifyEmergencyContactBySms(context, fullName)

        try {
            val profile = NetworkModule.api.getProfile()
            emergencyPhone = profile.emergencyContactPhone?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            // Sin conexión no bloqueamos la pantalla de crisis.
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "breathing_scale"
    )

    if (showConfirmEnd) {
        AlertDialog(
            onDismissRequest = { showConfirmEnd = false },
            title = { Text("¿Terminar sesión de crisis?") },
            text = { Text("Confirma que ya te sientes mejor y quieres finalizar el apoyo.") },
            confirmButton = {
                TextButton(onClick = {
                    NetworkModule.getSessionManager().recordCrisisEvent(elapsedSeconds)
                    onFinished()
                }) { Text("Sí, terminar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEnd = false }) { Text("Continuar") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A3A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Respira conmigo",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 48.dp)
        )

        Box(
            modifier = Modifier
                .padding(top = 32.dp)
                .size((160 * scale).dp)
                .background(Color(0xFFF3E9DC), CircleShape)
        )

        Text(
            text = "No estás solo. Vamos a superar esto juntos, un respiro a la vez.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 32.dp)
        )

        if (smsSent) {
            Text(
                text = "✓ Se avisó por SMS a tu contacto de emergencia.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB8E0C4),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Button(
            onClick = {
                val phone = emergencyPhone
                if (phone != null) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) {
            Text(if (emergencyPhone != null) "Contactar ayuda" else "Contactar ayuda (agrega un contacto en tu Perfil)")
        }

        Button(
            onClick = { if (canEnd) showConfirmEnd = true },
            enabled = canEnd,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3E9DC), contentColor = Color(0xFF1B2A3A)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text(if (canEnd) "Terminar sesión" else "Terminar sesión (disponible en ${MIN_SECONDS_BEFORE_END - elapsedSeconds}s)")
        }
    }
}
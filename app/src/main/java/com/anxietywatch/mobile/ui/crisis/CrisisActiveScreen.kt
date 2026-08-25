package com.anxietywatch.mobile.ui.crisis

import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CrisisActiveScreen(
    onFeelingBetter: () -> Unit,
    onEndSession: () -> Unit,
    viewModel: CrisisActiveViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val profileState by viewModel.profile.collectAsState()
    val emergencyPhone = (profileState as? CrisisProfileUiState.Loaded)?.profile?.emergencyContactPhone
    var smsMessage by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendEmergencySms(context, emergencyPhone, onFailure = { smsMessage = it })
        else smsMessage = "Permiso de SMS rechazado. Puedes reintentarlo desde esta pantalla."
    }
    val transition = rememberInfiniteTransition(label = "crisis-breathing")
    val breathProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "breath-progress",
    )
    val breathingText = if (breathProgress < 0.5f) "Inhala" else "Exhala"

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Toma un momento para respirar con nosotros",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            "Estás a salvo. Sigue el ritmo del círculo y permite que tu cuerpo se calme poco a poco.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 32.dp).size(220.dp),
        ) {
            Box(
                modifier = Modifier.size(180.dp).scale(0.86f + breathProgress * 0.14f)
                    .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
            )
            Box(
                modifier = Modifier.size(132.dp).background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(breathingText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        Button(
            onClick = {
                // ACTION_DIAL abre el marcador; nunca realiza una llamada automática.
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Contactar ayuda inmediata") }
        Button(
            onClick = {
                if (emergencyPhone.isNullOrBlank()) {
                    smsMessage = "No hay un contacto de emergencia guardado en tu perfil."
                } else if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    sendEmergencySms(context, emergencyPhone, onFailure = { smsMessage = it })
                } else {
                    smsPermission.launch(android.Manifest.permission.SEND_SMS)
                }
            },
            enabled = !emergencyPhone.isNullOrBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Enviar SMS al contacto de emergencia") }
        if (emergencyPhone.isNullOrBlank()) {
            Text("Configura un contacto de emergencia para habilitar el SMS.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
        OutlinedButton(onClick = onFeelingBetter, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Me siento mejor ahora")
        }
        Text(
            "Terminar sesión",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 18.dp),
        )
        OutlinedButton(onClick = onEndSession, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("Terminar sesión")
        }
        Spacer(Modifier.size(20.dp))
        Text(
            "Protocolo de crisis activo — estamos contigo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    if (smsMessage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { smsMessage = null },
            text = { Text(smsMessage.orEmpty()) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { smsMessage = null }) { Text("Aceptar") } },
        )
    }
}

private fun sendEmergencySms(context: android.content.Context, phone: String?, onFailure: (String) -> Unit) {
    if (phone.isNullOrBlank()) return
    try {
        android.telephony.SmsManager.getDefault().sendTextMessage(
            phone,
            null,
            "AnxietyWatch: necesito apoyo. Estoy en una sesión de crisis activa.",
            null,
            null,
        )
    } catch (_: SecurityException) {
        onFailure("No se pudo enviar el SMS. Revisa el permiso e inténtalo de nuevo.")
    }
}

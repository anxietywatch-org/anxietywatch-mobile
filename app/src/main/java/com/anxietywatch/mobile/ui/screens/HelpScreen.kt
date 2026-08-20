package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val FAQ_ITEMS = listOf(
    "¿Cómo vinculo mi reloj?" to "Ve a Ajustes → Vincular Reloj. Asegúrate de haber emparejado tu Galaxy Watch7 desde el Bluetooth del sistema primero.",
    "¿Qué pasa si no tengo reloj conectado?" to "La app sigue funcionando: puedes usar ejercicios de respiración, registrar tu información médica y ver tu historial. El monitoreo de BPM en vivo requiere un reloj vinculado.",
    "¿Cómo cambio mi información médica?" to "Ve a Ajustes → Perfil Personal. Ahí puedes editar todos tus datos y guardar los cambios en cualquier momento.",
    "¿Mi cuidador ve todos mis datos?" to "Tu cuidador ve tu resumen de bienestar y recibe alertas de crisis. Puedes revisar exactamente qué comparte en Seguridad y Privacidad.",
    "¿Cómo elimino mi cuenta?" to "Por ahora, contacta directamente al equipo de soporte para solicitar la eliminación de tu cuenta y datos."
)

@Composable
fun HelpScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver") }
            Text(text = "Centro de Ayuda", style = MaterialTheme.typography.headlineSmall)
        }

        for ((question, answer) in FAQ_ITEMS) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = question, style = MaterialTheme.typography.titleMedium)
                    Text(text = answer, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}
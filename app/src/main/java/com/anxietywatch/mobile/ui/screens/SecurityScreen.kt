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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SecurityScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver") }
            Text(text = "Seguridad y Privacidad", style = MaterialTheme.typography.headlineSmall)
        }

        InfoCard(
            icon = Icons.Filled.Lock,
            title = "Cifrado de tu información",
            body = "Tu sesión y tus datos médicos locales (edad, contacto de emergencia, hábitos) se guardan cifrados en tu dispositivo con AES-256, usando el Android Keystore. Nadie puede leerlos sin desbloquear tu teléfono."
        )
        InfoCard(
            icon = Icons.Filled.Shield,
            title = "Conexión segura con el servidor",
            body = "Toda comunicación con nuestros servidores viaja cifrada mediante HTTPS/TLS. Tu token de sesión nunca se comparte con terceros ni se guarda en texto plano."
        )
        InfoCard(
            icon = Icons.Filled.VisibilityOff,
            title = "Qué comparte tu cuidador",
            body = "Solo tu cuidador vinculado puede ver el resumen de tu bienestar y recibir alertas si ocurre una crisis. Nunca vendemos ni compartimos tu información con anunciantes."
        )
        InfoCard(
            icon = Icons.Filled.Lock,
            title = "Control sobre tus permisos",
            body = "Puedes revocar en cualquier momento los permisos de Bluetooth, cámara, contactos o notificaciones desde los ajustes de Android — la app seguirá funcionando, aunque algunas funciones se limitarán."
        )
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
            }
            Text(text = body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
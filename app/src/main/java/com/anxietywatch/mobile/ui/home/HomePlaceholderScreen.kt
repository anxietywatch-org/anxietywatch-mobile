package com.anxietywatch.mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * PLACEHOLDER honesto: esto NO es la pantalla Home real de E06 (estado biometrico,
 * respiracion, etc. -- ver DESIGN.md del Stitch para el diseño real que falta portar).
 * Existe solo para confirmar que, tras activarse con el token, el
 * MonitoringForegroundService ya esta corriendo (revisa la notificacion persistente
 * "Monitoreo de bienestar activo" en la barra de estado del emulador/telefono).
 */
@Composable
fun HomePlaceholderScreen(role: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sesión activa", style = MaterialTheme.typography.headlineLarge)
        Text("Rol: $role", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Revisa la notificación persistente: el monitoreo en segundo plano ya está corriendo.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )
        Button(onClick = onLogout) { Text("Cerrar sesión (prueba)") }
    }
}

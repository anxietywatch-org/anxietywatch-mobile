package com.anxietywatch.mobile.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsPatientScreen(
    onPersonalProfile: () -> Unit,
    onManageWatch: () -> Unit,
    onLogout: () -> Unit,
    darkModeEnabled: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    onGrounding: () -> Unit = {},
    onRelaxingSounds: () -> Unit = {},
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var caregiverDialogVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineLarge)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Avatar", modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Mi perfil", style = MaterialTheme.typography.titleLarge)
                    Text("Paciente", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SettingsSectionTitle("PREFERENCIAS DE SISTEMA")
        Card(modifier = Modifier.fillMaxWidth()) {
            ToggleRow("Notificaciones", "Alertas y recordatorios", notificationsEnabled) { notificationsEnabled = it }
            HorizontalDivider()
            ToggleRow("Sonido", "Sonidos de la aplicación", soundEnabled) { soundEnabled = it }
            HorizontalDivider()
            ToggleRow("Modo oscuro", "Reduce la fatiga visual", darkModeEnabled, onDarkModeChange)
        }

        SettingsSectionTitle("CUENTA")
        Card(modifier = Modifier.fillMaxWidth()) {
            SettingsRow(Icons.Default.AccountCircle, "Perfil Personal", onPersonalProfile)
            HorizontalDivider()
            SettingsRow(Icons.Default.Watch, "Gestionar Reloj", onManageWatch)
            HorizontalDivider()
            SettingsRow(Icons.Default.AccountCircle, "Mi cuidador") { caregiverDialogVisible = true }
        }

        SettingsSectionTitle("BIENESTAR")
        Card(modifier = Modifier.fillMaxWidth()) {
            SettingsRow(Icons.Default.SelfImprovement, "Ejercicio de grounding", onGrounding)
            HorizontalDivider()
            SettingsRow(Icons.Default.MusicNote, "Sonidos relajantes", onRelaxingSounds)
        }

        SettingsSectionTitle("SOPORTE")
        Card(modifier = Modifier.fillMaxWidth()) {
            SettingsRow(Icons.Default.HelpOutline, "Centro de Ayuda") {
                // TODO: conectar al centro de ayuda cuando exista contenido confirmado.
            }
            HorizontalDivider()
            SettingsRow(Icons.Default.Security, "Términos de Servicio") {
                // TODO: conectar a los términos publicados cuando exista una ruta confirmada.
            }
        }

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Cerrar sesión", modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            "AnxietyWatch v0.1.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp, bottom = 24.dp),
        )
    }

    if (caregiverDialogVisible) {
        AlertDialog(
            onDismissRequest = { caregiverDialogVisible = false },
            title = { Text("Mi cuidador") },
            text = { Text("Esta relación estará disponible próximamente cuando el backend confirme el endpoint correspondiente.") },
            confirmButton = { TextButton(onClick = { caregiverDialogVisible = false }) { Text("Aceptar") } },
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, modifier = Modifier.weight(1f).padding(start = 14.dp), color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

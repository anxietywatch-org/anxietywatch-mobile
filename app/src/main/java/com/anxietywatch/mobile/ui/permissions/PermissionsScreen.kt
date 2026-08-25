package com.anxietywatch.mobile.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class PermissionItem(val title: String, val explanation: String)

private val permissionItems = listOf(
    PermissionItem("Ubicación", "Permite compartir la ubicación durante una alerta crítica."),
    PermissionItem("Datos móviles", "Mantiene la sincronización cuando no hay Wi-Fi."),
    PermissionItem("Bluetooth", "Conecta el teléfono con tu AnxietyWatch."),
    PermissionItem("Archivos y almacenamiento", "Guarda datos cifrados para sincronizar después."),
    PermissionItem("Notificaciones", "Te avisa sobre cambios de estado y alertas importantes."),
)

@Composable
fun PermissionsScreen(roleLabel: String, onContinue: () -> Unit) {
    var enabled by remember { mutableStateOf(permissionItems.associate { it.title to false }) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Text("Permisos del sistema", style = MaterialTheme.typography.headlineLarge)
        Text(
            "AnxietyWatch necesita estos permisos para proteger tu información y mantener " +
                "la conexión con el reloj.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(24.dp))

        permissionItems.forEach { item ->
            PermissionRow(
                item = item,
                checked = enabled[item.title] == true,
                onCheckedChange = { checked -> enabled = enabled + (item.title to checked) },
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text("Continuar")
        }
    }
}

@Composable
private fun PermissionRow(item: PermissionItem, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(
                item.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

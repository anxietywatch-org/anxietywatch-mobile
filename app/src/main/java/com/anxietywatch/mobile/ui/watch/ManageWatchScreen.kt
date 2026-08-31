package com.anxietywatch.mobile.ui.watch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.common.ConnectivityCard
import com.anxietywatch.mobile.ui.common.ConnectivityStatus
import com.anxietywatch.mobile.ui.common.SectionHeader

@Composable
fun ManageWatchScreen(
    onPairWatch: () -> Unit = {},
    viewModel: ManageWatchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showDisconnectDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        SectionHeader(eyebrow = "DISPOSITIVO", title = "Gestionar reloj")
        ConnectivityCard(
            status = if (state.connected) ConnectivityStatus.ConnectedRecent else ConnectivityStatus.Disconnected,
            deviceName = state.deviceName,
            lastSync = state.lastSync,
        )
        if (state.pairingStored) {
            Text(
                "Reloj vinculado",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Button(
                onClick = onPairWatch,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text("Vincular reloj")
            }
        }
        if (!state.pairingStored) {
            Button(
                onClick = onPairWatch,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text("Vincular reloj")
            }
        }
        SectionTitle("Sincronización")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(
                    Icons.Default.Bluetooth,
                    "Conexión",
                    if (state.connected) "Conectado" else "Desconectado",
                )
                InfoRow(Icons.Default.Sync, "Última sincronización", state.lastSync)
                Button(
                    onClick = viewModel::forceSync,
                    enabled = !state.refreshing,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(if (state.refreshing) "Actualizando..." else "Forzar Sincronización")
                }
            }
        }
        SectionTitle("Ajustes del Dispositivo")
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notificaciones Hápticas", style = MaterialTheme.typography.titleMedium)
                    Text("Vibraciones para avisos del reloj", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = state.hapticNotifications,
                    onCheckedChange = viewModel::setHapticNotifications,
                )
            }
            TextButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lectura de Pulso")
                    Text("Monitoreo continuo")
                }
            }
        }
        Spacer(Modifier.size(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth().border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                RoundedCornerShape(12.dp),
            ).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Zona de Gestión Crítica", modifier = Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.error)
            }
            Text(
                "Desconectar detendrá el monitoreo en tiempo real y requerirá volver a vincular el reloj.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            TextButton(onClick = { showDisconnectDialog = true }) {
                Text("Desconectar Reloj", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("¿Desconectar reloj?") },
            text = { Text("Perderás el monitoreo en tiempo real hasta volver a vincularlo.") },
            confirmButton = {
                TextButton(onClick = { viewModel.disconnect(); showDisconnectDialog = false }) {
                    Text("Desconectar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDisconnectDialog = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(label, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

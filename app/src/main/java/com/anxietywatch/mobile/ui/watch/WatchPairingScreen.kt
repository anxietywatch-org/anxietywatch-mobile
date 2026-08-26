package com.anxietywatch.mobile.ui.watch

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun WatchPairingScreen(
    onConnected: () -> Unit,
    onSkip: () -> Unit,
    viewModel: WatchPairingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selected by remember { mutableStateOf<NearbyWatch?>(null) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    val devices = (state as? WatchPairingUiState.Ready)?.devices.orEmpty()

    LaunchedEffect(state) {
        if (state is WatchPairingUiState.Paired) onConnected()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Vincular reloj", style = MaterialTheme.typography.headlineLarge)
        Text("Conecta tu AnxietyWatch para iniciar el monitoreo.", modifier = Modifier.padding(top = 8.dp))
        PairingAnimation()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Buscando dispositivos...", modifier = Modifier.weight(1f))
            IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, "Actualizar") }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(devices) { device ->
                Card(
                    onClick = { selected = device },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Watch, contentDescription = null, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(device.name, style = MaterialTheme.typography.titleMedium)
                            Text("${device.distance} · ${device.signal}", style = MaterialTheme.typography.bodySmall)
                        }
                        RadioButton(selected = selected == device, onClick = { selected = device })
                    }
                }
            }
        }
        Button(
            onClick = {
                selected?.let { device ->
                    if (viewModel.hasExistingPairing()) showReplaceDialog = true
                    else viewModel.pairSelected(device)
                }
            },
            enabled = selected != null && state !is WatchPairingUiState.Pairing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state is WatchPairingUiState.Pairing) "Vinculando..." else "Conectar dispositivo")
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Conectar después") }
        Text(
            "Asegúrate de que tu reloj esté en modo de emparejamiento y cerca del teléfono.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("¿Reemplazar reloj vinculado?") },
            text = { Text("El reloj actual dejará de estar vinculado a este teléfono.") },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceDialog = false
                    selected?.let { viewModel.pairSelected(it, replaceExisting = true) }
                }) { Text("Reemplazar") }
            },
            dismissButton = { TextButton(onClick = { showReplaceDialog = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PairingAnimation() {
    val transition = rememberInfiniteTransition(label = "pairing")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pairing-alpha",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp).alpha(alpha),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.PhoneAndroid, contentDescription = "Teléfono", modifier = Modifier.size(48.dp))
        Box(modifier = Modifier.padding(horizontal = 24.dp).size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Icon(Icons.Default.Watch, contentDescription = "Reloj", modifier = Modifier.size(48.dp))
    }
}

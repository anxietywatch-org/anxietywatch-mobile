package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.anxietywatch.mobile.network.NetworkModule

@Composable
fun LinkedCaregiverScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val session = NetworkModule.getSessionManager()
    var caregiverName by remember { mutableStateOf(session.getLinkedCaregiverInfo()) }
    var showConfirmUnlink by remember { mutableStateOf(false) }

    if (showConfirmUnlink) {
        AlertDialog(
            onDismissRequest = { showConfirmUnlink = false },
            title = { Text("¿Desvincular cuidador?") },
            text = { Text("Dejará de recibir tus alertas y resumen de bienestar. Podrás vincular otro cuidador después.") },
            confirmButton = {
                TextButton(onClick = {
                    session.clearLinkedCaregiverInfo()
                    caregiverName = null
                    showConfirmUnlink = false
                }) { Text("Desvincular") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmUnlink = false }) { Text("Cancelar") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver") }
            Text(text = "Mi cuidador", style = MaterialTheme.typography.titleLarge)
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            if (caregiverName != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = caregiverName!!, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                    }
                }
                Button(
                    onClick = { showConfirmUnlink = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Desvincular cuidador")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Text(
                        text = "Aún no tenemos forma de mostrar tu cuidador vinculado aquí — esta función está pendiente de que el backend confirme el endpoint correspondiente.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
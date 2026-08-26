package com.anxietywatch.mobile.ui.alerts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.common.AsyncUiState
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState

@Composable
fun CriticalAlertScreen(
    eventId: String,
    initialAlert: CriticalAlertUiModel? = null,
    onViewGuide: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: CriticalAlertViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val alert = androidx.compose.runtime.remember(eventId) { initialAlert }
    LaunchedEffect(eventId) { viewModel.loadAlert(eventId, alert) }

    when (val state = uiState) {
        AsyncUiState.Loading -> LoadingState("Cargando alerta...")
        AsyncUiState.Empty -> Column(modifier = Modifier.fillMaxSize()) {
            EmptyState(
                icon = Icons.Default.NotificationsOff,
                title = "No hay una alerta disponible",
                message = "La alerta no contiene información que pueda mostrarse todavía.",
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            ) { Text("Volver") }
        }
        is AsyncUiState.Error -> ErrorState(
            message = state.message,
            onRetry = { viewModel.loadAlert(eventId, alert) },
        )
        is AsyncUiState.Success -> CriticalAlertContent(state.data, onViewGuide, onDismiss)
    }
}

@Composable
private fun CriticalAlertContent(
    alert: CriticalAlertUiModel,
    onViewGuide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = "Alerta crítica",
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(bottom = 16.dp).height(64.dp),
        )
        Text(
            "Alerta de ${alert.patientName}",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onError,
            textAlign = TextAlign.Center,
        )
        Text(
            alert.message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(top = 8.dp),
        )
        alert.location?.let { location ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Text(location, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
        }
        alert.emergencyPhone?.let { phone ->
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onError,
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            ) { Text("Llamar") }
        }
        OutlinedButton(onClick = onViewGuide, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Ver guía", color = MaterialTheme.colorScheme.onError)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onDismiss) { Text("Cerrar alerta", color = MaterialTheme.colorScheme.onError) }
    }
}

package com.anxietywatch.mobile.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState
import com.anxietywatch.mobile.ui.common.MetricCard
import com.anxietywatch.mobile.ui.common.ScreenScaffold
import com.anxietywatch.mobile.ui.common.SectionHeader
import com.anxietywatch.mobile.ui.common.StatusBadge

@Composable
fun CaregiverAlertDetailScreen(
    alertId: String,
    onBack: () -> Unit = {},
    viewModel: CaregiverAlertDetailViewModel? = null,
    state: CaregiverAlertDetailUiState? = null,
    onRetry: (() -> Unit)? = null,
) {
    val resolved = viewModel ?: if (state == null) hiltViewModel() else null
    val collected by resolved?.uiState?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(state!!) }
    ScreenScaffold {
        when (val current = collected) {
            CaregiverAlertDetailUiState.Loading -> LoadingState("Cargando alerta...")
            is CaregiverAlertDetailUiState.Error -> ErrorState(current.message, onRetry ?: resolved?.let { it::retry })
            is CaregiverAlertDetailUiState.Content -> AlertDetailContent(current.data, alertId, onBack)
        }
    }
}

@Composable
private fun AlertDetailContent(alert: CaregiverAlertDetailUiModel, alertId: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("Volver a alertas") }
        SectionHeader("CUIDADOR", "Detalle de alerta", "Información disponible del registro seleccionado.")
        StatusBadge(alert.status ?: alert.type ?: "Sin estado")
        DetailRow("Paciente", alert.patientDisplayName)
        DetailRow("Tipo", alert.type ?: "No disponible")
        DetailRow("Fecha y hora", alert.timestamp ?: "No disponible")
        DetailRow("Resumen", alert.summary ?: "No disponible")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Frecuencia cardíaca", alert.bpm?.toString() ?: "--", alert.bpm?.let { "BPM" }, alert.bpm?.let { null } ?: "Sin lectura", Modifier.weight(1f))
            MetricCard("Ansiedad", alert.anxiety?.toString() ?: "Sin estado disponible", detail = alert.anxiety?.let { "Nivel disponible" }, modifier = Modifier.weight(1f))
        }
        alert.acknowledged?.let { DetailRow("Reconocida", if (it) "Sí" else "No") }
        alert.resolved?.let { DetailRow("Resuelta", if (it) "Sí" else "No") }
        Text("ID: $alertId", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
    }
}

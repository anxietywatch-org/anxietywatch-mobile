package com.anxietywatch.mobile.ui.dashboard

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class CaregiverPatientUiModel(
    val id: String,
    val name: String,
    val status: String,
    val heartRate: Int,
    val lastSync: String,
)

@Composable
fun DashboardCaregiverScreen(
    viewModel: DashboardCaregiverViewModel = hiltViewModel(),
    onPatientClick: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val data = (uiState as? DashboardCaregiverUiState.Success)?.data
    if (data == null) {
        Text(
            text = if (uiState is DashboardCaregiverUiState.Error) {
                (uiState as DashboardCaregiverUiState.Error).message
            } else {
                "Cargando pacientes..."
            },
            modifier = Modifier.padding(24.dp),
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text("Bienvenida de nuevo, ${data.caregiverName}", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Resumen de tus pacientes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        data.patients.forEach { patient ->
            Card(
                onClick = { onPatientClick(patient.name) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(patient.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        AssistChip(onClick = {}, label = { Text(patient.status) })
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("${patient.heartRate} BPM", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Última sincronización: ${patient.lastSync}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Pacientes activos", data.patients.size.toString(), Modifier.weight(1f))
            SummaryCard("Intervenciones hoy", "2", Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text("Registro de intervenciones", style = MaterialTheme.typography.titleMedium)
        Text("Sesión de respiración guiada · Hoy, 10:30", style = MaterialTheme.typography.bodyMedium)
        Text("Revisión de estado · Ayer, 20:15", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

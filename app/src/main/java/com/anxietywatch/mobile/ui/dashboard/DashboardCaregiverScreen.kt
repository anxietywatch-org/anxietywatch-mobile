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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.ui.common.AlertRow
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState
import com.anxietywatch.mobile.ui.common.MetricCard
import com.anxietywatch.mobile.ui.common.PatientRow
import com.anxietywatch.mobile.ui.common.SectionHeader

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
        when (val state = uiState) {
            DashboardCaregiverUiState.Idle,
            DashboardCaregiverUiState.Loading,
            -> LoadingState("Cargando pacientes...")
            is DashboardCaregiverUiState.Error -> ErrorState(state.message, viewModel::loadDashboard)
            is DashboardCaregiverUiState.Success -> Unit
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        SectionHeader(
            eyebrow = "CUIDADOR",
            title = "Bienvenida de nuevo, ${data.caregiverName}",
            description = "Resumen de tus pacientes",
        )
        data.patients.forEach { patient ->
            PatientRow(
                name = patient.name,
                status = patient.status,
                heartRate = patient.heartRate,
                lastSync = patient.lastSync,
                onClick = { onPatientClick(patient.id) },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        if (data.patients.isEmpty()) {
            EmptyState(
                title = "No hay pacientes vinculados.",
                description = "Cuando exista una vinculación disponible, aparecerá aquí.",
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Pacientes activos", data.patients.size.toString(), modifier = Modifier.weight(1f))
            MetricCard("Intervenciones hoy", "2", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text("Registro de intervenciones", style = MaterialTheme.typography.titleMedium)
        AlertRow(
            title = "Sesión de respiración guiada",
            patientName = "Paciente demo",
            occurredAt = "Hoy, 10:30",
            status = "Registrada",
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )
        AlertRow(
            title = "Revisión de estado",
            patientName = "Paciente demo",
            occurredAt = "Ayer, 20:15",
            status = "Registrada",
        )
    }
}

package com.anxietywatch.mobile.ui.wellness

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
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.common.ConnectivityCard
import com.anxietywatch.mobile.ui.common.ConnectivityStatus
import com.anxietywatch.mobile.ui.common.DataFreshnessLabel
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState
import com.anxietywatch.mobile.ui.common.MetricCard
import com.anxietywatch.mobile.ui.common.ScreenScaffold
import com.anxietywatch.mobile.ui.common.SectionHeader
import com.anxietywatch.mobile.ui.common.StatusBadge
import com.anxietywatch.mobile.ui.common.AlertRow

@Composable
fun PatientDetailScreen(
    patientId: String,
    onBack: () -> Unit = {},
    onEventClick: (String) -> Unit = {},
    onAlertClick: (String) -> Unit = {},
    viewModel: CaregiverPatientDetailViewModel? = null,
    state: CaregiverPatientDetailUiState? = null,
    onRetry: (() -> Unit)? = null,
) {
    if (state != null) {
        PatientDetailStateContent(state, onBack, onEventClick, onAlertClick, onRetry ?: {})
    } else {
        val resolvedViewModel = viewModel ?: hiltViewModel<CaregiverPatientDetailViewModel>()
        val collectedState by resolvedViewModel.uiState.collectAsState()
        PatientDetailStateContent(
            state = collectedState,
            onBack = onBack,
            onEventClick = onEventClick,
            onAlertClick = onAlertClick,
            onRetry = resolvedViewModel::retry,
        )
    }
}

@Composable
private fun PatientDetailStateContent(
    state: CaregiverPatientDetailUiState,
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    onAlertClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    ScreenScaffold {
        when (state) {
            CaregiverPatientDetailUiState.Loading -> LoadingState("Cargando paciente...")
            is CaregiverPatientDetailUiState.Error -> ErrorState(state.message, onRetry)
            is CaregiverPatientDetailUiState.Content -> PatientDetailContent(
                state.data,
                onBack,
                onEventClick,
                onAlertClick,
            )
        }
    }
}

@Composable
private fun PatientDetailContent(
    patient: CaregiverPatientDetailUiModel,
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    onAlertClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        TextButton(onClick = onBack) { Text("Volver a pacientes") }
        SectionHeader(
            eyebrow = "CUIDADOR",
            title = patient.displayName,
            description = "Detalle disponible para este paciente",
        )
        patient.alertState?.let { StatusBadge(it, modifier = Modifier.padding(bottom = 12.dp)) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "Frecuencia cardíaca",
                value = patient.bpm?.toString() ?: "--",
                unit = patient.bpm?.let { "BPM" },
                detail = patient.bpm?.let { null } ?: "Sin lectura",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Ansiedad",
                value = patient.anxiety?.toString() ?: "Sin estado disponible",
                detail = patient.anxiety?.let { "Nivel disponible" },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))
        ConnectivityCard(
            status = patient.connectivity ?: ConnectivityStatus.Unknown,
            lastSync = patient.lastUpdated ?: "Sin actualización",
        )
        patient.lastUpdated?.let {
            DataFreshnessLabel(it, modifier = Modifier.padding(top = 8.dp))
        }
        DetailEvents(patient.recentEvents, onEventClick)
        DetailAlerts(patient.recentAlerts, onAlertClick)
    }
}

@Composable
private fun DetailEvents(
    events: List<CaregiverRecentEventUiModel>,
    onEventClick: (String) -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text("Actividad reciente", style = MaterialTheme.typography.titleLarge)
    if (events.isEmpty()) {
        EmptyState("Historial no disponible", "No hay eventos recientes disponibles.")
    } else {
        events.forEach { event ->
            AlertRow(
                title = event.title,
                patientName = event.description ?: "Sin detalle disponible",
                occurredAt = event.occurredAt ?: "Sin fecha",
                status = "Disponible",
                onClick = { onEventClick(event.id) },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun DetailAlerts(
    alerts: List<CaregiverRecentAlertUiModel>,
    onAlertClick: (String) -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text("Alertas recientes", style = MaterialTheme.typography.titleLarge)
    if (alerts.isEmpty()) {
        EmptyState("Alertas no disponibles", "No hay alertas recientes disponibles.")
    } else {
        alerts.forEach { alert ->
            AlertRow(
                title = alert.title,
                patientName = alert.description ?: "Sin detalle disponible",
                occurredAt = alert.occurredAt ?: "Sin fecha",
                status = alert.status ?: "Disponible",
                onClick = { onAlertClick(alert.id) },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

package com.anxietywatch.mobile.ui.wellness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.caregiver.CaregiverTopBar
import com.anxietywatch.mobile.ui.common.AlertRow
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState
import com.anxietywatch.mobile.ui.common.ScreenScaffold
import com.anxietywatch.mobile.ui.common.SectionHeader
import com.anxietywatch.mobile.ui.common.StatusBadge

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
    val resolved = viewModel ?: if (state == null) hiltViewModel() else null
    val collected by resolved?.uiState?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(state!!) }
    ScreenScaffold {
        Column(Modifier.fillMaxSize()) {
            CaregiverTopBar("Detalle del paciente", onBack = onBack)
            when (val current = collected) {
                CaregiverPatientDetailUiState.Loading -> LoadingState("Cargando paciente...", Modifier.weight(1f))
                is CaregiverPatientDetailUiState.Error -> ErrorState(
                    current.message,
                    onRetry ?: resolved?.let { it::retry },
                    Modifier.weight(1f),
                )
                is CaregiverPatientDetailUiState.Content -> PatientDetailContent(
                    patient = current.data,
                    onEventClick = onEventClick,
                    onAlertClick = onAlertClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PatientDetailContent(
    patient: CaregiverPatientDetailUiModel,
    onEventClick: (String) -> Unit,
    onAlertClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
        SectionHeader(
            title = patient.displayName,
            eyebrow = "CUIDADOR",
            description = "Información compartida por este paciente",
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Estado actual", style = MaterialTheme.typography.titleMedium)
                Text(
                    patient.bpm?.let { "$it BPM" } ?: "Sin lectura reciente",
                    style = MaterialTheme.typography.headlineSmall,
                )
                patient.lastUpdated?.let {
                    Text(
                        "Última medición: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            "Eventos recientes",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        if (patient.recentEvents.isEmpty()) {
            EmptyState("No hay eventos disponibles.", "Cuando exista actividad compartida, aparecerá aquí.")
        } else {
            patient.recentEvents.forEach { event ->
                Card(
                    onClick = { onEventClick(event.id) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium)
                            event.description?.let {
                                StatusBadge(it, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                        event.occurredAt?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Text(
            "Alertas recientes",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        if (patient.recentAlerts.isEmpty()) {
            EmptyState(
                "No hay alertas recientes.",
                "Las alertas compartidas aparecerán aquí cuando estén disponibles.",
            )
        } else {
            patient.recentAlerts.forEach { alert ->
                AlertRow(
                    title = alert.title,
                    patientName = alert.description ?: patient.displayName,
                    occurredAt = alert.occurredAt ?: "Fecha no disponible",
                    status = alert.status ?: "Sin estado",
                    onClick = { onAlertClick(alert.id) },
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}

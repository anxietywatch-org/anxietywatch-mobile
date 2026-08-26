package com.anxietywatch.mobile.ui.dashboard

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.caregiver.CaregiverBottomBar
import com.anxietywatch.mobile.ui.caregiver.CaregiverDestination
import com.anxietywatch.mobile.ui.caregiver.CaregiverSummaryCard
import com.anxietywatch.mobile.ui.caregiver.CaregiverTopBar
import com.anxietywatch.mobile.ui.caregiver.PatientCard
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState

@Composable
fun DashboardCaregiverScreen(
    viewModel: DashboardCaregiverViewModel? = null,
    onPatientClick: (String) -> Unit = {},
    onViewAllPatientsClick: () -> Unit = {},
    onViewAlertsClick: () -> Unit = {},
    onViewProfileClick: () -> Unit = {},
    state: DashboardCaregiverUiState? = null,
    onNavigate: (CaregiverDestination) -> Unit = {},
) {
    val resolvedViewModel = viewModel ?: if (state == null) hiltViewModel<DashboardCaregiverViewModel>() else null
    val collectedState by resolvedViewModel?.uiState?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(state!!) }

    Column(modifier = Modifier.fillMaxSize()) {
        CaregiverTopBar(
            title = "AnxietyWatch",
            subtitle = "Panel de cuidador",
            onProfileClick = onViewProfileClick,
        )
        DashboardStateContent(
            state = collectedState,
            modifier = Modifier.weight(1f),
            onRetry = resolvedViewModel?.let { it::retry } ?: {},
            onRefresh = resolvedViewModel?.let { it::refresh } ?: {},
            onPatientClick = onPatientClick,
            onViewAllPatientsClick = onViewAllPatientsClick,
            onViewAlertsClick = onViewAlertsClick,
        )
        CaregiverBottomBar(CaregiverDestination.Home, onNavigate)
    }
}

@Composable
private fun DashboardStateContent(
    state: DashboardCaregiverUiState,
    modifier: Modifier,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onPatientClick: (String) -> Unit,
    onViewAllPatientsClick: () -> Unit,
    onViewAlertsClick: () -> Unit,
) {
    when (state) {
        DashboardCaregiverUiState.Loading -> LoadingState("Cargando pacientes...", modifier)
        is DashboardCaregiverUiState.Error -> ErrorState(state.message, onRetry, modifier)
        is DashboardCaregiverUiState.Empty -> {
            Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                EmptyState(
                    title = "No hay pacientes vinculados.",
                    description = "Cuando exista una vinculación disponible, aparecerá aquí.",
                    modifier = Modifier.padding(20.dp),
                )
                state.refreshError?.let { ErrorState(it, onRefresh) }
            }
        }
        is DashboardCaregiverUiState.Content -> {
            Column(
                modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            ) {
                Text("Resumen", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Personas vinculadas a tu cuenta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                CaregiverSummaryCard(state.data.patients.size)
                Text("Pacientes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp))
                Text(
                    "Lecturas disponibles en cada detalle de paciente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                state.data.patients.take(3).forEach { patient ->
                    PatientCard(
                        displayName = patient.displayName,
                        onClick = { onPatientClick(patient.id) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                TextButton(onClick = onViewAllPatientsClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver todos los pacientes")
                }
                TextButton(onClick = onViewAlertsClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver alertas")
                }
                state.refreshError?.let { ErrorState(it, onRefresh) }
            }
        }
    }
}

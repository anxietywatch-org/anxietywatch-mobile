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
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState
import com.anxietywatch.mobile.ui.common.MetricCard
import com.anxietywatch.mobile.ui.common.PatientRow
import com.anxietywatch.mobile.ui.common.SectionHeader

@Composable
fun DashboardCaregiverScreen(
    viewModel: DashboardCaregiverViewModel? = null,
    onPatientClick: (String) -> Unit = {},
    onViewAllPatientsClick: () -> Unit = {},
    onViewAlertsClick: () -> Unit = {},
    onViewProfileClick: () -> Unit = {},
    state: DashboardCaregiverUiState? = null,
) {
    if (state != null) {
        DashboardCaregiverStateContent(state, {}, {}, onPatientClick, onViewAllPatientsClick, onViewAlertsClick, onViewProfileClick)
    } else {
        val resolvedViewModel = viewModel ?: hiltViewModel<DashboardCaregiverViewModel>()
        val collectedState by resolvedViewModel.uiState.collectAsState()
        DashboardCaregiverStateContent(
            state = collectedState,
            onRetry = resolvedViewModel::retry,
            onRefresh = resolvedViewModel::refresh,
            onPatientClick = onPatientClick,
            onViewAllPatientsClick = onViewAllPatientsClick,
            onViewAlertsClick = onViewAlertsClick,
            onViewProfileClick = onViewProfileClick,
        )
    }
}

@Composable
private fun DashboardCaregiverStateContent(
    state: DashboardCaregiverUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onPatientClick: (String) -> Unit,
    onViewAllPatientsClick: () -> Unit,
    onViewAlertsClick: () -> Unit,
    onViewProfileClick: () -> Unit,
) {
    when (state) {
        DashboardCaregiverUiState.Loading -> LoadingState("Cargando pacientes...")
        is DashboardCaregiverUiState.Error -> ErrorState(state.message, onRetry)
        is DashboardCaregiverUiState.Empty -> DashboardEmptyState(state, onRefresh)
        is DashboardCaregiverUiState.Content -> DashboardContent(state, onRefresh, onPatientClick, onViewAllPatientsClick, onViewAlertsClick, onViewProfileClick)
    }
}

@Composable
private fun DashboardEmptyState(
    state: DashboardCaregiverUiState.Empty,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            SectionHeader(
                eyebrow = "CUIDADOR",
                title = "Panel de pacientes",
                description = "Pacientes vinculados a tu cuenta",
            )
            state.refreshError?.let { ErrorState(it, onRefresh) }
            EmptyState(
                title = "No hay pacientes vinculados.",
                description = "Cuando exista una vinculación disponible, aparecerá aquí.",
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardCaregiverUiState.Content,
    onRefresh: () -> Unit,
    onPatientClick: (String) -> Unit,
    onViewAllPatientsClick: () -> Unit,
    onViewAlertsClick: () -> Unit,
    onViewProfileClick: () -> Unit,
) {
    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            SectionHeader(
                eyebrow = "CUIDADOR",
                title = "Panel de pacientes",
                description = "Pacientes vinculados a tu cuenta",
            )
            state.refreshError?.let { ErrorState(it, onRefresh) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Pacientes vinculados", state.data.patients.size.toString(), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Text("Pacientes recientes", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onViewAllPatientsClick) { Text("Ver todos") }
            TextButton(onClick = onViewAlertsClick) { Text("Ver alertas") }
            TextButton(onClick = onViewProfileClick) { Text("Ver perfil") }
            Spacer(Modifier.height(8.dp))
            state.data.patients.forEach { patient ->
                CaregiverPatientRow(patient, onPatientClick)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CaregiverPatientRow(
    patient: CaregiverPatientUiModel,
    onPatientClick: (String) -> Unit,
) {
    PatientRow(
        name = patient.displayName,
        status = patient.alertState ?: patient.connectivityLabel(),
        heartRate = patient.bpm,
        lastSync = patient.lastUpdated ?: "Sin actualización",
        onClick = { onPatientClick(patient.id) },
    )
}

private fun CaregiverPatientUiModel.connectivityLabel(): String = when {
    connectivity != null -> when (connectivity) {
        com.anxietywatch.mobile.ui.common.ConnectivityStatus.ConnectedRecent -> "Conectado · reciente"
        com.anxietywatch.mobile.ui.common.ConnectivityStatus.ConnectedStale -> "Conectado · antiguo"
        com.anxietywatch.mobile.ui.common.ConnectivityStatus.Disconnected -> "Desconectado"
        com.anxietywatch.mobile.ui.common.ConnectivityStatus.Unknown -> "Sin información"
    }
    freshness != null -> freshness.name
    else -> "Sin estado"
}

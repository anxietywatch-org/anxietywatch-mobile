package com.anxietywatch.mobile.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import com.anxietywatch.mobile.ui.common.PatientRow
import com.anxietywatch.mobile.ui.common.ScreenScaffold
import com.anxietywatch.mobile.ui.common.SectionHeader

@Composable
fun CaregiverPatientsScreen(
    viewModel: DashboardCaregiverViewModel? = null,
    onPatientClick: (String) -> Unit = {},
    state: DashboardCaregiverUiState? = null,
    onRetry: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
) {
    if (state != null) {
        CaregiverPatientsStateContent(
            state = state,
            onRetry = onRetry ?: {},
            onRefresh = onRefresh ?: {},
            onPatientClick = onPatientClick,
        )
    } else {
        val resolvedViewModel = viewModel ?: hiltViewModel<DashboardCaregiverViewModel>()
        val collectedState by resolvedViewModel.uiState.collectAsState()
        CaregiverPatientsStateContent(
            state = collectedState,
            onRetry = resolvedViewModel::retry,
            onRefresh = resolvedViewModel::refresh,
            onPatientClick = onPatientClick,
        )
    }
}

@Composable
private fun CaregiverPatientsStateContent(
    state: DashboardCaregiverUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onPatientClick: (String) -> Unit,
) {
    ScreenScaffold {
        when (state) {
            DashboardCaregiverUiState.Loading -> LoadingState("Cargando pacientes...")
            is DashboardCaregiverUiState.Error -> ErrorState(state.message, onRetry)
            is DashboardCaregiverUiState.Empty -> PatientsEmptyState(state, onRefresh)
            is DashboardCaregiverUiState.Content -> PatientsContent(state, onRefresh, onPatientClick)
        }
    }
}

@Composable
private fun PatientsEmptyState(
    state: DashboardCaregiverUiState.Empty,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            PatientsHeader()
            state.refreshError?.let { ErrorState(it, onRefresh) }
            EmptyState(
                title = "No hay pacientes vinculados.",
                description = "Cuando exista una vinculación disponible, aparecerá aquí.",
            )
        }
    }
}

@Composable
private fun PatientsContent(
    state: DashboardCaregiverUiState.Content,
    onRefresh: () -> Unit,
    onPatientClick: (String) -> Unit,
) {
    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            PatientsHeader()
            state.refreshError?.let { ErrorState(it, onRefresh) }
            state.data.patients.forEach { patient ->
                PatientRow(
                    name = patient.displayName,
                    status = patient.statusLabel(),
                    heartRate = patient.bpm,
                    lastSync = patient.lastUpdated ?: "Sin actualización",
                    anxietyLabel = patient.anxiety?.let { "Ansiedad: $it" } ?: "Sin estado disponible",
                    showMissingHeartRatePlaceholder = true,
                    onClick = { onPatientClick(patient.id) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PatientsHeader() {
    SectionHeader(
        eyebrow = "CUIDADOR",
        title = "Pacientes",
        description = "Personas vinculadas a tu cuenta",
    )
    Text("Lista de pacientes", modifier = Modifier.padding(bottom = 8.dp))
}

private fun CaregiverPatientUiModel.statusLabel(): String = when {
    alertState != null -> alertState
    connectivity != null -> when (connectivity) {
        com.anxietywatch.mobile.ui.common.ConnectivityStatus.ConnectedRecent -> "Conectado · reciente"
        com.anxietywatch.mobile.ui.common.ConnectivityStatus.ConnectedStale -> "Conectado · antiguo"
        com.anxietywatch.mobile.ui.common.ConnectivityStatus.Disconnected -> "Desconectado"
        com.anxietywatch.mobile.ui.common.ConnectivityStatus.Unknown -> "Sin información"
    }
    freshness != null -> freshness.name
    else -> "Sin estado"
}

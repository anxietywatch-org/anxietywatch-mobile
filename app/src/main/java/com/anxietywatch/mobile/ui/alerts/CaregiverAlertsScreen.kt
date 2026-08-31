package com.anxietywatch.mobile.ui.alerts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.caregiver.CaregiverBottomBar
import com.anxietywatch.mobile.ui.caregiver.CaregiverDestination
import com.anxietywatch.mobile.ui.caregiver.CaregiverTopBar
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState
import com.anxietywatch.mobile.ui.common.ScreenScaffold
import com.anxietywatch.mobile.ui.common.SectionHeader

@Composable
fun CaregiverAlertsScreen(
    onBack: () -> Unit = {},
    onAlertClick: (String) -> Unit = {},
    viewModel: CaregiverAlertsViewModel? = null,
    state: CaregiverAlertsUiState? = null,
    onRetry: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onNavigate: (CaregiverDestination) -> Unit = {},
) {
    val resolved = viewModel ?: if (state == null) hiltViewModel() else null
    val collected by resolved?.uiState?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(state!!) }
    Column(Modifier.fillMaxSize()) {
        CaregiverTopBar("Alertas", onBack = onBack)
        CaregiverAlertsStateContent(
            state = collected,
            modifier = Modifier.weight(1f),
            onAlertClick = onAlertClick,
            onRetry = onRetry ?: resolved?.let { it::retry } ?: {},
            onRefresh = onRefresh ?: resolved?.let { it::refresh } ?: {},
        )
        CaregiverBottomBar(CaregiverDestination.Alerts, onNavigate)
    }
}

@Composable
private fun CaregiverAlertsStateContent(
    state: CaregiverAlertsUiState,
    modifier: Modifier,
    onAlertClick: (String) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
) {
    ScreenScaffold(modifier) {
        when (state) {
            CaregiverAlertsUiState.Loading -> LoadingState("Cargando alertas...")
            is CaregiverAlertsUiState.Error -> ErrorState(state.message, onRetry)
            is CaregiverAlertsUiState.Empty -> AlertsEmpty(state, onRefresh)
            is CaregiverAlertsUiState.Content -> AlertsContent(state, onAlertClick, onRefresh)
        }
    }
}

@Composable
private fun AlertsEmpty(state: CaregiverAlertsUiState.Empty, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        SectionHeader("Alertas", "CUIDADOR", "Información compartida por tus pacientes")
        state.refreshError?.let { ErrorState(it, onRefresh) }
        EmptyState("No hay alertas", "Las alertas compartidas aparecerán aquí cuando estén disponibles.")
    }
}

@Composable
private fun AlertsContent(
    state: CaregiverAlertsUiState.Content,
    onAlertClick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        SectionHeader("Alertas", "CUIDADOR", "Información compartida por tus pacientes")
        state.refreshError?.let { ErrorState(it, onRefresh) }
        state.data.forEach { alert ->
            com.anxietywatch.mobile.ui.common.AlertRow(
                title = alert.title,
                patientName = buildString {
                    append(alert.patientDisplayName)
                    alert.summary?.let { append(" · ").append(it) }
                },
                occurredAt = alert.timestamp ?: "Fecha no disponible",
                status = alert.status ?: alert.type ?: "Sin estado",
                onClick = { onAlertClick(alert.id) },
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

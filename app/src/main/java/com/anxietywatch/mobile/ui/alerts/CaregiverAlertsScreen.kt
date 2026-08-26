package com.anxietywatch.mobile.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.common.AlertRow
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
) {
    val resolved = viewModel ?: if (state == null) hiltViewModel() else null
    val collected by resolved?.uiState?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(state!!) }
    CaregiverAlertsStateContent(
        state = collected,
        onBack = onBack,
        onAlertClick = onAlertClick,
        onRetry = onRetry ?: resolved?.let { it::retry } ?: {},
        onRefresh = onRefresh ?: resolved?.let { it::refresh } ?: {},
    )
}

@Composable
private fun CaregiverAlertsStateContent(
    state: CaregiverAlertsUiState,
    onBack: () -> Unit,
    onAlertClick: (String) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
) {
    ScreenScaffold {
        when (state) {
            CaregiverAlertsUiState.Loading -> LoadingState("Cargando alertas...")
            is CaregiverAlertsUiState.Error -> ErrorState(state.message, onRetry)
            is CaregiverAlertsUiState.Empty -> AlertsEmpty(state, onBack, onRefresh)
            is CaregiverAlertsUiState.Content -> AlertsContent(state, onBack, onAlertClick, onRefresh)
        }
    }
}

@Composable
private fun AlertsEmpty(state: CaregiverAlertsUiState.Empty, onBack: () -> Unit, onRefresh: () -> Unit) {
    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            TextButton(onClick = onBack) { Text("Volver al dashboard") }
            SectionHeader("CUIDADOR", "Alertas", "Alertas disponibles de tus pacientes")
            state.refreshError?.let { ErrorState(it, onRefresh) }
            EmptyState("No hay alertas", "Cuando exista una alerta disponible, aparecerá aquí.")
        }
    }
}

@Composable
private fun AlertsContent(
    state: CaregiverAlertsUiState.Content,
    onBack: () -> Unit,
    onAlertClick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            TextButton(onClick = onBack) { Text("Volver al dashboard") }
            SectionHeader("CUIDADOR", "Alertas", "Alertas disponibles de tus pacientes")
            state.refreshError?.let { ErrorState(it, onRefresh) }
            state.data.forEach { alert ->
                AlertRow(
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
            Spacer(Modifier.height(8.dp))
        }
    }
}

package com.anxietywatch.mobile.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.caregiver.CaregiverBottomBar
import com.anxietywatch.mobile.ui.caregiver.CaregiverDestination
import com.anxietywatch.mobile.ui.caregiver.CaregiverTopBar
import com.anxietywatch.mobile.ui.caregiver.PatientCard
import com.anxietywatch.mobile.ui.caregiver.CaregiverSummaryCard
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCaregiverScreen(
    viewModel: DashboardCaregiverViewModel? = null,
    onPatientClick: (String) -> Unit = {},
    onViewAllPatientsClick: () -> Unit = {},
    onViewAlertsClick: () -> Unit = {},
    onViewProfileClick: () -> Unit = {},
    state: DashboardCaregiverUiState? = null,
    onNavigate: (CaregiverDestination) -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val resolvedViewModel = viewModel ?: if (state == null) hiltViewModel() else null
    val collectedState by resolvedViewModel?.uiState?.collectAsState()
        ?: remember { mutableStateOf(state!!) }
    val linkState by resolvedViewModel?.linkPatientUiState?.collectAsState()
        ?: remember { mutableStateOf<LinkPatientUiState>(LinkPatientUiState.Idle) }
    val isRefreshing by resolvedViewModel?.isRefreshing?.collectAsState()
        ?: remember { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(linkState) {
        if (linkState is LinkPatientUiState.Success) code = ""
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        CaregiverTopBar(
            title = "AnxietyWatch",
            subtitle = "Panel de cuidador",
            onProfileClick = onViewProfileClick,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onLogout) { Text("Cerrar sesión") }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { resolvedViewModel?.loadDashboard(isManualRefresh = true) },
            modifier = Modifier.weight(1f),
        ) {
            DashboardStateContent(
                state = collectedState,
                modifier = Modifier.fillMaxSize(),
                onRetry = resolvedViewModel?.let { it::retry } ?: {},
                onRefresh = resolvedViewModel?.let { it::refresh } ?: {},
                onPatientClick = onPatientClick,
                onViewAllPatientsClick = onViewAllPatientsClick,
                onViewAlertsClick = onViewAlertsClick,
            )
        }
        LinkPatientSection(
            code = code,
            uiState = linkState,
            onCodeChange = { input ->
                code = input.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(20)
                if (linkState is LinkPatientUiState.Error) resolvedViewModel?.dismissLinkPatientError()
            },
            onLink = { resolvedViewModel?.linkPatient(code) },
        )
        CaregiverBottomBar(CaregiverDestination.Home, onNavigate)
    }
}

@Composable
private fun LinkPatientSection(
    code: String,
    uiState: LinkPatientUiState,
    onCodeChange: (String) -> Unit,
    onLink: () -> Unit,
) {
    val isLoading = uiState is LinkPatientUiState.Loading
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Vincular nuevo paciente", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                label = { Text("Código") },
                placeholder = { Text("ANX-XXXXXX") },
                singleLine = true,
                isError = uiState is LinkPatientUiState.Error,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (!isLoading) onLink() }),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            when (uiState) {
                is LinkPatientUiState.Error -> Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
                LinkPatientUiState.Success -> Text(
                    text = "Paciente vinculado correctamente.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                LinkPatientUiState.Idle,
                LinkPatientUiState.Loading,
                -> Unit
            }
            Button(
                onClick = onLink,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                }
                Text(if (isLoading) "Vinculando..." else "Vincular")
            }
        }
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
                        bpm = patient.bpm,
                        lastUpdated = patient.lastUpdated,
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

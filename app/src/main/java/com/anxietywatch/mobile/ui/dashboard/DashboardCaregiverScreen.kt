package com.anxietywatch.mobile.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.anxietywatch.mobile.ui.common.AsyncUiState
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState

data class CaregiverPatientUiModel(
    val id: String,
    val name: String,
    val status: String? = null,
    val heartRate: Int? = null,
    val lastSync: String? = null,
)

@Composable
fun DashboardCaregiverScreen(
    viewModel: DashboardCaregiverViewModel = hiltViewModel(),
    onPatientClick: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val linkPatientUiState by viewModel.linkPatientUiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(linkPatientUiState) {
        if (linkPatientUiState is LinkPatientUiState.Success) code = ""
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadDashboard(isManualRefresh = true) },
            modifier = Modifier.weight(1f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    AsyncUiState.Loading -> LoadingState("Cargando pacientes...")
                    AsyncUiState.Empty -> EmptyState(
                        icon = Icons.Default.People,
                        title = "No hay pacientes vinculados todavía",
                        message = "Cuando se vincule un paciente, aparecerá aquí.",
                    )
                    is AsyncUiState.Error -> ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadDashboard() },
                    )
                    is AsyncUiState.Success -> DashboardContent(state.data, onPatientClick)
                }
            }
        }
        LinkPatientSection(
            code = code,
            uiState = linkPatientUiState,
            onCodeChange = { input ->
                code = input.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(20)
                if (linkPatientUiState is LinkPatientUiState.Error) viewModel.dismissLinkPatientError()
            },
            onLink = { viewModel.linkPatient(code) },
        )
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
            if (uiState is LinkPatientUiState.Error) {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (uiState is LinkPatientUiState.Success) {
                Text(
                    text = "Paciente vinculado correctamente.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
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
private fun DashboardContent(data: DashboardCaregiverData, onPatientClick: (String) -> Unit) {
    if (data.patients.isEmpty()) {
        EmptyState(
            icon = Icons.Default.People,
            title = "No hay pacientes vinculados todavía",
            message = "Cuando se vincule un paciente, aparecerá aquí.",
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text(
            data.caregiverName?.let { "Bienvenida de nuevo, $it" } ?: "Panel de cuidador",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            "Resumen de tus pacientes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        data.patients.forEach { patient ->
            Card(
                onClick = { onPatientClick(patient.id) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(patient.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        patient.status?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                    }
                    patient.heartRate?.let {
                        Spacer(Modifier.height(12.dp))
                        Text("$it BPM", style = MaterialTheme.typography.headlineSmall)
                    }
                    patient.lastSync?.let {
                        Text(
                            "Última sincronización: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

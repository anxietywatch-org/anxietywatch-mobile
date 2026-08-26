package com.anxietywatch.mobile.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.caregiver.CaregiverBottomBar
import com.anxietywatch.mobile.ui.caregiver.CaregiverDestination
import com.anxietywatch.mobile.ui.caregiver.CaregiverSearchField
import com.anxietywatch.mobile.ui.caregiver.CaregiverTopBar
import com.anxietywatch.mobile.ui.caregiver.PatientCard
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState

@Composable
fun CaregiverPatientsScreen(
    viewModel: DashboardCaregiverViewModel? = null,
    onPatientClick: (String) -> Unit = {},
    state: DashboardCaregiverUiState? = null,
    onRetry: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onNavigate: (CaregiverDestination) -> Unit = {},
) {
    val resolvedViewModel = viewModel ?: if (state == null) hiltViewModel<DashboardCaregiverViewModel>() else null
    val collectedState by resolvedViewModel?.uiState?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(state!!) }
    var query by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        CaregiverTopBar("Pacientes")
        PatientsStateContent(
            state = collectedState,
            modifier = Modifier.weight(1f),
            query = query,
            onQueryChange = { query = it },
            onRetry = onRetry ?: resolvedViewModel?.let { it::retry } ?: {},
            onRefresh = onRefresh ?: resolvedViewModel?.let { it::refresh } ?: {},
            onPatientClick = onPatientClick,
        )
        CaregiverBottomBar(CaregiverDestination.Patients, onNavigate)
    }
}

@Composable
private fun PatientsStateContent(
    state: DashboardCaregiverUiState,
    modifier: Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onPatientClick: (String) -> Unit,
) {
    when (state) {
        DashboardCaregiverUiState.Loading -> LoadingState("Cargando pacientes...", modifier)
        is DashboardCaregiverUiState.Error -> ErrorState(state.message, onRetry, modifier)
        is DashboardCaregiverUiState.Empty -> EmptyState(
            title = "No hay pacientes vinculados.",
            description = "Cuando exista una vinculación disponible, aparecerá aquí.",
            modifier = modifier,
        )
        is DashboardCaregiverUiState.Content -> {
            val filtered = state.data.patients.filter { it.displayName.contains(query.trim(), ignoreCase = true) }
            Column(modifier = modifier.fillMaxWidth()) {
                CaregiverSearchField(query, onQueryChange, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                if (filtered.isEmpty()) {
                    EmptyState(
                        title = "No encontramos pacientes con ese nombre.",
                        description = if (query.isBlank()) "No hay pacientes vinculados." else null,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
                        items(filtered, key = { it.id }) { patient ->
                            PatientCard(
                                displayName = patient.displayName,
                                onClick = { onPatientClick(patient.id) },
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                    }
                }
                state.refreshError?.let { ErrorState(it, onRefresh) }
            }
        }
    }
}

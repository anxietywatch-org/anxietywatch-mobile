package com.anxietywatch.mobile.ui.events

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.common.AsyncUiState
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState

@Composable
fun EventDetailScreen(
    patientId: String,
    eventId: String,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(patientId, eventId) { viewModel.loadEvent(patientId, eventId) }

    when (val state = uiState) {
        AsyncUiState.Loading -> LoadingState("Cargando evento...")
        AsyncUiState.Empty -> EmptyState(
            icon = Icons.Default.EventBusy,
            title = "Evento no disponible",
            message = "Todavía no hay información de este evento para mostrar.",
        )
        is AsyncUiState.Error -> ErrorState(
            message = state.message,
            onRetry = { viewModel.loadEvent(patientId, eventId) },
        )
        is AsyncUiState.Success -> EventDetailContent(state.data)
    }
}

@Composable
private fun EventDetailContent(event: EventDetailUiModel) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            event.category?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
            event.occurredAt?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
        }
        Text(event.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 12.dp))
        event.summary?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        if (event.metrics.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                event.metrics.forEach { metric -> MetricCard(metric) }
            }
        }
        event.location?.let { location ->
            Text("Ubicación del evento", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(location, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        event.systemNotes?.let { notes ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notas del sistema", style = MaterialTheme.typography.titleMedium)
                    Text(notes, modifier = Modifier.padding(top = 8.dp))
                    if (event.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()),
                        ) {
                            event.tags.forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(metric: EventMetricUiModel) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(metric.title, style = MaterialTheme.typography.labelSmall)
            Text(metric.value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp))
            metric.detail?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

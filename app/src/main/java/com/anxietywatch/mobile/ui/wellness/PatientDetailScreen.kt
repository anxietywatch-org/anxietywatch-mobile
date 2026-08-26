package com.anxietywatch.mobile.ui.wellness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.common.AsyncUiState
import com.anxietywatch.mobile.ui.common.EmptyState
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState

@Composable
fun PatientDetailScreen(
    patientId: String,
    onEventClick: (String) -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(patientId) { viewModel.loadPatient(patientId) }

    when (val state = uiState) {
        AsyncUiState.Loading -> LoadingState("Cargando información del paciente...")
        AsyncUiState.Empty -> EmptyState(
            icon = Icons.Default.PersonOff,
            title = "Información no disponible",
            message = "Todavía no hay datos de este paciente para mostrar.",
        )
        is AsyncUiState.Error -> ErrorState(
            message = state.message,
            onRetry = { viewModel.loadPatient(patientId) },
        )
        is AsyncUiState.Success -> PatientDetailContent(state.data, onEventClick)
    }
}

@Composable
private fun PatientDetailContent(patient: PatientDetailUiModel, onEventClick: (String) -> Unit) {
    var selectedBar by remember(patient.heartRateSamples) {
        mutableIntStateOf((patient.heartRateSamples.size - 1).coerceAtLeast(0))
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(patient.name, style = MaterialTheme.typography.headlineLarge)
        patient.status?.let {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(it, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) }
        }
        if (patient.heartRateSamples.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Frecuencia cardíaca", style = MaterialTheme.typography.titleLarge)
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("BPM por franja horaria", style = MaterialTheme.typography.bodySmall)
                    HeartRateBarChart(
                        samples = patient.heartRateSamples,
                        selectedIndex = selectedBar,
                        onSelected = { selectedBar = it },
                    )
                }
            }
        }
        Text("Eventos recientes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        if (patient.events.isEmpty()) {
            Text(
                "No hay eventos registrados.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            patient.events.forEach { event ->
                EventRow(event = event, onClick = { onEventClick(event.id) })
            }
        }
    }
}

@Composable
private fun HeartRateBarChart(
    samples: List<HeartRateSampleUiModel>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.primaryContainer
    val maxValue = samples.maxOfOrNull { it.beatsPerMinute }?.coerceAtLeast(1) ?: 1
    Row(modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
        samples.forEachIndexed { index, sample ->
            Column(
                modifier = Modifier.weight(1f).fillMaxSize().clickable { onSelected(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (selectedIndex == index) {
                    Text("${sample.beatsPerMinute} BPM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Canvas(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                    onDraw = {
                        val barHeight = size.height * (sample.beatsPerMinute / maxValue.toFloat())
                        drawRoundRect(
                            color = if (selectedIndex == index) selectedColor else unselectedColor,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight),
                            size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                            cornerRadius = CornerRadius(10f, 10f),
                        )
                    },
                )
                Text(sample.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EventRow(event: WellnessEventUiModel, onClick: () -> Unit) {
    val (icon, tint) = when (event.type) {
        WellnessEventType.Crisis -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        WellnessEventType.Breathing -> Icons.Default.Air to MaterialTheme.colorScheme.tertiary
        WellnessEventType.ElevatedRhythm -> Icons.AutoMirrored.Filled.DirectionsRun to MaterialTheme.colorScheme.secondary
        WellnessEventType.Unknown -> Icons.Default.Event to MaterialTheme.colorScheme.primary
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = tint.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(9.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                event.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            event.time?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

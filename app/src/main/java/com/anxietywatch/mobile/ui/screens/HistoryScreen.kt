package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.DashboardSummary
import com.anxietywatch.mobile.network.EpisodeSummary
import com.anxietywatch.mobile.network.HistoryExporter
import com.anxietywatch.mobile.network.NetworkModule
import retrofit2.HttpException

private enum class EventFilter { ALL, CRISIS, RELAXATION }

@Composable
fun HistoryScreen(modifier: Modifier = Modifier, onOpenEpisode: (EpisodeSummary) -> Unit = {}) {
    val localContext = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var episodes by remember { mutableStateOf<List<EpisodeSummary>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf(EventFilter.ALL) }
    var sortNewestFirst by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            summary = NetworkModule.api.getDashboardSummary()
            episodes = NetworkModule.api.getEpisodes()
        } catch (e: HttpException) {
            errorMessage = "Error del servidor: código ${e.code()}"
        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    val filteredEpisodes = episodes
        .filter {
            when (filter) {
                EventFilter.ALL -> true
                EventFilter.CRISIS -> it.severity?.contains("alta", ignoreCase = true) == true || it.severity?.contains("crisis", ignoreCase = true) == true
                EventFilter.RELAXATION -> it.notes?.contains("relaj", ignoreCase = true) == true
            }
        }
        .let { list -> if (sortNewestFirst) list.sortedByDescending { it.date } else list.sortedBy { it.date } }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text(text = "Historial de salud", style = MaterialTheme.typography.headlineMedium)

        if (isLoading) CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
        errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }

        summary?.let { data ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = "Resumen actual", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                    HistoryStatRow("Nivel de ansiedad", "${data.anxietyLevel.current}", "Tendencia: ${data.anxietyLevel.trend}")
                    HistoryStatRow("Registros esta semana", "${data.weeklyRecords.used} / ${data.weeklyRecords.limit}", "Usados de tu límite semanal")
                    HistoryStatRow("Racha", "${data.streakDays} días", "Días consecutivos de seguimiento")
                    HistoryStatRow("Ejercicios completados", "${data.exercisesCompleted}", "Total acumulado")
                }
            }
        }

        Text(text = "Eventos recientes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { filter = EventFilter.ALL }, label = { Text("Todos") }, colors = chipColors(filter == EventFilter.ALL))
            AssistChip(onClick = { filter = EventFilter.CRISIS }, label = { Text("Crisis") }, colors = chipColors(filter == EventFilter.CRISIS))
            AssistChip(onClick = { filter = EventFilter.RELAXATION }, label = { Text("Relajación") }, colors = chipColors(filter == EventFilter.RELAXATION))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            AssistChip(
                onClick = { sortNewestFirst = !sortNewestFirst },
                label = { Text(if (sortNewestFirst) "Más reciente primero" else "Más antiguo primero") }
            )
            AssistChip(
                onClick = { HistoryExporter.shareHistory(localContext, episodes, summary) },
                label = { Text("Exportar") }
            )
        }

        if (!isLoading && errorMessage == null && filteredEpisodes.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Filled.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Text(
                    text = "Aún no tienes episodios registrados",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        for (episode in filteredEpisodes) {
            val icon: ImageVector = if (episode.severity?.contains("alta", ignoreCase = true) == true) Icons.Filled.Warning else Icons.Filled.SelfImprovement
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onOpenEpisode(episode) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = episode.date ?: "Fecha no disponible", style = MaterialTheme.typography.labelLarge)
                        episode.severity?.let { Text(text = "Severidad: $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun chipColors(selected: Boolean) = if (selected) {
    AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
} else {
    AssistChipDefaults.assistChipColors()
}

@Composable
private fun HistoryStatRow(title: String, value: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}
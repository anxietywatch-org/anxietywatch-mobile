package com.anxietywatch.mobile.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.data.remote.EpisodeDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PatientHistoryScreen(viewModel: PatientHistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        PatientHistoryUiState.Idle,
        PatientHistoryUiState.Loading,
        -> Text("Cargando tu historial...", modifier = Modifier.padding(24.dp))
        is PatientHistoryUiState.Error -> Text(
            state.message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(24.dp),
        )
        is PatientHistoryUiState.Success -> HistoryContent(state.episodes)
    }
}

@Composable
private fun HistoryContent(episodes: List<EpisodeDto>) {
    var selectedIndex by remember(episodes) { mutableIntStateOf((episodes.size - 1).coerceAtLeast(0)) }
    val chartEpisodes = episodes.takeLast(4)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Historial del paciente", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Últimos 7 días",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (episodes.isEmpty()) {
            EmptyHistoryMessage()
        } else {
            Text("Nivel de ansiedad", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Intensidad por episodio", style = MaterialTheme.typography.bodySmall)
                    PatientHistoryBarChart(
                        episodes = chartEpisodes,
                        selectedIndex = (selectedIndex - (episodes.size - chartEpisodes.size)).coerceIn(0, chartEpisodes.lastIndex),
                        onSelected = { index -> selectedIndex = episodes.size - chartEpisodes.size + index },
                    )
                }
            }
            Text("Eventos recientes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            episodes.asReversed().forEach { episode -> HistoryEventRow(episode) }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun EmptyHistoryMessage() {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Aún no hay episodios registrados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            Text(
                "Cuando haya actividad registrada, aparecerá aquí.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun PatientHistoryBarChart(
    episodes: List<EpisodeDto>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
        episodes.forEachIndexed { index, episode ->
            Column(
                modifier = Modifier.weight(1f).fillMaxSize().clickable { onSelected(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (selectedIndex == index) {
                    Text("${episode.intensity}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                val selectedColor = MaterialTheme.colorScheme.primary
                val normalColor = MaterialTheme.colorScheme.primaryContainer
                Canvas(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val barHeight = size.height * (episode.intensity / 100f)
                    drawRoundRect(
                        color = if (selectedIndex == index) selectedColor else normalColor,
                        topLeft = Offset(0f, size.height - barHeight),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                }
                Text(formatHour(episode.date), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HistoryEventRow(episode: EpisodeDto) {
    val isHigh = episode.intensity > 70
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = (if (isHigh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary).copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    if (isHigh) Icons.Default.Warning else Icons.Default.History,
                    contentDescription = null,
                    tint = if (isHigh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(9.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(titleFor(episode.intensity), style = MaterialTheme.typography.titleMedium)
                Text(episode.notes ?: "Sin notas registradas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatHour(episode.date), style = MaterialTheme.typography.labelSmall)
                Text("${episode.intensity}/100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun titleFor(intensity: Int): String = when {
    intensity > 70 -> "Pico de ansiedad"
    intensity >= 40 -> "Ritmo elevado"
    else -> "Episodio registrado"
}

private fun formatHour(rawDate: String): String = runCatching {
    DateTimeFormatter.ofPattern("HH:mm").format(Instant.parse(rawDate).atZone(ZoneId.systemDefault()))
}.getOrDefault(rawDate)

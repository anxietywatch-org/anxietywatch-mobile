package com.anxietywatch.mobile.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.data.remote.EpisodeDto
import com.anxietywatch.mobile.ui.common.SectionHeader
import com.anxietywatch.mobile.ui.common.StatusBadge
import com.anxietywatch.mobile.ui.common.StatusTone

@Composable
fun EventDetailScreen(
    eventId: String,
    episode: EpisodeDto? = null,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        onBack?.let {
            TextButton(onClick = it) { Text("Volver al historial") }
        }
        SectionHeader(
            eyebrow = "PACIENTE",
            title = "Detalle del episodio",
            description = "Información disponible del registro seleccionado.",
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusBadge(
                    label = episode?.let { "Intensidad ${it.intensity}/100" } ?: "Información limitada",
                    tone = episode?.let { if (it.intensity >= 70) StatusTone.Warning else StatusTone.Neutral }
                        ?: StatusTone.Neutral,
                )
                DetailRow("ID", eventId)
                if (episode == null) {
                    Text(
                        "No existe un detalle remoto disponible para este evento.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    DetailRow("Fecha", episode.date)
                    DetailRow("Intensidad", "${episode.intensity}/100")
                    DetailRow(
                        "Síntomas",
                        episode.symptoms.takeIf { it.isNotEmpty() }?.joinToString(", ")
                            ?: "Información no disponible",
                    )
                    DetailRow("Notas", episode.notes ?: "Información no disponible")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
fun LocationPlaceholderCard() {
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp).background(
            MaterialTheme.colorScheme.secondaryContainer,
            RoundedCornerShape(12.dp),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            androidx.compose.material3.Icon(Icons.Default.LocationOn, contentDescription = null)
            Text("Ubicación no disponible", style = MaterialTheme.typography.titleMedium)
            Text("El contrato actual no incluye coordenadas.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

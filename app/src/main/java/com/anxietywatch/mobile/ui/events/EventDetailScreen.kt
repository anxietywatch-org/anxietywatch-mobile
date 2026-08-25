package com.anxietywatch.mobile.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EventDetailScreen(eventId: String, onShare: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Alerta Crítica", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            Text("Hoy, 14:20", style = MaterialTheme.typography.labelMedium)
        }
        Text("Pico de Ansiedad Detectado", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 12.dp))
        Text("El reloj detectó una elevación significativa de la frecuencia cardíaca.", modifier = Modifier.padding(top = 8.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Duración", "12m 45s", "Intensidad alta", Modifier.weight(1f))
            MetricCard("BPM Pico", "128", "+65% basal", Modifier.weight(1f))
            MetricCard("Recuperación", "Estable", "18 minutos", Modifier.weight(1f))
        }
        Text("Ubicación del evento", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        LocationPlaceholderCard()
        Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Notas del Sistema", style = MaterialTheme.typography.titleMedium)
                Text(
                    "La frecuencia cardíaca aumentó rápidamente y se mantuvo elevada durante varios minutos. El patrón fue compatible con un episodio de ansiedad.",
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()),
                ) {
                    AssistChip(onClick = {}, label = { Text("Frecuencia Elevada") })
                    AssistChip(onClick = {}, label = { Text("Entorno Exterior") })
                    AssistChip(onClick = {}, label = { Text("IA Confirmado") })
                }
            }
        }
        Button(onClick = onShare, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text("Compartir con cuidador")
        }
        Text(
            "ID del evento: $eventId",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun MetricCard(title: String, value: String, detail: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp))
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LocationPlaceholderCard() {
    // TODO: integrar mapa cuando se confirme el proveedor y el contrato de ubicación del evento.
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp).background(
            MaterialTheme.colorScheme.secondaryContainer,
            RoundedCornerShape(12.dp),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(42.dp))
            Text("Parque Central", style = MaterialTheme.typography.titleMedium)
            Text("Ubicación aproximada del evento", style = MaterialTheme.typography.bodySmall)
        }
    }
}

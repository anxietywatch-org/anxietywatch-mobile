package com.anxietywatch.mobile.ui.wellness

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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class WellnessEvent(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val type: WellnessEventType,
)

enum class WellnessEventType { Crisis, Breathing, ElevatedRhythm }

@Composable
fun PatientDetailScreen(
    patientId: String,
    onEventClick: (String) -> Unit,
) {
    val patientName = if (patientId == "patient-sofia") "Sofía" else "Alex"
    val events = remember {
        listOf(
            WellnessEvent("event-crisis-1", "Crisis detectada", "Pico de ansiedad detectado por el reloj", "Hoy, 14:20", WellnessEventType.Crisis),
            WellnessEvent("event-breathing-1", "Sesión de respiración", "Respiración guiada completada", "Hoy, 11:05", WellnessEventType.Breathing),
            WellnessEvent("event-rhythm-1", "Ritmo elevado", "Frecuencia cardíaca sobre el promedio basal", "Ayer, 19:40", WellnessEventType.ElevatedRhythm),
        )
    }
    var selectedBar by remember { mutableIntStateOf(3) }
    val bars = listOf(78, 84, 91, 102)
    val labels = listOf("08:00", "12:00", "16:00", "Ahora")

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(patientName, style = MaterialTheme.typography.headlineLarge)
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Estable", modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) }
        Spacer(Modifier.height(24.dp))
        Text("Frecuencia cardíaca", style = MaterialTheme.typography.titleLarge)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BPM por franja horaria", style = MaterialTheme.typography.bodySmall)
                HeartRateBarChart(
                    values = bars,
                    labels = labels,
                    selectedIndex = selectedBar,
                    onSelected = { selectedBar = it },
                )
            }
        }
        Text("Eventos recientes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        events.forEach { event ->
            EventRow(event = event, onClick = { onEventClick(event.id) })
        }
    }
}

@Composable
private fun HeartRateBarChart(
    values: List<Int>,
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.primaryContainer
    Row(modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { index, value ->
            Column(
                modifier = Modifier.weight(1f).fillMaxSize().clickable { onSelected(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (selectedIndex == index) {
                    Text("$value BPM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Canvas(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val maxHeight = size.height * (value / 120f)
                    drawRoundRect(
                        color = if (selectedIndex == index) selectedColor else unselectedColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - maxHeight),
                        size = androidx.compose.ui.geometry.Size(size.width, maxHeight),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                }
                Text(labels[index], style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EventRow(event: WellnessEvent, onClick: () -> Unit) {
    val (icon, tint) = when (event.type) {
        WellnessEventType.Crisis -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        WellnessEventType.Breathing -> Icons.Default.Air to MaterialTheme.colorScheme.tertiary
        WellnessEventType.ElevatedRhythm -> Icons.Default.DirectionsRun to MaterialTheme.colorScheme.secondary
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = tint.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                androidx.compose.material3.Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(9.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Text(event.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(event.time, style = MaterialTheme.typography.labelSmall)
        }
    }
}

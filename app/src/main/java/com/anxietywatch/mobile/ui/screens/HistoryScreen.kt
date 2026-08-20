package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MonitorHeart
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.DashboardSummary
import com.anxietywatch.mobile.network.EpisodeSummary
import com.anxietywatch.mobile.network.NetworkModule
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    var isLoading by remember { mutableStateOf(true) }
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var episodes by remember { mutableStateOf<List<EpisodeSummary>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val breathingDates = NetworkModule.getSessionManager().getBreathingSessionDates()
    val sleepHours = NetworkModule.getSessionManager().getSleepHours()

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

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text(text = "Historial de salud", style = MaterialTheme.typography.headlineMedium)

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
        }

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
        }

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

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Ejercicios de respiración (últimos 7 días)", style = MaterialTheme.typography.titleMedium)
                if (breathingDates.isEmpty()) {
                    Text(
                        text = "Aún no has completado ninguna sesión. Se irá llenando conforme uses la app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    WeeklyBreathingChart(breathingDates)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Horas de sueño habituales", style = MaterialTheme.typography.titleMedium)
                if (sleepHours.isNullOrBlank()) {
                    Text(
                        text = "Aún no registras tus horas de sueño. Puedes agregarlas en tu Perfil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(text = "$sleepHours horas por noche", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        Text(text = "Eventos recientes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))

        if (!isLoading && errorMessage == null && episodes.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Filled.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Text(
                    text = "Aún no tienes episodios registrados",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "Cuando tu reloj esté conectado y registres actividad, tus eventos aparecerán aquí con detalle completo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        for (episode in episodes) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = episode.date ?: "Fecha no disponible", style = MaterialTheme.typography.labelLarge)
                    episode.severity?.let { Text(text = "Severidad: $it", style = MaterialTheme.typography.bodyMedium) }
                    episode.durationMinutes?.let { Text(text = "Duración: $it min", style = MaterialTheme.typography.bodySmall) }
                    episode.notes?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
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

@Composable
private fun WeeklyBreathingChart(sessionDates: List<String>) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayLabelFormat = SimpleDateFormat("EEE", Locale("es"))
    val today = Calendar.getInstance()

    val days = (6 downTo 0).map { offset ->
        val cal = today.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, -offset)
        cal
    }
    val counts = days.map { cal -> sessionDates.count { it == sdf.format(cal.time) } }
    val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in days.indices) {
            val count = counts[i]
            val heightFraction = if (count == 0) 0.05f else count.toFloat() / maxCount
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height((80 * heightFraction).dp)
                        .background(
                            color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Text(
                    text = dayLabelFormat.format(days[i].time).take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
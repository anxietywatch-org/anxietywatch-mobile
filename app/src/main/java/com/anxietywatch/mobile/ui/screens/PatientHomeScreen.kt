package com.anxietywatch.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anxietywatch.mobile.network.DashboardSummary
import com.anxietywatch.mobile.network.NetworkModule
import retrofit2.HttpException

@Composable
fun PatientHomeScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenNotifications: () -> Unit,
    avatarUri: String?,
    onAvatarClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            summary = NetworkModule.api.getDashboardSummary()
        } catch (e: HttpException) {
            errorMessage = "Error del servidor: código ${e.code()}"
        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopHeaderBar(avatarUri = avatarUri, onAvatarClick = onAvatarClick, onNotificationsClick = onOpenNotifications)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HeartRateGaugeCard()

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard(Modifier.weight(1f), Icons.Filled.Air, "Respiración", "—", "rpm")
                MiniStatCard(Modifier.weight(1f), Icons.Filled.Bedtime, "Sueño", "—", "hrs")
            }

            Text(text = "Acciones rápidas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))

            QuickActionRow(icon = Icons.Filled.SelfImprovement, label = "Relajarme", onClick = { })
            QuickActionRow(icon = Icons.Filled.History, label = "Historial", onClick = onOpenHistory)
            QuickActionRow(icon = Icons.Filled.Settings, label = "Ajustes", onClick = onOpenSettings)

            Text(text = "Tu resumen", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))

            if (isLoading) CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))

            summary?.let { data ->
                SummaryCard("Nivel de ansiedad", "${data.anxietyLevel.current}", "Tendencia: ${data.anxietyLevel.trend}")
                SummaryCard("Registros esta semana", "${data.weeklyRecords.used} / ${data.weeklyRecords.limit}", "Usados de tu límite semanal")
                SummaryCard("Racha", "${data.streakDays} días", "Días consecutivos de seguimiento")
                SummaryCard("Ejercicios completados", "${data.exercisesCompleted}", "Total acumulado")
            }

            errorMessage?.let { Text(text = it, modifier = Modifier.padding(top = 16.dp)) }

            Box(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TopHeaderBar(avatarUri: String?, onAvatarClick: () -> Unit, onNotificationsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "AnxietyWatch", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notificaciones",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp).clickable(onClick = onNotificationsClick)
            )
            Box(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onAvatarClick)
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = Uri.parse(avatarUri),
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeartRateGaugeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Frecuencia Cardíaca", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier.padding(top = 16.dp).size(140.dp).background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "—", style = MaterialTheme.typography.headlineLarge)
                    Text(text = "BPM", style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(text = "Sin datos del reloj aún", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
            Text(
                text = "Vincula tu Galaxy Watch7 para ver tu ritmo cardíaco en tiempo real.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun MiniStatCard(modifier: Modifier = Modifier, icon: ImageVector, label: String, value: String, unit: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(text = label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
            }
            Text(text = "$value $unit", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun QuickActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
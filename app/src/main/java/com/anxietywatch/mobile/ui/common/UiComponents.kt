package com.anxietywatch.mobile.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(
    title: String,
    eyebrow: String? = null,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        eyebrow?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(title, style = MaterialTheme.typography.headlineLarge)
        description?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String? = null,
    detail: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                Text(value, style = MaterialTheme.typography.headlineMedium)
                unit?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
            }
            detail?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

enum class StatusTone { Neutral, Positive, Warning, Critical }

enum class ConnectivityStatus {
    ConnectedRecent,
    ConnectedStale,
    Disconnected,
    Unknown,
}

@Composable
fun StatusBadge(
    label: String,
    tone: StatusTone = StatusTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val (containerColor, contentColor) = when (tone) {
        StatusTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        StatusTone.Positive -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        StatusTone.Warning -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        StatusTone.Critical -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
fun DataFreshnessLabel(
    lastUpdated: String,
    modifier: Modifier = Modifier,
) {
    Text(
        "Última actualización: $lastUpdated",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}

@Composable
fun ConnectivityCard(
    status: ConnectivityStatus,
    lastSync: String,
    deviceName: String? = null,
    modifier: Modifier = Modifier,
) {
    val (statusLabel, tone) = when (status) {
        ConnectivityStatus.ConnectedRecent -> "Conectado · reciente" to StatusTone.Positive
        ConnectivityStatus.ConnectedStale -> "Conectado · datos antiguos" to StatusTone.Warning
        ConnectivityStatus.Disconnected -> "Desconectado" to StatusTone.Warning
        ConnectivityStatus.Unknown -> "Sin información" to StatusTone.Neutral
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Conectividad", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(deviceName ?: "Reloj no vinculado")
                StatusBadge(label = statusLabel, tone = tone)
            }
            DataFreshnessLabel(lastSync)
        }
    }
}

@Composable
fun PatientRow(
    name: String,
    status: String,
    heartRate: Int?,
    lastSync: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { role = Role.Button },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                StatusBadge(label = status)
            }
            Text(
                heartRate?.let { "$it BPM" } ?: "Sin lectura de BPM",
                style = MaterialTheme.typography.headlineSmall,
            )
            DataFreshnessLabel(lastSync)
        }
    }
}

@Composable
fun AlertRow(
    title: String,
    patientName: String,
    occurredAt: String,
    status: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { role = Role.Button },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusBadge(label = status, tone = StatusTone.Warning)
            }
            Text(patientName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DataFreshnessLabel(occurredAt)
        }
    }
}

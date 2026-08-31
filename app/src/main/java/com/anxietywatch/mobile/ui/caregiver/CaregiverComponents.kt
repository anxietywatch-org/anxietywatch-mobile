package com.anxietywatch.mobile.ui.caregiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedTextField

enum class CaregiverDestination {
    Home,
    Patients,
    Alerts,
    Profile,
}

@Composable
fun CaregiverTopBar(
    title: String,
    subtitle: String? = null,
    onProfileClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = if (onBack == null) 8.dp else 0.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onProfileClick != null) {
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Default.Person, contentDescription = "Abrir perfil de cuidador")
            }
        }
    }
}

@Composable
fun CaregiverBottomBar(
    selected: CaregiverDestination,
    onDestinationSelected: (CaregiverDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        CaregiverDestination.entries.forEach { destination ->
            val (icon, label) = when (destination) {
                CaregiverDestination.Home -> Icons.Default.Home to "Home"
                CaregiverDestination.Patients -> Icons.Default.People to "Patients"
                CaregiverDestination.Alerts -> Icons.Default.NotificationsNone to "Alerts"
                CaregiverDestination.Profile -> Icons.Default.Person to "Profile"
            }
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
fun PatientCard(
    displayName: String,
    bpm: Int? = null,
    lastUpdated: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { role = Role.Button },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    Icons.Default.PersonOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    bpm?.let { "$it BPM" } ?: "Sin lectura reciente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (bpm == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                lastUpdated?.let {
                    Text("Última medición: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir detalle")
        }
    }
}

@Composable
fun CaregiverSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("Buscar paciente") },
        singleLine = true,
    )
}

@Composable
fun CaregiverSummaryCard(
    patientCount: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Pacientes vinculados", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(patientCount.toString(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Relaciones aceptadas en tu cuenta", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

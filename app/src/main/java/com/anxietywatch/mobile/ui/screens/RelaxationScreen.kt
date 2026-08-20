package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class RelaxationOption(val icon: ImageVector, val title: String, val subtitle: String, val available: Boolean)

@Composable
fun RelaxationScreen(
    modifier: Modifier = Modifier,
    onOpenBreathing: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenGrounding: () -> Unit,
    onBack: () -> Unit
) {
    val options = listOf(
        RelaxationOption(Icons.Filled.Air, "Respiración guiada", "Técnica 4-4-4 con temporizador", true),
        RelaxationOption(Icons.Filled.Terrain, "Grounding", "Técnica 5-4-3-2-1 paso a paso", true),
        RelaxationOption(Icons.Filled.LibraryMusic, "Sonidos relajantes", "Sonidos de la app o tu música", true),
        RelaxationOption(Icons.Filled.EditNote, "Diario de gratitud", "Próximamente", false)
    )

    Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(text = "Relajarme", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            text = "Elige una técnica para calmar tu mente en este momento.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        for (option in options) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable(enabled = option.available) {
                        when (option.title) {
                            "Respiración guiada" -> onOpenBreathing()
                            "Sonidos relajantes" -> onOpenMusic()
                            "Grounding" -> onOpenGrounding()
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = option.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(text = option.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = option.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (option.available) {
                        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}
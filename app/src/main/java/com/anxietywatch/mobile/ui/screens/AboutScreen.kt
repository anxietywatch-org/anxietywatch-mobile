package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver") }
            Text(text = "Acerca del proyecto", style = MaterialTheme.typography.headlineSmall)
        }

        Text(text = "AnxietyWatch", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp))
        Text(
            text = "AnxietyWatch nació con el objetivo de ayudar a personas que viven con ansiedad a monitorear su bienestar en tiempo real, y de darle a sus cuidadores y familiares una forma de acompañarlas y actuar rápido en caso de una crisis. Combina un reloj inteligente, una app móvil y un sistema de alertas para que nadie tenga que enfrentar la ansiedad solo.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = "Desarrollado por:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp)
        )

        Column(
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "Jorge Alberto Rodríguez Enríquez",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "David Rafael Aguilar Solis",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Diana Montoya Rodríguez",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Kevin Hernandez Trejo",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Rafael Hernandez Perez",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = "Versión 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}
package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CaregiverHomeScreen(modifier: Modifier = Modifier, onOpenSettings: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Bienvenido, cuidador", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "El panel completo de pacientes vinculados llegará cuando confirmemos el endpoint real con el backend.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Ajustes")
        }
    }
}
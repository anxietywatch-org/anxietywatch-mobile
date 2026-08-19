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
fun PatientHomeScreen(modifier: Modifier = Modifier, onLogout: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Bienvenido, paciente", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Cerrar sesión")
        }
    }
}
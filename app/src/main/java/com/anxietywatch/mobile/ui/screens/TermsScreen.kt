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
fun TermsScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver") }
            Text(text = "Términos de Servicio", style = MaterialTheme.typography.headlineSmall)
        }

        Text(
            text = "AnxietyWatch es una herramienta de apoyo para el monitoreo de bienestar emocional. No sustituye el diagnóstico, tratamiento o consejo de un profesional de la salud mental o médica calificado.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = "En caso de emergencia real, contacta a los servicios de emergencia de tu localidad o a una línea de crisis. No dependas únicamente de esta app en situaciones de riesgo inmediato.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Al usar esta app, aceptas que la información médica que proporcionas se almacena de forma cifrada, y que puedes solicitar su eliminación en cualquier momento contactando al soporte.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "El vínculo entre paciente y cuidador es voluntario y puede desvincularse en cualquier momento desde los ajustes del dispositivo.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )
    }
}
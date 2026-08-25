package com.anxietywatch.mobile.ui.alerts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.ui.events.LocationPlaceholderCard

@Composable
fun CriticalAlertScreen(
    eventId: String,
    onViewGuide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Warning, contentDescription = "Alerta crítica", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp).height(64.dp))
        Text("Alex está en crisis", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onError, textAlign = TextAlign.Center)
        Text(
            "El reloj detectó una actividad elevada. Revisa su ubicación y contacta con ella.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(top = 8.dp),
        )
        LocationPlaceholderCard()
        Button(
            onClick = {
                // ACTION_DIAL abre el marcador y requiere confirmación explícita del cuidador.
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onError,
                contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) { Text("Llamar") }
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Ver ubicación", color = MaterialTheme.colorScheme.onError) }
        OutlinedButton(onClick = onViewGuide, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Ver guía", color = MaterialTheme.colorScheme.onError) }
        Spacer(Modifier.height(12.dp))
        Text(
            "Omitir alerta si ya estás con ella",
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(8.dp),
        )
        OutlinedButton(onClick = onDismiss) { Text("Omitir alerta", color = MaterialTheme.colorScheme.onError) }
        Text("ID del evento: $eventId", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(top = 8.dp))
    }
}

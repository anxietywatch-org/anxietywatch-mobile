package com.anxietywatch.mobile.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CriticalAlertScreen(
    modifier: Modifier = Modifier,
    patientName: String,
    onCallPatient: () -> Unit,
    onOpenGuide: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var actionTaken by remember { mutableStateOf(false) }

    // Sonido distintivo real al entrar a la pantalla (tono de notificación del sistema).
    LaunchedEffect(Unit) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, uri)?.play()
        } catch (e: Exception) {
            // Si el dispositivo no permite reproducir el tono, no bloqueamos la alerta.
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFB3261E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlertIcon(Modifier.padding(top = 48.dp))

        Text(
            text = "$patientName necesita tu atención",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = "Se ha confirmado un episodio de crisis. Actúa lo antes posible.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )

        Button(
            onClick = {
                actionTaken = true
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
                onCallPatient()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFB3261E)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp)
        ) {
            androidx.compose.material3.Icon(imageVector = Icons.Filled.Call, contentDescription = null)
            Text(text = "Llamar", modifier = Modifier.padding(start = 8.dp))
        }

        Button(
            onClick = {
                actionTaken = true
                onOpenGuide()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1913), contentColor = Color.White),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            androidx.compose.material3.Icon(imageVector = Icons.Filled.MenuBook, contentDescription = null)
            Text(text = "Ver guía de apoyo", modifier = Modifier.padding(start = 8.dp))
        }

        if (actionTaken) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) {
                Text("Ya atendí esta alerta")
            }
        } else {
            Text(
                text = "Toma una acción antes de poder cerrar esta alerta.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@Composable
private fun AlertIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.size(72.dp).background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = Color(0xFFB3261E),
            modifier = Modifier.size(40.dp)
        )
    }
}
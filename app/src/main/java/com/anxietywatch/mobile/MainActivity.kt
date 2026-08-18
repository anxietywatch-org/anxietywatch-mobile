package com.anxietywatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.HealthResponse
import com.anxietywatch.mobile.network.NetworkModule
import com.anxietywatch.mobile.ui.theme.AnxietyWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnxietyWatchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HealthCheckScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun HealthCheckScreen(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf<HealthResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = NetworkModule.api.getHealth()
            result = response
        } catch (e: Exception) {
            errorMessage = e.message ?: "Error desconocido al conectar con el servidor"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "AnxietyWatch - Prueba de conexión real", style = MaterialTheme.typography.titleLarge)

        when {
            isLoading -> {
                Text(text = "Conectando con api.mangoon.xyz...")
                CircularProgressIndicator()
            }
            result != null -> {
                Text(text = "Estado: ${result!!.status}")
                Text(text = "Servicio: ${result!!.service}")
                Text(text = "Versión: ${result!!.version}")
                Text(text = "Timestamp: ${result!!.timestamp}")
            }
            errorMessage != null -> {
                Text(text = "Error real de conexión: $errorMessage")
            }
        }
    }
}
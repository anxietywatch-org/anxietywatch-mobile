package com.anxietywatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.AcceptCodeRequest
import com.anxietywatch.mobile.network.DeviceIdProvider
import com.anxietywatch.mobile.network.NetworkModule
import com.anxietywatch.mobile.ui.theme.AnxietyWatchTheme
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnxietyWatchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TokenEntryScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TokenEntryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Ingresa tu código de vinculación", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Código (ej. AW-80JW-NOBB-MOW8)") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        Button(
            onClick = {
                isLoading = true
                resultMessage = null
                scope.launch {
                    try {
                        val deviceId = DeviceIdProvider.getDeviceId(context)
                        val response = NetworkModule.api.acceptByCode(
                            AcceptCodeRequest(code = code, deviceId = deviceId)
                        )
                        NetworkModule.getSessionManager().saveSession(
                            token = response.token,
                            expiresAt = response.expiresAt,
                            userRole = response.user.role
                        )
                        resultMessage = "Vinculado con éxito. Rol: ${response.user.role}, Nombre: ${response.user.fullName}"
                    } catch (e: HttpException) {
                        resultMessage = when (e.code()) {
                            404 -> "Código inválido."
                            409 -> "Este código ya fue usado o ya no está disponible."
                            410 -> "Este código ha expirado."
                            else -> "Error del servidor: código ${e.code()}"
                        }
                    } catch (e: Exception) {
                        resultMessage = "Error de conexión: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Vincular")
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        resultMessage?.let {
            Text(text = it, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
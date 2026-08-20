package com.anxietywatch.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.AcceptCodeRequest
import com.anxietywatch.mobile.network.DeviceIdProvider
import com.anxietywatch.mobile.network.NetworkModule
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val MAX_ALNUM_CHARS = 14
private val GROUP_SIZES = listOf(2, 4, 4, 4)

private fun formatVinculationCode(raw: String): String {
    val alnum = raw.uppercase().filter { it.isLetterOrDigit() }.take(MAX_ALNUM_CHARS)
    val groups = mutableListOf<String>()
    var index = 0
    for (size in GROUP_SIZES) {
        if (index >= alnum.length) break
        val end = minOf(index + size, alnum.length)
        groups.add(alnum.substring(index, end))
        index = end
    }
    return groups.joinToString("-")
}

@Composable
fun TokenEntryScreen(modifier: Modifier = Modifier, onLinkSuccess: () -> Unit) {
    val context = LocalContext.current
    var fieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp).padding(top = 32.dp)
        )
        Text(
            text = "Ingresa tu código de vinculación",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Conecta de forma segura tu cuenta usando el código que recibiste.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        val formatted = formatVinculationCode(newValue.text)
                        fieldValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                    },
                    label = { Text("Código (ej. AW-80JW-NOBB-MOW8)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        isLoading = true
                        resultMessage = null
                        scope.launch {
                            try {
                                val deviceId = DeviceIdProvider.getDeviceId(context)
                                val response = NetworkModule.api.acceptByCode(
                                    AcceptCodeRequest(code = fieldValue.text, deviceId = deviceId)
                                )
                                NetworkModule.getSessionManager().saveSession(
                                    token = response.token,
                                    expiresAt = response.expiresAt,
                                    userRole = response.user.role,
                                    userFullName = response.user.fullName
                                )
                                onLinkSuccess()
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
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Vincular")
                }

                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                }

                resultMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row_SecurityNote()
        }
    }
}

@Composable
private fun Row_SecurityNote() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = "Conexión cifrada de extremo a extremo", style = MaterialTheme.typography.labelLarge)
        Text(
            text = "Tu información se protege con cifrado de nivel bancario.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
package com.anxietywatch.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anxietywatch.mobile.network.NetworkModule
import org.json.JSONArray
import retrofit2.HttpException

data class TokenSummary(val status: String, val code: String?, val patientName: String?)

private fun parseTokens(json: String): List<TokenSummary> {
    return try {
        val array = JSONArray(json)
        val list = mutableListOf<TokenSummary>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                TokenSummary(
                    status = obj.optString("status", "desconocido"),
                    code = if (obj.has("code")) obj.optString("code") else null,
                    patientName = if (obj.has("linkedUserFullName")) obj.optString("linkedUserFullName") else null
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun CaregiverHomeScreen(
    modifier: Modifier = Modifier,
    avatarUri: String?,
    onAvatarClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    onGenerateCode: () -> Unit = {},
    onOpenPatientDetail: (String) -> Unit = {},
    onViewPendingCode: (TokenSummary) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(true) }
    var tokens by remember { mutableStateOf<List<TokenSummary>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val raw = NetworkModule.api.getTokensRaw().string()
            tokens = parseTokens(raw)
        } catch (e: HttpException) {
            errorMessage = "Error del servidor: código ${e.code()}"
        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Los pacientes activos van primero (ordenados por prioridad de atención real
    // requeriría el campo de estado por paciente, aún no confirmado por el backend --
    // por ahora ordenamos aceptados antes que pendientes, que es el dato real que sí tenemos).
    val sortedTokens = tokens.sortedBy { it.status.equals("pending", ignoreCase = true) }
    val filteredTokens = if (searchQuery.isBlank()) {
        sortedTokens
    } else {
        sortedTokens.filter { (it.patientName ?: "").contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "AnxietyWatch", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notificaciones",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp).clickable(onClick = onOpenNotifications)
                )
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onAvatarClick)
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = Uri.parse(avatarUri),
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Text(text = "Mis pacientes", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Aquí verás a las personas que has vinculado y podrás acompañarlas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            if (tokens.size > 3) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar paciente") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }

            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }

            if (!isLoading && errorMessage == null && filteredTokens.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Filled.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Text(
                            text = if (tokens.isEmpty()) "Aún no tienes pacientes vinculados" else "Sin resultados para \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        if (tokens.isEmpty()) {
                            Text(
                                text = "Genera un código y compártelo con la persona que quieres acompañar.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            for (token in filteredTokens) {
                val isPending = token.status.equals("pending", ignoreCase = true)
                val name = token.patientName ?: if (isPending) "Vínculo pendiente" else "Paciente vinculado"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clickable {
                            if (isPending) onViewPendingCode(token) else onOpenPatientDetail(name)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (isPending) "Toca para ver el código de nuevo" else "Estado: ${token.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }
            }

            Button(
                onClick = onGenerateCode,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Text(text = "Generar código de vinculación", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
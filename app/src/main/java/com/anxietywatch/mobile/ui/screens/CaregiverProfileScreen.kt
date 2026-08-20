package com.anxietywatch.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anxietywatch.mobile.network.MedicalProfileUpdate
import com.anxietywatch.mobile.network.NetworkModule
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun CaregiverProfileScreen(
    modifier: Modifier = Modifier,
    avatarUri: String?,
    onAvatarChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    val session = NetworkModule.getSessionManager()
    val scope = rememberCoroutineScope()

    var showPhotoDialog by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf(session.getFullName() ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    if (showPhotoDialog) {
        ProfilePhotoPickerDialog(
            onDismiss = { showPhotoDialog = false },
            onPhotoSelected = { uri ->
                session.saveAvatarUri(uri.toString())
                onAvatarChanged(uri.toString())
                showPhotoDialog = false
            },
            onSkip = { showPhotoDialog = false }
        )
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver") }
            Text(text = "Perfil", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { showPhotoDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = Uri.parse(avatarUri),
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
                }
            }
            Text(
                text = "Toca la foto para cambiarla",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = filterName(it) },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }
            savedMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp)) }

            Button(
                onClick = {
                    isSaving = true
                    errorMessage = null
                    savedMessage = null
                    scope.launch {
                        try {
                            NetworkModule.api.updateProfile(MedicalProfileUpdate(fullName = fullName))
                            savedMessage = "Cambios guardados con éxito."
                        } catch (e: HttpException) {
                            errorMessage = "Error ${e.code()}: ${e.response()?.errorBody()?.string()}"
                        } catch (e: Exception) {
                            errorMessage = "No se pudo guardar: ${e.message}"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Guardar cambios")
            }
        }
    }
}
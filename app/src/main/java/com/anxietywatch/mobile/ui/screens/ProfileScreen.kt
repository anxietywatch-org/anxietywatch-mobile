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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun ProfileScreen(
    modifier: Modifier = Modifier,
    avatarUri: String?,
    onAvatarChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    val session = NetworkModule.getSessionManager()
    val scope = rememberCoroutineScope()

    var showPhotoDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var currentMedications by remember { mutableStateOf("") }
    var previousDiagnosis by remember { mutableStateOf(false) }
    var treatingProfessional by remember { mutableStateOf("") }

    var age by remember { mutableStateOf(session.getAge() ?: "") }
    var gender by remember { mutableStateOf(session.getGender() ?: "") }
    var heightCm by remember { mutableStateOf(session.getHeightCm() ?: "") }
    var weightKg by remember { mutableStateOf(session.getWeightKg() ?: "") }

    var baselineAnxiety by remember { mutableStateOf(session.getBaselineAnxiety() ?: "") }
    var triggers by remember { mutableStateOf(session.getTriggers() ?: "") }
    var relaxationTechnique by remember { mutableStateOf(session.getRelaxationTechnique() ?: "") }
    var sleepHours by remember { mutableStateOf(session.getSleepHours() ?: "") }

    val linkedWatch = session.getLinkedWatchAddress()

    val pickContact = rememberContactPickerLauncher { picked ->
        emergencyContactName = picked.name
        emergencyContactPhone = picked.phone
    }

    LaunchedEffect(Unit) {
        try {
            val profile = NetworkModule.api.getProfile()
            emergencyContactName = profile.emergencyContactName ?: ""
            emergencyContactPhone = profile.emergencyContactPhone ?: ""
            allergies = profile.allergies ?: ""
            currentMedications = profile.currentMedications ?: ""
            previousDiagnosis = profile.previousAnxietyDiagnosis ?: false
            treatingProfessional = profile.treatingProfessional ?: ""
        } catch (e: Exception) {
            errorMessage = "No se pudo cargar tu perfil: ${e.message}"
        } finally {
            isLoading = false
        }
    }

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

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(text = "Perfil", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
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
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 24.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            SectionCard(title = "Información personal") {
                OutlinedTextField(age, { age = filterNumeric(it) }, label = { Text("Edad") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(gender, { gender = filterName(it) }, label = { Text("Género") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(heightCm, { heightCm = filterNumeric(it) }, label = { Text("Estatura (cm)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(weightKg, { weightKg = filterNumeric(it) }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth())
            }

            SectionCard(title = "Contacto de emergencia") {
                OutlinedTextField(emergencyContactName, { emergencyContactName = filterName(it) }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(emergencyContactPhone, { emergencyContactPhone = filterPhone(it) }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                Button(onClick = pickContact, modifier = Modifier.fillMaxWidth()) { Text("Elegir desde mis contactos") }
            }

            SectionCard(title = "Salud") {
                OutlinedTextField(allergies, { allergies = it }, label = { Text("Alergias") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(currentMedications, { currentMedications = it }, label = { Text("Medicamentos actuales") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(treatingProfessional, { treatingProfessional = filterName(it) }, label = { Text("Médico o terapeuta tratante") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Diagnóstico previo de ansiedad", modifier = Modifier.weight(1f))
                    Switch(checked = previousDiagnosis, onCheckedChange = { previousDiagnosis = it })
                }
            }

            SectionCard(title = "Hábitos y bienestar") {
                OutlinedTextField(baselineAnxiety, { baselineAnxiety = it }, label = { Text("Nivel de ansiedad habitual") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(triggers, { triggers = it }, label = { Text("Desencadenantes comunes") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(relaxationTechnique, { relaxationTechnique = it }, label = { Text("Técnica de relajación preferida") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(sleepHours, { sleepHours = filterNumeric(it) }, label = { Text("Horas de sueño habituales") }, modifier = Modifier.fillMaxWidth())
            }

            SectionCard(title = "Dispositivo") {
                Text(
                    text = if (linkedWatch != null) "Reloj vinculado: $linkedWatch" else "Ningún reloj vinculado todavía",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }
            savedMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp)) }

            Button(
                onClick = {
                    isSaving = true
                    errorMessage = null
                    savedMessage = null
                    session.saveLocalProfileExtras(age, gender, heightCm, weightKg)
                    session.saveWellnessExtras(baselineAnxiety, triggers, relaxationTechnique, sleepHours)
                    scope.launch {
                        try {
                            NetworkModule.api.updateProfile(
                                MedicalProfileUpdate(
                                    fullName = session.getFullName() ?: "",
                                    allergies = allergies.ifBlank { null },
                                    currentMedications = currentMedications.ifBlank { null },
                                    emergencyContactName = emergencyContactName.ifBlank { null },
                                    emergencyContactPhone = emergencyContactPhone.ifBlank { null },
                                    previousAnxietyDiagnosis = previousDiagnosis,
                                    treatingProfessional = treatingProfessional.ifBlank { null }
                                )
                            )
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
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Guardar cambios")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}
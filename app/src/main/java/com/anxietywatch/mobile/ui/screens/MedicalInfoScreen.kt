package com.anxietywatch.mobile.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.anxietywatch.mobile.network.MedicalProfileUpdate
import com.anxietywatch.mobile.network.NetworkModule
import kotlinx.coroutines.launch
import retrofit2.HttpException

private enum class MedicalStep { PERSONAL, CONTACT, HEALTH, WELLNESS }
private val GENDER_OPTIONS = listOf("Femenino", "Masculino", "No binario", "Prefiero no decirlo")
private val ANXIETY_LEVEL_OPTIONS = listOf("Bajo", "Moderado", "Alto")
private val RELAXATION_OPTIONS = listOf("Respiración guiada", "Meditación", "Ejercicio físico", "Música", "Escritura/diario", "Otra")

@Composable
private fun DropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = selectedValue.ifBlank { "Selecciona una opción" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in options) {
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MedicalInfoScreen(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    var step by remember { mutableStateOf(MedicalStep.PERSONAL) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }

    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }

    var allergies by remember { mutableStateOf("") }
    var currentMedications by remember { mutableStateOf("") }
    var previousDiagnosis by remember { mutableStateOf(false) }
    var treatingProfessional by remember { mutableStateOf("") }

    var baselineAnxiety by remember { mutableStateOf("") }
    var triggers by remember { mutableStateOf("") }
    var relaxationTechnique by remember { mutableStateOf("") }
    var sleepHours by remember { mutableStateOf("") }

    val pickContact = rememberContactPickerLauncher { picked ->
        emergencyContactName = picked.name
        emergencyContactPhone = picked.phone
    }

    fun goNext(next: () -> Unit) {
        when (step) {
            MedicalStep.PERSONAL -> {
                NetworkModule.getSessionManager().saveLocalProfileExtras(age, gender, heightCm, weightKg)
                next()
            }
            MedicalStep.WELLNESS -> {
                NetworkModule.getSessionManager().saveWellnessExtras(baselineAnxiety, triggers, relaxationTechnique, sleepHours)
                NetworkModule.getSessionManager().setMedicalInfoDone()
                next()
            }
            else -> {
                isSaving = true
                errorMessage = null
                scope.launch {
                    try {
                        NetworkModule.api.updateProfile(
                            MedicalProfileUpdate(
                                fullName = NetworkModule.getSessionManager().getFullName() ?: "",
                                allergies = allergies.ifBlank { null },
                                currentMedications = currentMedications.ifBlank { null },
                                emergencyContactName = emergencyContactName.ifBlank { null },
                                emergencyContactPhone = emergencyContactPhone.ifBlank { null },
                                previousAnxietyDiagnosis = previousDiagnosis,
                                treatingProfessional = treatingProfessional.ifBlank { null }
                            )
                        )
                        next()
                    } catch (e: HttpException) {
                        errorMessage = "Error ${e.code()}: ${e.response()?.errorBody()?.string()}"
                    } catch (e: Exception) {
                        errorMessage = "No se pudo guardar: ${e.message}"
                    } finally {
                        isSaving = false
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        StepDots(current = step)

        Text(
            text = when (step) {
                MedicalStep.PERSONAL -> "Información personal"
                MedicalStep.CONTACT -> "Contacto de emergencia"
                MedicalStep.HEALTH -> "Salud y profesional tratante"
                MedicalStep.WELLNESS -> "Hábitos y bienestar"
            },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Esta información nos ayuda a cuidarte mejor. Puedes completarla ahora o después desde tu perfil.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                when (step) {
                    MedicalStep.PERSONAL -> {
                        OutlinedTextField(age, { age = filterNumeric(it) }, label = { Text("Edad") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                        DropdownField("Género", gender, GENDER_OPTIONS, { gender = it }, modifier = Modifier.padding(bottom = 12.dp))
                        OutlinedTextField(heightCm, { heightCm = filterNumeric(it) }, label = { Text("Estatura (cm)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                        OutlinedTextField(weightKg, { weightKg = filterNumeric(it) }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth())
                    }
                    MedicalStep.CONTACT -> {
                        OutlinedTextField(emergencyContactName, { emergencyContactName = filterName(it) }, label = { Text("Nombre del contacto") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                        OutlinedTextField(emergencyContactPhone, { emergencyContactPhone = filterPhone(it) }, label = { Text("Teléfono del contacto") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                        TextButton(onClick = pickContact, modifier = Modifier.fillMaxWidth()) { Text("Elegir desde mis contactos") }
                    }
                    MedicalStep.HEALTH -> {
                        OutlinedTextField(allergies, { allergies = it }, label = { Text("Alergias") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                        OutlinedTextField(currentMedications, { currentMedications = it }, label = { Text("Medicamentos actuales") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                        OutlinedTextField(treatingProfessional, { treatingProfessional = filterName(it) }, label = { Text("Médico o terapeuta tratante") }, modifier = Modifier.fillMaxWidth())
                    }
                    MedicalStep.WELLNESS -> {
                        DropdownField("Nivel de ansiedad habitual", baselineAnxiety, ANXIETY_LEVEL_OPTIONS, { baselineAnxiety = it }, modifier = Modifier.padding(bottom = 12.dp))
                        OutlinedTextField(triggers, { triggers = it }, label = { Text("Desencadenantes comunes") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                        DropdownField("Técnica de relajación preferida", relaxationTechnique, RELAXATION_OPTIONS, { relaxationTechnique = it }, modifier = Modifier.padding(bottom = 12.dp))
                        OutlinedTextField(sleepHours, { sleepHours = filterNumeric(it) }, label = { Text("Horas de sueño habituales") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }
        if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))

        Button(
            onClick = {
                when (step) {
                    MedicalStep.PERSONAL -> goNext { step = MedicalStep.CONTACT }
                    MedicalStep.CONTACT -> goNext { step = MedicalStep.HEALTH }
                    MedicalStep.HEALTH -> goNext { step = MedicalStep.WELLNESS }
                    MedicalStep.WELLNESS -> goNext { onFinished() }
                }
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) {
            Text(if (step == MedicalStep.WELLNESS) "Finalizar" else "Siguiente")
        }

        TextButton(
            onClick = {
                NetworkModule.getSessionManager().setMedicalInfoDone()
                onFinished()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Completar esto después")
        }
    }
}

@Composable
private fun StepDots(current: MedicalStep) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (s in MedicalStep.entries) {
            val active = s.ordinal <= current.ordinal
            Box(
                modifier = Modifier
                    .size(if (s == current) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        }
    }
}
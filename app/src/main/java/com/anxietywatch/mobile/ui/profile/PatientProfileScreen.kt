package com.anxietywatch.mobile.ui.profile

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(
    onCompleted: () -> Unit,
    onPrivacyPolicyClick: () -> Unit = {},
    viewModel: PatientProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val remoteProfile by viewModel.profile.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var allergies by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf<Boolean?>(null) }
    var professional by remember { mutableStateOf("") }
    var profileLoaded by remember { mutableStateOf(false) }
    var photoUri by remember {
        mutableStateOf(
            context.getSharedPreferences(PHOTO_PREFERENCES, android.content.Context.MODE_PRIVATE)
                .getString(PHOTO_URI_KEY, null)
                ?.let(Uri::parse),
        )
    }
    var photoBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var photoDialogVisible by remember { mutableStateOf(false) }
    var profileMessage by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val photoScope = rememberCoroutineScope()

    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val selected = readContact(context, uri)
                emergencyName = selected.first
                emergencyPhone = selected.second
            }
        }
    }
    val contactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI))
        } else {
            profileMessage = "Permiso de contactos rechazado. Puedes introducir el número manualmente."
        }
    }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoScope.launch {
                val copiedUri = withContext(Dispatchers.IO) { copyGalleryPhoto(context, uri) }
                if (copiedUri != null) {
                    photoUri = copiedUri
                } else {
                    profileMessage = "No se pudo guardar la foto seleccionada."
                }
            }
        }
    }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) {
            photoUri = pendingCameraUri
        } else {
            pendingCameraUri?.let { context.contentResolver.delete(it, null, null) }
        }
    }
    val imagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) galleryPicker.launch("image/*") else profileMessage = "Permiso de imágenes rechazado."
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val values = ContentValues().apply { put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg") }
            pendingCameraUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            pendingCameraUri?.let(cameraCapture::launch)
        } else {
            profileMessage = "Permiso de cámara rechazado."
        }
    }

    LaunchedEffect(Unit) { viewModel.loadProfile() }
    LaunchedEffect(remoteProfile) {
        val profile = remoteProfile ?: return@LaunchedEffect
        if (!profileLoaded) {
            fullName = profile.fullName
            allergies = profile.allergies.orEmpty()
            medications = profile.currentMedications.orEmpty()
            emergencyName = profile.emergencyContactName.orEmpty()
            emergencyPhone = profile.emergencyContactPhone.orEmpty()
            diagnosis = profile.previousAnxietyDiagnosis
            professional = profile.treatingProfessional.orEmpty()
            consent = true
            profileLoaded = true
        }
    }
    LaunchedEffect(photoUri) {
        val preferences = context.getSharedPreferences(PHOTO_PREFERENCES, android.content.Context.MODE_PRIVATE)
        val uri = photoUri
        if (uri == null) {
            preferences.edit().remove(PHOTO_URI_KEY).apply()
            photoBitmap = null
            return@LaunchedEffect
        }

        try {
            val bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }
            if (bitmap == null) throw IOException("La imagen no pudo decodificarse")
            preferences.edit().putString(PHOTO_URI_KEY, uri.toString()).apply()
            photoBitmap = bitmap
        } catch (_: SecurityException) {
            photoUri = null
            photoBitmap = null
            preferences.edit().remove(PHOTO_URI_KEY).apply()
        } catch (_: IOException) {
            photoUri = null
            photoBitmap = null
            preferences.edit().remove(PHOTO_URI_KEY).apply()
        }
    }
    val valid = fullName.trim().length >= 2 &&
        (!profileLoaded || (
            age.toIntOrNull()?.let { it in 1..120 } == true &&
                gender.isNotBlank() &&
                height.toIntOrNull()?.let { it in 50..250 } == true &&
                weight.toDoubleOrNull()?.let { it in 2.0..350.0 } == true
            )) && consent

    LaunchedEffect(uiState) {
        if (uiState is PatientProfileUiState.Success) onCompleted()
    }

    when (val state = uiState) {
        PatientProfileUiState.Idle,
        PatientProfileUiState.Loading,
        -> if (remoteProfile == null) {
            LoadingState("Cargando tu perfil...")
            return
        }
        is PatientProfileUiState.LoadError -> {
            ErrorState(state.message, viewModel::loadProfile)
            return
        }
        else -> Unit
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
    ) {
        Text("Registro de paciente", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Completa tu información para personalizar el monitoreo.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            // TODO: confirmar con backend el mecanismo de subida antes de enviar avatarUrl.
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap!!,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                )
            } else {
                androidx.compose.material3.Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Foto de perfil",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
            }
            TextButton(onClick = { photoDialogVisible = true }, modifier = Modifier.padding(start = 12.dp)) {
                androidx.compose.material3.Icon(Icons.Default.CameraAlt, contentDescription = null)
                Text("Cambiar foto", modifier = Modifier.padding(start = 8.dp))
            }
        }
        ProfileField("Nombre completo", fullName) { fullName = it }
        Text(
            "Edad, género, altura y peso se mantienen localmente en esta sesión; no se envían al backend actual.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        ProfileField("Edad", age) { age = it.filter(Char::isDigit).take(3) }
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded },
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Género") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(genderExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            DropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                listOf("Femenino", "Masculino", "No binario", "Prefiero no decirlo").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { gender = option; genderExpanded = false },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileField(
                "Altura (cm)",
                height,
                Modifier.weight(1f),
            ) { height = it.filter(Char::isDigit).take(3) }
            ProfileField(
                "Peso (kg)",
                weight,
                Modifier.weight(1f),
            ) { weight = it.filter { char -> char.isDigit() || char == '.' }.take(6) }
        }
        Text("Información de salud (opcional)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        ProfileField("Alergias", allergies) { allergies = it.take(1000) }
        ProfileField("Medicamentos actuales", medications) { medications = it.take(2000) }
        ProfileField("Profesional tratante", professional) { professional = it.take(200) }
        Text("Contacto de emergencia", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        ProfileField("Nombre del contacto", emergencyName) { emergencyName = it.take(120) }
        ProfileField("Teléfono del contacto", emergencyPhone) { emergencyPhone = it.take(40) }
        TextButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI))
                } else {
                    contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                }
            },
        ) {
            androidx.compose.material3.Icon(Icons.Default.Contacts, contentDescription = null)
            Text("Elegir desde mis contactos", modifier = Modifier.padding(start = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = diagnosis == true, onCheckedChange = { diagnosis = it })
            Text("Tengo un diagnóstico previo de ansiedad (opcional)")
        }
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 16.dp)) {
            Checkbox(checked = consent, onCheckedChange = { consent = it })
            Column(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                Text("Consiento la recolección y procesamiento de datos de salud")
                Text(
                    "Política de Privacidad",
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.padding(top = 4.dp).clickable(onClick = onPrivacyPolicyClick),
                )
            }
        }
        val error = (uiState as? PatientProfileUiState.Error)?.message
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        Button(
            onClick = {
                viewModel.submit(
                    PatientProfileData(
                        fullName = fullName,
                        age = age,
                        gender = gender,
                        heightCm = height,
                        weightKg = weight,
                        allergies = allergies,
                        currentMedications = medications,
                        emergencyContactName = emergencyName,
                        emergencyContactPhone = emergencyPhone,
                        previousAnxietyDiagnosis = diagnosis,
                        treatingProfessional = professional,
                    ),
                    consent,
                    requireDemographics = !profileLoaded,
                )
            },
            enabled = valid && uiState !is PatientProfileUiState.Loading,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) { Text(if (uiState is PatientProfileUiState.Loading) "Guardando..." else "Continuar") }
        Spacer(Modifier.padding(top = 16.dp))
        Text(
            "Cifrado de extremo a extremo / datos con seguridad AES-256",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (photoDialogVisible) {
        AlertDialog(
            onDismissRequest = { photoDialogVisible = false },
            title = { Text("Foto de perfil") },
            text = { Text("Elige cómo quieres actualizar tu foto.") },
            confirmButton = {
                TextButton(onClick = {
                    photoDialogVisible = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        val values = ContentValues().apply { put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg") }
                        pendingCameraUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        pendingCameraUri?.let(cameraCapture::launch)
                    } else cameraPermission.launch(Manifest.permission.CAMERA)
                }) { Text("Tomar foto") }
            },
            dismissButton = {
                TextButton(onClick = {
                    photoDialogVisible = false
                    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
                    if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        galleryPicker.launch("image/*")
                    } else imagePermission.launch(permission)
                }) { Text("Elegir de galería") }
            },
        )
    }
    if (profileMessage != null) {
        AlertDialog(
            onDismissRequest = { profileMessage = null },
            confirmButton = { TextButton(onClick = { profileMessage = null }) { Text("Aceptar") } },
            text = { Text(profileMessage.orEmpty()) },
        )
    }
}

private const val PHOTO_PREFERENCES = "anxietywatch_local_profile"
private const val PHOTO_URI_KEY = "photo_uri"
private const val PRIVATE_PROFILE_PHOTO = "profile_photo.jpg"

/** Copies a temporary picker grant into app-private storage while the grant is valid. */
private fun copyGalleryPhoto(context: android.content.Context, source: Uri): Uri? {
    return try {
        val target = File(context.filesDir, PRIVATE_PROFILE_PHOTO)
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        Uri.fromFile(target)
    } catch (_: SecurityException) {
        null
    } catch (_: IOException) {
        null
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.padding(bottom = 12.dp),
    )
}

private fun readContact(context: android.content.Context, uri: Uri): Pair<String, String> {
    var name = ""
    var phone = ""
    context.contentResolver.query(
        uri,
        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            name = cursor.getString(1).orEmpty()
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(id),
                null,
            )?.use { phones -> if (phones.moveToFirst()) phone = phones.getString(0).orEmpty() }
        }
    }
    return name to phone
}

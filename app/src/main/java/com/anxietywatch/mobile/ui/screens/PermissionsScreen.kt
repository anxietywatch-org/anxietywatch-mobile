package com.anxietywatch.mobile.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private fun requiredPermissionsPatient(): Array<String> {

    val list = mutableListOf(Manifest.permission.READ_CONTACTS)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        list.add(Manifest.permission.BLUETOOTH_SCAN)
        list.add(Manifest.permission.BLUETOOTH_CONNECT)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.POST_NOTIFICATIONS)
        list.add(Manifest.permission.READ_MEDIA_IMAGES)
        list.add(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    list.add(Manifest.permission.CAMERA)

    // Permiso para enviar SMS durante una crisis
    list.add(Manifest.permission.SEND_SMS)

    return list.toTypedArray()
}

private fun requiredPermissionsCaregiver(): Array<String> {
    val list = mutableListOf(Manifest.permission.READ_CONTACTS)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.POST_NOTIFICATIONS)
        list.add(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    list.add(Manifest.permission.CAMERA)

    return list.toTypedArray()
}

@Composable
fun PermissionsScreen(
    modifier: Modifier = Modifier,
    isCaregiver: Boolean,
    onFinished: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { onFinished() }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Permisos del sistema",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Necesitamos estos permisos para brindarte una experiencia segura y completa.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        if (!isCaregiver) {
            PermissionCard(
                Icons.Filled.Bluetooth,
                "Bluetooth",
                "Vital para conectar tu Galaxy Watch7 y sincronizar tus datos."
            )

            PermissionCard(
                Icons.Filled.Sms,
                "Mensajes SMS",
                "Para avisar por SMS real a tu contacto de emergencia durante una crisis."
            )
        }

        PermissionCard(
            Icons.Filled.Notifications,
            "Notificaciones",
            "Te avisaremos al instante ante eventos importantes."
        )

        PermissionCard(
            Icons.Filled.CameraAlt,
            "Cámara",
            "Para tomar tu foto de perfil directamente desde la app."
        )

        PermissionCard(
            Icons.Filled.Photo,
            "Fotos y archivos multimedia",
            "Para elegir tu foto de perfil desde tu galería."
        )

        PermissionCard(
            Icons.Filled.Contacts,
            "Contactos",
            "Para elegir contactos relevantes directamente de tu agenda."
        )

        Button(
            onClick = {
                val perms = if (isCaregiver) {
                    requiredPermissionsCaregiver()
                } else {
                    requiredPermissionsPatient()
                }

                launcher.launch(perms)
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            Text("Continuar")
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
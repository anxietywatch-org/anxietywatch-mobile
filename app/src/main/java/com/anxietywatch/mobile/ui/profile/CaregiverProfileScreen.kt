package com.anxietywatch.mobile.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anxietywatch.mobile.ui.common.ErrorState
import com.anxietywatch.mobile.ui.common.LoadingState
import com.anxietywatch.mobile.ui.common.ScreenScaffold
import com.anxietywatch.mobile.ui.common.SectionHeader
import com.anxietywatch.mobile.ui.common.StatusBadge

@Composable
fun CaregiverProfileScreen(
    onBack: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {},
    viewModel: CaregiverProfileViewModel? = null,
    state: CaregiverProfileUiState? = null,
    onRetry: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
) {
    val resolved = viewModel ?: if (state == null) hiltViewModel() else null
    val collected by resolved?.uiState?.collectAsState()
        ?: remember { mutableStateOf(state!!) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    ScreenScaffold {
        when (val current = collected) {
            CaregiverProfileUiState.Loading -> LoadingState("Cargando perfil...")
            is CaregiverProfileUiState.Error -> ErrorState(current.message, onRetry ?: resolved?.let { it::retry })
            is CaregiverProfileUiState.Content -> {
                ProfileContent(
                    profile = current.data,
                    isLoggingOut = current.isLoggingOut,
                    logoutError = current.logoutError,
                    onBack = onBack,
                    onLogout = { showLogoutDialog = true },
                )
                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = { Text("¿Quieres desvincular este dispositivo?") },
                        text = { Text("Se cerrará la sesión de cuidador en este dispositivo.") },
                        dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") } },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showLogoutDialog = false
                                    (onLogout ?: { resolved?.logout(onLogoutSuccess) })()
                                },
                            ) { Text("Desvincular") }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: CaregiverProfileUiModel,
    isLoggingOut: Boolean,
    logoutError: String?,
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("Volver al dashboard") }
        SectionHeader("CUIDADOR", "Perfil", "Información disponible en la sesión actual.")
        StatusBadge("Cuidador")
        ProfileRow("Nombre", profile.displayName ?: "Información no disponible")
        ProfileRow("Email", profile.email ?: "Información no disponible")
        ProfileRow("Rol", profile.role)
        logoutError?.let { ErrorState(it) }
        Button(
            onClick = onLogout,
            enabled = !isLoggingOut,
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text(if (isLoggingOut) "Desvinculando..." else "Desvincular") }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        Text(value, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

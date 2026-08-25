package com.anxietywatch.mobile.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * E02: unica puerta de entrada a la app. Sin email, sin password -- solo el codigo
 * AW-XXXX-XXXX-XXXX que la persona recibio (de si misma en otro dispositivo, o de un
 * paciente que la invito como cuidadora).
 */
@Composable
fun TokenEntryScreen(
    onActivated: (role: String) -> Unit,
    showExpiredBanner: Boolean = false,
    viewModel: TokenEntryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var code by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is TokenEntryUiState.Success) {
            onActivated(state.role)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (showExpiredBanner) {
            com.anxietywatch.mobile.ui.session.SessionExpiredMessage()
        }

        Text(
            text = "Ingresa tu código",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Escribe el código que te compartieron. " +
                "No necesitas crear una contraseña.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = code,
            onValueChange = { input ->
                // Filtra en vivo -- mismo criterio (whitelist) que TokenEntryViewModel.sanitize,
                // asi el usuario ve de inmediato que un caracter invalido no se escribio,
                // en vez de descubrirlo hasta que le da a "Continuar".
                code = input.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(20)
                if (uiState is TokenEntryUiState.Error) viewModel.dismissError()
            },
            label = { Text("Código") },
            placeholder = { Text("Código de acceso") },
            singleLine = true,
            isError = uiState is TokenEntryUiState.Error,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.redeem(code) }),
            enabled = uiState !is TokenEntryUiState.Loading,
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState is TokenEntryUiState.Error) {
            Text(
                text = (uiState as TokenEntryUiState.Error).message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Button(
            onClick = { viewModel.redeem(code) },
            enabled = uiState !is TokenEntryUiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            if (uiState is TokenEntryUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            }
            Text(if (uiState is TokenEntryUiState.Loading) "Validando..." else "Continuar")
        }
    }
}

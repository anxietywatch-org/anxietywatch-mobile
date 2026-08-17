package com.anxietywatch.mobile.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.remote.AcceptByCodeRequest
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

sealed interface TokenEntryUiState {
    data object Idle : TokenEntryUiState
    data object Loading : TokenEntryUiState
    data class Success(val role: String) : TokenEntryUiState
    data class Error(val message: String) : TokenEntryUiState
}

/**
 * E02 del backlog: "Ingreso y Activacion por Token. Reemplaza el login tradicional."
 * Un solo campo, un solo boton, sin pantalla de contrasena en ningun momento.
 *
 * NOTA DE SEGURIDAD: esto NO reemplaza validacion del lado del servidor -- el backend
 * SIEMPRE debe volver a validar todo (FluentValidation ya lo hace en TokenRedeemCommand).
 * Esta capa es defensa adicional: normaliza la entrada, rechaza caracteres que no tienen
 * ningun sentido en un codigo de acceso, y limita el largo antes de gastar una llamada de
 * red en algo que obviamente esta mal escrito. La inyeccion SQL en si se previene en el
 * backend con consultas parametrizadas/tipadas (ya es el caso -- ningun repositorio del
 * .NET arma SQL pegando texto), no aqui.
 */
@HiltViewModel
class TokenEntryViewModel @Inject constructor(
    private val api: AnxietyWatchApi,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TokenEntryUiState>(TokenEntryUiState.Idle)
    val uiState: StateFlow<TokenEntryUiState> = _uiState.asStateFlow()

    fun redeem(rawCode: String) {
        val sanitized = sanitize(rawCode)

        if (sanitized.isBlank()) {
            _uiState.update { TokenEntryUiState.Error("Ingresa el código que te compartieron.") }
            return
        }
        if (sanitized.length < MIN_CODE_LENGTH) {
            _uiState.update { TokenEntryUiState.Error("El código es muy corto. Revisa que lo hayas copiado completo.") }
            return
        }
        if (sanitized.length > MAX_CODE_LENGTH) {
            _uiState.update { TokenEntryUiState.Error("Ese código es demasiado largo para ser válido.") }
            return
        }

        _uiState.update { TokenEntryUiState.Loading }
        viewModelScope.launch {
            runCatching {
                val deviceId = sessionRepository.mobileDeviceId()
                api.acceptByCode(AcceptByCodeRequest(code = sanitized, deviceId = deviceId))
            }.onSuccess { response ->
                sessionRepository.saveSession(response.token, response.role, response.expiresAt)
                _uiState.update { TokenEntryUiState.Success(role = response.role) }
            }.onFailure { error ->
                val message = when {
                    error is HttpException && error.code() == 404 ->
                        "Ese código no es válido. Revisa que esté escrito tal cual te lo compartieron."
                    error is HttpException && error.code() == 409 ->
                        "Ese código ya se usó o expiró. Pide uno nuevo a quien te lo compartió."
                    error is HttpException && error.code() == 429 ->
                        "Demasiados intentos. Espera un momento antes de volver a intentar."
                    else -> "No pudimos validar el código. Revisa tu conexión e intenta de nuevo."
                }
                _uiState.update { TokenEntryUiState.Error(message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { TokenEntryUiState.Idle }
    }

    /**
     * Normaliza y filtra la entrada: mayúsculas, sin espacios en los extremos, y SOLO
     * letras/números/guion -- rechaza silenciosamente cualquier otro carácter (comillas,
     * punto y coma, símbolos) en vez de mandarlo tal cual al backend. Whitelist, no
     * blacklist -- es la forma correcta de sanear: en vez de intentar adivinar y bloquear
     * caracteres "peligrosos", solo se permite pasar lo que sabemos que es válido.
     */
    private fun sanitize(rawCode: String): String =
        rawCode.trim().uppercase().filter { it.isLetterOrDigit() || it == '-' }

    private companion object {
        // 4 (si el backend cambia a numérico corto) hasta 20 (deja margen sobre el
        // AW-XXXX-XXXX-XXXX de 15 caracteres actual). Ajustar cuando el backend confirme
        // el formato final.
        const val MIN_CODE_LENGTH = 4
        const val MAX_CODE_LENGTH = 20
    }
}

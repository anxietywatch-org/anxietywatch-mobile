package com.anxietywatch.mobile.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.local.FrontendPreferencesStore
import com.anxietywatch.mobile.data.local.PatientLocalProfilePreferences
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.ProfileResponseDto
import com.anxietywatch.mobile.data.remote.ProfileUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class PatientProfileData(
    val fullName: String,
    val age: String,
    val gender: String,
    val heightCm: String,
    val weightKg: String,
    val allergies: String = "",
    val currentMedications: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val previousAnxietyDiagnosis: Boolean? = null,
    val treatingProfessional: String = "",
)

sealed interface PatientProfileUiState {
    data object Idle : PatientProfileUiState
    data object Loading : PatientProfileUiState
    data object Loaded : PatientProfileUiState
    data object Success : PatientProfileUiState
    data class LoadError(val message: String) : PatientProfileUiState
    data class Error(val message: String) : PatientProfileUiState
}

@HiltViewModel
class PatientProfileViewModel @Inject constructor(
    private val api: AnxietyWatchApi,
    private val frontendPreferences: FrontendPreferencesStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PatientProfileUiState>(PatientProfileUiState.Idle)
    val uiState: StateFlow<PatientProfileUiState> = _uiState.asStateFlow()
    private val _profile = MutableStateFlow<ProfileResponseDto?>(null)
    val profile: StateFlow<ProfileResponseDto?> = _profile.asStateFlow()
    private val _localDemographics = MutableStateFlow<PatientProfileData?>(null)
    val localDemographics: StateFlow<PatientProfileData?> = _localDemographics.asStateFlow()

    init {
        frontendPreferences.patientDemographicsFlow
            .onEach { _localDemographics.value = it.toProfileData() }
            .launchIn(viewModelScope)
    }

    fun loadProfile() {
        Log.d(TAG, "PatientProfile state=LOADING")
        _uiState.update { PatientProfileUiState.Loading }
        viewModelScope.launch {
            runCatching { api.getProfile() }
                .onSuccess {
                    Log.d(TAG, "PatientProfile state=CONTENT")
                    _profile.value = it
                    _uiState.update { PatientProfileUiState.Loaded }
                }
                .onFailure { error ->
                    val message = if (error is HttpException && error.code() == 401) {
                        "Tu sesión expiró. Ingresa nuevamente tu código."
                    } else {
                        "No pudimos cargar tu perfil. Revisa tu conexión e inténtalo de nuevo."
                    }
                    Log.d(TAG, "PatientProfile state=ERROR")
                    _uiState.update { PatientProfileUiState.LoadError(message) }
                }
        }
    }

    fun submit(
        profile: PatientProfileData,
        consentGiven: Boolean,
        requireDemographics: Boolean = true,
    ) {
        val error = when {
            profile.fullName.trim().length !in 2..60 -> "El nombre debe tener entre 2 y 60 caracteres."
            requireDemographics && profile.age.toIntOrNull()?.let { it in 1..120 } != true -> "Ingresa una edad válida."
            requireDemographics && profile.gender.isBlank() -> "Selecciona tu género."
            requireDemographics && profile.heightCm.toIntOrNull()?.let { it in 50..250 } != true -> "Ingresa una altura válida."
            requireDemographics && profile.weightKg.toDoubleOrNull()?.let { it in 2.0..350.0 } != true -> "Ingresa un peso válido."
            !consentGiven -> "Debes aceptar el consentimiento de datos de salud."
            else -> null
        }
        if (error != null) {
            _uiState.update { PatientProfileUiState.Error(error) }
            return
        }

        _uiState.update { PatientProfileUiState.Loading }
        viewModelScope.launch {
            // Age/height/weight no existen en /api/profile: quedan solo en estado local.
            // TODO: hábitos y bienestar tampoco tienen campos confirmados en el backend.
            runCatching {
                frontendPreferences.savePatientDemographics(localPreferencesFrom(profile))
            }.onFailure { error ->
                _uiState.update { PatientProfileUiState.Error(error.message ?: "No se pudo guardar la información local.") }
                return@launch
            }
            _localDemographics.value = profile
            runCatching {
                api.updateProfile(profileUpdateRequestFrom(profile))
            }.onSuccess { updatedProfile ->
                _profile.value = updatedProfile
                _uiState.update { PatientProfileUiState.Success }
            }.onFailure { error ->
                val message = when {
                    error is HttpException && error.code() == 400 ->
                        "Revisa los datos del perfil e inténtalo de nuevo."
                    error is HttpException && error.code() == 401 ->
                        "Tu sesión expiró. Ingresa nuevamente tu código."
                    else -> "No pudimos guardar tu perfil. Revisa tu conexión e inténtalo de nuevo."
                }
                _uiState.update { PatientProfileUiState.Error(message) }
            }
        }
    }

    private companion object {
        const val TAG = "AnxietyWatchUi"
    }
}

internal fun localPreferencesFrom(profile: PatientProfileData) = PatientLocalProfilePreferences(
    age = profile.age.trim().ifBlank { null },
    gender = profile.gender.trim().ifBlank { null },
    heightCm = profile.heightCm.trim().ifBlank { null },
    weightKg = profile.weightKg.trim().ifBlank { null },
)

private fun PatientLocalProfilePreferences.toProfileData() = PatientProfileData(
    fullName = "",
    age = age.orEmpty(),
    gender = gender.orEmpty(),
    heightCm = heightCm.orEmpty(),
    weightKg = weightKg.orEmpty(),
)

internal fun profileUpdateRequestFrom(profile: PatientProfileData): ProfileUpdateRequest =
    ProfileUpdateRequest(
        fullName = profile.fullName.trim(),
        allergies = profile.allergies.trim().ifBlank { null },
        currentMedications = profile.currentMedications.trim().ifBlank { null },
        emergencyContactName = profile.emergencyContactName.trim().ifBlank { null },
        emergencyContactPhone = profile.emergencyContactPhone.trim().ifBlank { null },
        previousAnxietyDiagnosis = profile.previousAnxietyDiagnosis,
        treatingProfessional = profile.treatingProfessional.trim().ifBlank { null },
    )

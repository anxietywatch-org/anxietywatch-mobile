package com.anxietywatch.mobile.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class PatientLocalProfilePreferences(
    val age: String? = null,
    val gender: String? = null,
    val heightCm: String? = null,
    val weightKg: String? = null,
)

interface FrontendPreferencesStore {
    val darkModeFlow: Flow<Boolean?>
    val patientDemographicsFlow: Flow<PatientLocalProfilePreferences>
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun savePatientDemographics(preferences: PatientLocalProfilePreferences)
}

@Singleton
class FrontendPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FrontendPreferencesStore {
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")
    private val ageKey = stringPreferencesKey("patient_local_age")
    private val genderKey = stringPreferencesKey("patient_local_gender")
    private val heightKey = stringPreferencesKey("patient_local_height_cm")
    private val weightKey = stringPreferencesKey("patient_local_weight_kg")

    override val darkModeFlow: Flow<Boolean?> = dataStore.data.map { it[darkModeKey] }

    override val patientDemographicsFlow: Flow<PatientLocalProfilePreferences> = dataStore.data.map {
        PatientLocalProfilePreferences(
            age = it[ageKey],
            gender = it[genderKey],
            heightCm = it[heightKey],
            weightKg = it[weightKey],
        )
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[darkModeKey] = enabled }
    }

    override suspend fun savePatientDemographics(preferences: PatientLocalProfilePreferences) {
        dataStore.edit {
            putNullable(it, ageKey, preferences.age)
            putNullable(it, genderKey, preferences.gender)
            putNullable(it, heightKey, preferences.heightCm)
            putNullable(it, weightKey, preferences.weightKg)
        }
    }

    private fun <T> putNullable(
        preferences: MutablePreferences,
        key: Preferences.Key<T>,
        value: T?,
    ) {
        if (value == null) preferences.remove(key) else preferences[key] = value
    }
}

package com.anxietywatch.mobile.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatientProfileMappingTest {

    @Test
    fun demographicFieldsAreNotSentToRemoteProfileContract() {
        val request = profileUpdateRequestFrom(
            PatientProfileData(
                fullName = " Ana Patient ",
                age = "31",
                gender = "No binario",
                heightCm = "170",
                weightKg = "65",
                allergies = "Polen",
            ),
        )

        assertEquals("Ana Patient", request.fullName)
        assertEquals("Polen", request.allergies)
        assertNull(request.currentMedications)
        assertNull(request.avatarUrl)
    }

    @Test
    fun localDemographicsAreTrimmedAndBlankValuesAreRemoved() {
        val preferences = localPreferencesFrom(
            PatientProfileData(
                fullName = "Ana Patient",
                age = " 31 ",
                gender = " ",
                heightCm = " 170 ",
                weightKg = "",
            ),
        )

        assertEquals("31", preferences.age)
        assertNull(preferences.gender)
        assertEquals("170", preferences.heightCm)
        assertNull(preferences.weightKg)
    }
}

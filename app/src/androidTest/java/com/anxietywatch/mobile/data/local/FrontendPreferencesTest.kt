package com.anxietywatch.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrontendPreferencesTest {
    @Test
    fun preferencesSurviveASecondRepositoryInstance() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val file = context.preferencesDataStoreFile("d2-frontend-preferences-test")
            file.delete()
            val firstStore = PreferenceDataStoreFactory.create { file }
            val first = FrontendPreferences(firstStore)

            first.setDarkMode(true)
            first.savePatientDemographics(
                PatientLocalProfilePreferences(
                    age = "31",
                    gender = "No binario",
                    heightCm = "170",
                    weightKg = "65",
                ),
            )

            // A second repository instance represents a recreated ViewModel/Activity.
            val second = FrontendPreferences(firstStore)
            assertEquals(true, second.darkModeFlow.first())
            assertEquals(
                PatientLocalProfilePreferences("31", "No binario", "170", "65"),
                second.patientDemographicsFlow.first(),
            )

            file.delete()
        }
    }
}

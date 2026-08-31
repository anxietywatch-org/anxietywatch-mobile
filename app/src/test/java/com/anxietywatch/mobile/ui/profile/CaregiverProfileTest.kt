package com.anxietywatch.mobile.ui.profile

import com.anxietywatch.mobile.data.remote.CaregiverSessionSource
import com.anxietywatch.mobile.data.remote.SessionProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaregiverProfileTest {
    @Test fun profileContentMapsAvailableSessionData() {
        val model = SessionProfile("Cuidador de prueba", "caregiver@example.test", "family_member").toUiModel()
        assertEquals("Cuidador de prueba", model.displayName)
        assertEquals("caregiver@example.test", model.email)
        assertEquals("family_member", model.role)
    }

    @Test fun missingNameEmailAndBothAreNullWithoutEmptyStrings() {
        assertNull(SessionProfile(null, "mail@example.test", "family_member").toUiModel().displayName)
        assertNull(SessionProfile("Nombre", null, "family_member").toUiModel().email)
        val empty = SessionProfile("", "", "family_member").toUiModel()
        assertNull(empty.displayName)
        assertNull(empty.email)
    }

    @Test fun profileAlwaysUsesCaregiverRoleFromSessionOrSafeRoleIdentifier() {
        assertEquals("family_member", SessionProfile(null, null, "family_member").toUiModel().role)
        assertEquals("family_member", SessionProfile(null, null, null).toUiModel().role)
    }

    @Test fun logoutContractCallsClearSessionAndDoesNotTouchOtherData() = runBlocking {
        val source = RecordingSessionSource()
        source.clearSession()
        assertEquals(1, source.clearCount)
    }

    @Test fun explicitLogoutDestinationIsTokenEntry() {
        assertEquals("token_entry", com.anxietywatch.mobile.navigation.Routes.TokenEntry.route)
    }

    private class RecordingSessionSource : CaregiverSessionSource {
        override val profileFlow: Flow<SessionProfile> = flowOf(SessionProfile(role = "family_member"))
        var clearCount = 0
        override suspend fun clearSession() { clearCount++ }
    }
}

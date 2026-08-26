package com.anxietywatch.mobile.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiDtosTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun sessionResponseDeserializesBackendShape() {
        val response = json.decodeFromString<AuthenticationResponseDto>(sessionJson)

        assertEquals("test-token", response.token)
        assertEquals("2026-08-30T12:00:00Z", response.expiresAt)
        assertEquals("user-1", response.user.id)
        assertEquals("Paciente de prueba", response.user.fullName)
        assertEquals("patient@example.test", response.user.email)
        assertEquals("free", response.user.planId)
        assertEquals(false, response.user.emailVerified)
        assertNull(response.user.avatarUrl)
        assertEquals("patient", response.user.role)
    }

    @Test
    fun authenticationResponseShapeIsSharedByRegisterAndLogin() {
        val response = json.decodeFromString<AuthenticationResponseDto>(sessionJson)

        assertEquals("test-token", response.token)
        assertEquals("user-1", response.user.id)
        assertEquals("patient", response.user.role)
    }

    @Test
    fun tokenRedeemResponseDeserializesRoleAndUser() {
        val response = json.decodeFromString<TokenRedeemResponseDto>(tokenRedeemJson)

        assertEquals("redeemed-token", response.token)
        assertEquals("2026-08-30T12:00:00Z", response.expiresAt)
        assertEquals("family_member", response.role)
        assertEquals("caregiver-1", response.user.id)
        assertEquals("family_member", response.user.role)
    }

    private companion object {
        const val sessionJson = """
            {
              "token": "test-token",
              "expiresAt": "2026-08-30T12:00:00Z",
              "user": {
                "id": "user-1",
                "fullName": "Paciente de prueba",
                "email": "patient@example.test",
                "planId": "free",
                "emailVerified": false,
                "avatarUrl": null,
                "role": "patient"
              }
            }
        """

        const val tokenRedeemJson = """
            {
              "token": "redeemed-token",
              "expiresAt": "2026-08-30T12:00:00Z",
              "role": "family_member",
              "user": {
                "id": "caregiver-1",
                "fullName": "Cuidador",
                "email": "caregiver@example.test",
                "planId": "free",
                "emailVerified": false,
                "avatarUrl": null,
                "role": "family_member"
              }
            }
        """
    }
}

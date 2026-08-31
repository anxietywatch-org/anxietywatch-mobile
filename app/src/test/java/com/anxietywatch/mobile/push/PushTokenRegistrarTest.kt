package com.anxietywatch.mobile.push

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PushTokenRegistrarTest {
    @Test
    fun authenticatedLogout_unregistersBeforeClearingSession() = runBlocking {
        val calls = mutableListOf<String>()

        logoutWithPushCleanup(
            tokenProvider = { "token-a" },
            unregister = { token -> calls += "unregister:$token" },
            clearSession = { calls += "clear" },
            onUnregisterFailure = { error -> calls += "warning:${error.message}" },
        )

        assertEquals(listOf("unregister:token-a", "clear"), calls)
    }

    @Test
    fun unregisterFailure_stillClearsSession() = runBlocking {
        var cleared = false
        var warned = false

        logoutWithPushCleanup(
            tokenProvider = { "token-a" },
            unregister = { error("offline") },
            clearSession = { cleared = true },
            onUnregisterFailure = { warned = true },
        )

        assertTrue(cleared)
        assertTrue(warned)
    }

    @Test
    fun missingToken_clearsSessionWithoutUnregister() = runBlocking {
        var unregisterCalled = false
        var cleared = false

        logoutWithPushCleanup(
            tokenProvider = { null },
            unregister = { unregisterCalled = true },
            clearSession = { cleared = true },
            onUnregisterFailure = { error -> error("unexpected warning: ${error.message}") },
        )

        assertTrue(cleared)
        assertEquals(false, unregisterCalled)
    }
}

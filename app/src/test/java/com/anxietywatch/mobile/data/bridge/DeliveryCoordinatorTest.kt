package com.anxietywatch.mobile.data.bridge

import com.anxietywatch.mobile.data.local.SyncStatus
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryCoordinatorTest {
    private val deviceId = "123e4567-e89b-12d3-a456-426614174001"
    private val pairing = FakePairing(deviceId, "node-a")

    @Test
    fun `accepted response plus ack success becomes delivered`() = runScenario(
        response = BackendDeliveryResponse("id", accepted = true, duplicate = false),
        ackResult = true,
    ) { states, _, ackCalls ->
        assertEquals(listOf(SyncStatus.BACKEND_DELIVERED_ACK_PENDING, SyncStatus.DELIVERED), states)
        assertEquals(1, ackCalls)
    }

    @Test
    fun `duplicate response is delivered like accepted response`() = runScenario(
        response = BackendDeliveryResponse("id", accepted = false, duplicate = true),
        ackResult = true,
    ) { states, _, _ -> assertEquals(SyncStatus.DELIVERED, states.last()) }

    @Test
    fun `backend success and ack failure stays ack pending`() = runScenario(
        response = BackendDeliveryResponse("id", accepted = true, duplicate = false),
        ackResult = false,
    ) { states, _, ackCalls ->
        assertEquals(listOf(SyncStatus.BACKEND_DELIVERED_ACK_PENDING), states)
        assertEquals(1, ackCalls)
    }

    @Test
    fun `ack pending retry never invokes HTTP again`() = runBlocking {
        val ack = FakeAckSender(true)
        val coordinator = DeliveryCoordinator(pairing, ack)
        var httpCalls = 0
        val states = mutableListOf<String>()
        coordinator.deliver(
            SyncStatus.BACKEND_DELIVERED_ACK_PENDING, 0, DeliveryReason.ACK_PENDING, deviceId, "id", "/ack",
            http = { httpCalls++; BackendDeliveryResponse("id", true, false) },
            persist = { state, _ -> states += state }, incrementAttempt = {},
        )
        assertEquals(0, httpCalls)
        assertEquals(listOf(SyncStatus.DELIVERED), states)
    }

    @Test
    fun `network failure increments pending and fifth failure is terminal`() = runBlocking {
        val coordinator = DeliveryCoordinator(pairing, FakeAckSender(true))
        var attempts = 0
        val states = mutableListOf<String>()
        coordinator.deliver(
            SyncStatus.PENDING_HTTP, 0, null, deviceId, "id", "/ack",
            http = { throw IOException("offline") }, persist = { state, _ -> states += state },
            incrementAttempt = { attempts++ },
        )
        assertEquals(1, attempts)
        assertTrue(states.isEmpty())
        coordinator.deliver(
            SyncStatus.PENDING_HTTP, DeliveryPolicy.MAX_TRANSIENT_ATTEMPTS - 1, null,
            deviceId, "id", "/ack", http = { throw IOException("offline") },
            persist = { state, _ -> states += state }, incrementAttempt = { attempts++ },
        )
        assertEquals(SyncStatus.TERMINAL_FAILED, states.single())
    }

    @Test
    fun `response mismatch never sends ack`() = runBlocking {
        val ack = FakeAckSender(true)
        val states = mutableListOf<String>()
        DeliveryCoordinator(pairing, ack).deliver(
            SyncStatus.PENDING_HTTP, 0, null, deviceId, "id", "/ack",
            http = { BackendDeliveryResponse("other", true, false) },
            persist = { state, _ -> states += state }, incrementAttempt = {},
        )
        assertEquals(listOf(SyncStatus.TERMINAL_FAILED), states)
        assertEquals(0, ack.calls)
    }

    @Test
    fun `missing or different pairing blocks HTTP and ACK`() = runBlocking {
        val ack = FakeAckSender(true)
        var httpCalls = 0
        val states = mutableListOf<String>()
        DeliveryCoordinator(FakePairing(null, null), ack).deliver(
            SyncStatus.PENDING_HTTP, 0, null, deviceId, "id", "/ack",
            http = { httpCalls++; BackendDeliveryResponse("id", true, false) },
            persist = { state, _ -> states += state }, incrementAttempt = {},
        )
        DeliveryCoordinator(FakePairing("123e4567-e89b-12d3-a456-426614174002", "node-b"), ack).deliver(
            SyncStatus.PENDING_HTTP, 0, null, deviceId, "id", "/ack",
            http = { httpCalls++; BackendDeliveryResponse("id", true, false) },
            persist = { state, _ -> states += state }, incrementAttempt = {},
        )
        assertEquals(0, httpCalls)
        assertTrue(states.isEmpty())
        assertEquals(0, ack.calls)
    }

    private fun runScenario(
        response: BackendDeliveryResponse,
        ackResult: Boolean,
        verify: (List<String>, Int, Int) -> Unit,
    ) = runBlocking {
        val ack = FakeAckSender(ackResult)
        val coordinator = DeliveryCoordinator(pairing, ack)
        val states = mutableListOf<String>()
        var httpCalls = 0
        coordinator.deliver(
            SyncStatus.PENDING_HTTP, 0, null, deviceId, "id", "/ack",
            http = { httpCalls++; response }, persist = { state, _ -> states += state }, incrementAttempt = {},
        )
        verify(states, httpCalls, ack.calls)
    }

    private class FakePairing(
        private val deviceId: String?,
        private val nodeId: String?,
    ) : CurrentWearablePairing {
        override fun pairedDeviceId(): String? = deviceId
        override fun lastKnownWearNodeId(): String? = nodeId
    }

    private class FakeAckSender(private val result: Boolean) : WearAckSender {
        var calls = 0
        override suspend fun sendAck(nodeId: String, path: String): Boolean {
            calls++
            return result
        }
    }
}

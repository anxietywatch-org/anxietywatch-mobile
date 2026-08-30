package com.anxietywatch.mobile.data.bridge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingPolicyTest {
    private val nonce = "123e4567-e89b-12d3-a456-426614174000"
    private val deviceX = "123e4567-e89b-12d3-a456-426614174001"
    private val deviceY = "123e4567-e89b-12d3-a456-426614174002"
    private val pending = PendingPairingSnapshot(nonce, "node-a", 1_000L)

    @Test
    fun `valid identity before five minutes is accepted`() {
        assertTrue(PairingPolicy.acceptsIdentity(pending, "node-a", nonce, deviceX, 301_000L))
    }

    @Test
    fun `expired identity is rejected without allowing a replacement`() {
        assertFalse(PairingPolicy.acceptsIdentity(pending, "node-a", nonce, deviceY, 301_001L))
    }

    @Test
    fun `wrong source node and unsolicited identity are rejected`() {
        assertFalse(PairingPolicy.acceptsIdentity(pending, "node-b", nonce, deviceX, 2_000L))
        assertFalse(PairingPolicy.acceptsIdentity(PendingPairingSnapshot(null, null, null), "node-a", nonce, deviceX, 2_000L))
    }

    @Test
    fun `invalid and zero UUIDs are rejected`() {
        assertTrue(PairingPolicy.isValidUuid(deviceX))
        assertFalse(PairingPolicy.isValidUuid("00000000-0000-0000-0000-000000000000"))
        assertFalse(PairingPolicy.isValidUuid("not-a-uuid"))
        assertFalse(PairingPolicy.isValidUuid(null))
    }

    @Test
    fun `new nonce cannot reuse an old pending pairing`() {
        val newNonce = "123e4567-e89b-12d3-a456-426614174003"
        assertFalse(PairingPolicy.acceptsIdentity(pending, "node-a", newNonce, deviceX, 2_000L))
    }

    @Test
    fun `same device can bind a newly selected node during reconnection`() {
        val reconnect = pending.copy(nonce = "123e4567-e89b-12d3-a456-426614174004", nodeId = "node-b")
        assertTrue(PairingPolicy.acceptsIdentity(reconnect, "node-b", reconnect.nonce!!, deviceX, 2_000L))
    }

    @Test
    fun `pairing confirm payload preserves schema and nonce only`() {
        val raw = pairingConfirmPayload(nonce).toString(Charsets.UTF_8)

        assertTrue(raw.contains("\"schemaVersion\":1"))
        assertTrue(raw.contains("\"pairingNonce\":\"$nonce\""))
        assertFalse(raw.contains("node"))
        assertFalse(raw.contains("deviceId"))
    }

    @Test
    fun `pairing unpair payload preserves schema only`() {
        val raw = pairingUnpairPayload().toString(Charsets.UTF_8)

        assertTrue(raw.contains("\"schemaVersion\":1"))
        assertFalse(raw.contains("node"))
        assertFalse(raw.contains("deviceId"))
        assertFalse(raw.contains("pairingNonce"))
    }

    @Test
    fun `valid identity is the only condition for sending confirm`() {
        assertTrue(PairingPolicy.acceptsIdentity(pending, "node-a", nonce, deviceX, 2_000L))
        assertFalse(PairingPolicy.acceptsIdentity(pending, "node-b", nonce, deviceX, 2_000L))
        assertFalse(PairingPolicy.acceptsIdentity(pending, "node-a", "123e4567-e89b-12d3-a456-426614174009", deviceX, 2_000L))
        assertFalse(PairingPolicy.acceptsIdentity(pending, "node-a", nonce, deviceX, 301_001L))
        assertEquals("node-a", pending.nodeId)
    }
}

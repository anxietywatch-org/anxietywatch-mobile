package com.anxietywatch.mobile.data.bridge

import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DeliveryPolicyTest {
    @Test
    fun `accepted and duplicate are backend delivery success`() {
        assertTrue(DeliveryPolicy.backendDelivered("id", BackendDeliveryResponse("id", true, false)))
        assertTrue(DeliveryPolicy.backendDelivered("id", BackendDeliveryResponse("id", false, true)))
        assertFalse(DeliveryPolicy.backendDelivered("id", BackendDeliveryResponse("other", true, false)))
        assertFalse(DeliveryPolicy.backendDelivered("id", BackendDeliveryResponse("id", false, false)))
    }

    @Test
    fun `backend errors use bounded retry classes`() {
        assertTrue(DeliveryPolicy.classifyFailure(IOException()) == RetryClass.TRANSIENT)
        assertTrue(DeliveryPolicy.classifyFailure(httpError(408)) == RetryClass.TRANSIENT)
        assertTrue(DeliveryPolicy.classifyFailure(httpError(429)) == RetryClass.TRANSIENT)
        assertTrue(DeliveryPolicy.classifyFailure(httpError(503)) == RetryClass.TRANSIENT)
        assertTrue(DeliveryPolicy.classifyFailure(httpError(401)) == RetryClass.WAIT_FOR_AUTH)
        assertTrue(DeliveryPolicy.classifyFailure(httpError(400)) == RetryClass.TERMINAL)
        assertTrue(DeliveryPolicy.classifyFailure(httpError(422)) == RetryClass.TERMINAL)
        assertTrue(DeliveryPolicy.classifyFailure(httpError(403)) == RetryClass.TERMINAL)
        assertTrue(DeliveryPolicy.classifyFailure(httpError(404)) == RetryClass.TERMINAL)
        assertTrue(DeliveryPolicy.classifyFailure(httpError(409)) == RetryClass.TERMINAL)
    }

    @Test
    fun `retry limit is explicit and testable`() {
        assertFalse(DeliveryPolicy.shouldTerminalize(DeliveryPolicy.MAX_TRANSIENT_ATTEMPTS - 1))
        assertTrue(DeliveryPolicy.shouldTerminalize(DeliveryPolicy.MAX_TRANSIENT_ATTEMPTS))
    }

    private fun httpError(status: Int): HttpException =
        HttpException(Response.error<Any>(status, okhttp3.ResponseBody.create(null, "")))
}

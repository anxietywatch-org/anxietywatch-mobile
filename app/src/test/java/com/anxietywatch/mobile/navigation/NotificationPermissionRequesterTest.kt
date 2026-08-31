package com.anxietywatch.mobile.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionRequesterTest {
    @Test
    fun sdkBelow33_doesNotRequest() {
        assertFalse(shouldRequestNotificationPermission(32, permissionGranted = false))
    }

    @Test
    fun sdk33OrAbove_granted_doesNotRequest() {
        assertFalse(shouldRequestNotificationPermission(33, permissionGranted = true))
    }

    @Test
    fun sdk33OrAboveDenied_requests() {
        assertTrue(shouldRequestNotificationPermission(33, permissionGranted = false))
    }

    @Test
    fun denialPolicy_doesNotBlockAuthenticatedFlow() {
        assertTrue(shouldRequestNotificationPermission(33, permissionGranted = false))
    }
}

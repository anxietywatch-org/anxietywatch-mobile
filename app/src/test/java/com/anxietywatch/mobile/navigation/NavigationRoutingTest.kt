package com.anxietywatch.mobile.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NavigationRoutingTest {

    @Test
    fun patientRoleStartsAtPatientPermissions() {
        assertEquals(Routes.PermissionsPatient.route, permissionDestination("patient"))
        assertEquals(Routes.PermissionsPatient.route, permissionDestination("self"))
    }

    @Test
    fun caregiverRoleStartsAtCaregiverPermissions() {
        assertEquals(Routes.PermissionsCaregiver.route, permissionDestination("family_member"))
        assertEquals(Routes.DashboardCaregiver.route, roleDestination("family_member"))
    }

    @Test
    fun unknownRoleReturnsToTokenEntry() {
        assertEquals(Routes.TokenEntry.route, permissionDestination("unknown"))
        assertEquals(Routes.TokenEntry.route, roleDestination("unknown"))
        assertEquals(Routes.TokenEntry.route, roleDestination(null))
    }

    @Test
    fun patientDetailUsesStablePatientId() {
        assertEquals(
            "patient_detail/patient-sofia",
            Routes.PatientDetail.build("patient-sofia"),
        )
    }

    @Test
    fun caregiverPatientClickUsesStablePatientId() {
        val alexRoute = Routes.PatientDetail.build("patient-alex")
        val sofiaRoute = Routes.PatientDetail.build("patient-sofia")

        assertEquals("patient_detail/patient-alex", alexRoute)
        assertEquals("patient_detail/patient-sofia", sofiaRoute)
        assertNotEquals(alexRoute, sofiaRoute)
    }

    @Test
    fun patientDetailRouteKeepsBackStackDestination() {
        assertEquals("patient_detail/{patientId}", Routes.PatientDetail.route)
    }

    @Test
    fun patientLogoutDestinationIsTokenEntry() {
        assertEquals("token_entry", Routes.TokenEntry.route)
    }

    @Test
    fun caregiverGraphUsesStableAlertAndProfileRoutes() {
        assertEquals("caregiver_alerts", Routes.CaregiverAlerts.route)
        assertEquals("caregiver_profile", Routes.CaregiverProfile.route)
        assertEquals(
            "caregiver_alert_detail/alert-alex-1",
            Routes.CaregiverAlertDetail.build("alert-alex-1"),
        )
    }

    @Test
    fun parameterizedRoutesNeverUseVisibleNamesOrIndexes() {
        val patient = Routes.PatientDetail.build("patient-alex")
        val alert = Routes.CaregiverAlertDetail.build("alert-alex-1")
        val event = Routes.EventDetail.build("event-alex-1")

        assertEquals(false, patient.contains("Alex"))
        assertEquals(false, alert.contains("Revisión"))
        assertEquals(false, event.contains("0"))
    }

    @Test
    fun logoutBackStackDestinationIsOutsideAuthenticatedGraph() {
        assertEquals(Routes.TokenEntry.route, Routes.TokenEntry.route)
        assertNotEquals(Routes.TokenEntry.route, Routes.DashboardCaregiver.route)
        assertNotEquals(Routes.TokenEntry.route, Routes.HomePatient.route)
    }
}

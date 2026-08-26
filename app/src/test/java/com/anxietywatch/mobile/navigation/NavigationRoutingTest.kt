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
}

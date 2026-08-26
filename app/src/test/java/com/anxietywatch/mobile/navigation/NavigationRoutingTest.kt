package com.anxietywatch.mobile.navigation

import org.junit.Assert.assertEquals
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
        assertEquals(
            "patient_detail/patient-alex",
            Routes.PatientDetail.build("patient-alex"),
        )
    }

    @Test
    fun patientLogoutDestinationIsTokenEntry() {
        assertEquals("token_entry", Routes.TokenEntry.route)
    }
}

package com.anxietywatch.mobile.data.caregiver

import com.anxietywatch.mobile.data.remote.*
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RealCaregiverRepositoryTest {
    @Test
    fun dashboardMapsOnlyFieldsReturnedByCaregiverPatients() = runBlocking {
        val api = FakeCaregiverApi(
            patients = listOf(
                CaregiverPatientResponseDto("patient-1", "Paciente", null, "family_member", "2026-08-25T20:00:00Z"),
            ),
        )

        val patient = RealCaregiverRepository(api).loadDashboard().patients.single()

        assertEquals("patient-1", patient.id)
        assertEquals("Paciente", patient.displayName)
        assertNull(patient.bpm)
        assertNull(patient.anxiety)
        assertNull(patient.connectivity)
        assertNull(patient.freshness)
        assertNull(patient.lastUpdated)
    }

    @Test
    fun dashboardPreservesEmptyResponse() = runBlocking {
        assertTrue(RealCaregiverRepository(FakeCaregiverApi()).loadDashboard().patients.isEmpty())
    }

    @Test
    fun detailCombinesOnlyRealDetailEventsAndLatestHeartRate() = runBlocking {
        val api = FakeCaregiverApi(
            detail = CaregiverPatientDetailResponseDto("patient-1", "Paciente", "avatar"),
            events = listOf(CaregiverEventResponseDto("event-1", "SOS", "2026-08-25T20:00:00Z", "OPEN")),
            heartRate = CaregiverLatestHeartRateResponseDto(82.5, "2026-08-25T20:00:00Z", 18, "good"),
        )

        val detail = RealCaregiverRepository(api).getPatientDetail("patient-1")

        assertEquals("patient-1", detail.id)
        assertEquals("Paciente", detail.displayName)
        assertEquals(83, detail.bpm)
        assertEquals("2026-08-25T20:00:00Z", detail.lastUpdated)
        assertEquals("event-1", detail.recentEvents.single().id)
        assertEquals("SOS", detail.recentEvents.single().title)
        assertEquals("OPEN", detail.recentEvents.single().description)
        assertTrue(detail.recentAlerts.isEmpty())
        assertNull(detail.anxiety)
        assertNull(detail.connectivity)
        assertNull(detail.freshness)
    }

    @Test
    fun detailAllowsMissingLatestHeartRate() = runBlocking {
        val detail = RealCaregiverRepository(
            FakeCaregiverApi(detail = CaregiverPatientDetailResponseDto("patient-1", "Paciente")),
        ).getPatientDetail("patient-1")

        assertNull(detail.bpm)
        assertNull(detail.lastUpdated)
    }

    @Test
    fun alertsAreEmptyBecauseDevelopExposesNoAlertReadEndpoint() = runBlocking {
        val repository = RealCaregiverRepository(FakeCaregiverApi())

        assertTrue(repository.getAlerts().isEmpty())
        assertNull(repository.getAlertDetail("alert-1"))
    }

    @Test
    fun httpAndNetworkFailuresReachCallerWithoutFakeFallback() = runBlocking {
        val unauthorized = HttpException(Response.error<Any>(401, "".toResponseBody("application/json".toMediaType())))
        val forbidden = HttpException(Response.error<Any>(403, "".toResponseBody("application/json".toMediaType())))
        val notFound = HttpException(Response.error<Any>(404, "".toResponseBody("application/json".toMediaType())))
        val serverError = HttpException(Response.error<Any>(500, "".toResponseBody("application/json".toMediaType())))

        for (failure in listOf(unauthorized, forbidden, notFound, serverError, IOException("offline"))) {
            val repository = RealCaregiverRepository(FakeCaregiverApi(failure = failure))
            val thrown = runCatching { repository.loadDashboard() }.exceptionOrNull()
            assertTrue(thrown === failure)
        }
    }
}

private class FakeCaregiverApi(
    private val patients: List<CaregiverPatientResponseDto> = emptyList(),
    private val detail: CaregiverPatientDetailResponseDto = CaregiverPatientDetailResponseDto("patient-1", "Paciente"),
    private val events: List<CaregiverEventResponseDto> = emptyList(),
    private val heartRate: CaregiverLatestHeartRateResponseDto? = null,
    private val failure: Throwable? = null,
) : AnxietyWatchApi {
    private fun fail(): Nothing = throw (failure ?: UnsupportedOperationException())

    override suspend fun register(request: RegisterRequest) = fail() as AuthenticationResponseDto
    override suspend fun login(request: LoginRequest) = fail() as AuthenticationResponseDto
    override suspend fun session() = fail() as AuthenticationResponseDto
    override suspend fun sendTelemetryBatch(request: CreateTelemetryBatchRequest) = fail() as TelemetryBatchAckResponse
    override suspend fun triggerSos(request: TriggerSosRequest) = fail() as SosTriggerResponse
    override suspend fun cancelSos(request: SosCancelRequest) = fail() as SosCancelResponse
    override suspend fun submitSuspectedEvent(request: SuspectedEventRequest) = fail() as WearableEventResponse
    override suspend fun submitEventDecision(request: EventDecisionRequest) = fail() as WearableEventResponse
    override suspend fun listTokens() = fail() as List<TokenResponseDto>
    override suspend fun createToken(request: CreateTokenRequest) = fail() as TokenResponseDto
    override suspend fun acceptByCode(request: AcceptByCodeRequest) = fail() as TokenRedeemResponseDto
    override suspend fun getProfile() = fail() as ProfileResponseDto
    override suspend fun updateProfile(request: ProfileUpdateRequest) = fail() as ProfileResponseDto
    override suspend fun getDashboardSummary() = fail() as DashboardSummaryDto
    override suspend fun getEpisodes(range: Int) = fail() as List<EpisodeDto>
    override suspend fun getCaregiverPatients(): List<CaregiverPatientResponseDto> {
        failure?.let { throw it }
        return patients
    }
    override suspend fun getCaregiverPatientDetail(patientId: String): CaregiverPatientDetailResponseDto {
        failure?.let { throw it }
        return detail
    }
    override suspend fun getCaregiverPatientEvents(patientId: String, limit: Int): List<CaregiverEventResponseDto> {
        failure?.let { throw it }
        return events
    }
    override suspend fun getCaregiverLatestHeartRate(patientId: String): CaregiverLatestHeartRateResponseDto? {
        failure?.let { throw it }
        return heartRate
    }

    override suspend fun getCaregiverPatient(patientId: String): CaregiverPatientDetailDto {
        failure?.let { throw it }
        return detail
    }

    override suspend fun getCaregiverPatientEpisodes(patientId: String): List<CaregiverEpisodeDto> {
        failure?.let { throw it }
        return emptyList()
    }

    override suspend fun getCaregiverPatientLatestTelemetry(patientId: String): CaregiverTelemetryLatestDto {
        failure?.let { throw it }
        return heartRate ?: CaregiverTelemetryLatestDto(measuredAt = "1970-01-01T00:00:00Z")
    }

    override suspend fun linkCaregiverPatient(request: LinkCaregiverPatientRequest) {
        fail()
    }

    override suspend fun registerDevice(request: RegisterDeviceRequest) {
        fail()
    }

    override suspend fun unregisterDevice(request: UnregisterDeviceRequest) {
        fail()
    }
}

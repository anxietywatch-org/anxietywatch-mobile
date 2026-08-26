package com.anxietywatch.mobile.ui.wellness

import com.anxietywatch.mobile.data.caregiver.CaregiverPatientDetailSource
import com.anxietywatch.mobile.data.caregiver.CaregiverRecentAlertSource
import com.anxietywatch.mobile.data.caregiver.CaregiverRecentEventSource
import com.anxietywatch.mobile.data.caregiver.FakeCaregiverRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class CaregiverPatientDetailTest {
    @Test
    fun alexLoadsFromFakeRepository() = runBlocking {
        val detail = FakeCaregiverRepository().getPatientDetail("patient-alex")

        assertEquals("patient-alex", detail?.id)
        assertEquals("Alex", detail?.displayName)
        assertEquals(72, detail?.bpm)
    }

    @Test
    fun sofiaLoadsDifferentDetail() = runBlocking {
        val detail = FakeCaregiverRepository().getPatientDetail("patient-sofia")

        assertEquals("patient-sofia", detail?.id)
        assertEquals("Sofía", detail?.displayName)
        assertEquals(96, detail?.bpm)
    }

    @Test
    fun unknownPatientDoesNotFallBackToAlex() = runBlocking {
        assertNull(FakeCaregiverRepository().getPatientDetail("unknown-id"))
    }

    @Test
    fun loadingContentAndErrorStatesAreExplicit() {
        assertSame(CaregiverPatientDetailUiState.Loading, CaregiverPatientDetailUiState.Loading)
        assertEquals("patient-test", CaregiverPatientDetailUiState.Content(detail().toUiModel()).data.id)
        assertEquals("offline", CaregiverPatientDetailUiState.Error("offline").message)
    }

    @Test
    fun bpmAndAnxietyAreIndependentWhenBothPresent() {
        val model = detail(bpm = 72, anxiety = 85).toUiModel()

        assertEquals(72, model.bpm)
        assertEquals(85, model.anxiety)
    }

    @Test
    fun knownAnxietyCanExistWithoutBpm() {
        val model = detail(bpm = null, anxiety = 40).toUiModel()

        assertNull(model.bpm)
        assertEquals(40, model.anxiety)
    }

    @Test
    fun knownBpmCanExistWithoutAnxiety() {
        val model = detail(bpm = 110, anxiety = null).toUiModel()

        assertEquals(110, model.bpm)
        assertNull(model.anxiety)
    }

    @Test
    fun bothMetricsCanBeNull() {
        val model = detail(bpm = null, anxiety = null).toUiModel()

        assertNull(model.bpm)
        assertNull(model.anxiety)
    }

    @Test
    fun eventsAndAlertsMapWhenAvailable() {
        val model = detail(
            events = listOf(CaregiverRecentEventSource("event-1", "Evento", "Detalle", "Hoy")),
            alerts = listOf(CaregiverRecentAlertSource("alert-1", "Alerta", "Detalle", "Hoy", "Pendiente")),
        ).toUiModel()

        assertEquals("event-1", model.recentEvents.single().id)
        assertEquals("alert-1", model.recentAlerts.single().id)
    }

    @Test
    fun eventsAndAlertsCanBeEmpty() {
        val model = detail(events = emptyList(), alerts = emptyList()).toUiModel()

        assertTrue(model.recentEvents.isEmpty())
        assertTrue(model.recentAlerts.isEmpty())
    }

    @Test
    fun refreshRetryErrorStateKeepsPatientContentConceptuallySeparate() {
        val content = CaregiverPatientDetailUiState.Content(detail().toUiModel())
        val error = CaregiverPatientDetailUiState.Error("retry failed")

        assertEquals("patient-test", content.data.id)
        assertEquals("retry failed", error.message)
    }

    private fun detail(
        bpm: Int? = 72,
        anxiety: Int? = 68,
        events: List<CaregiverRecentEventSource> = emptyList(),
        alerts: List<CaregiverRecentAlertSource> = emptyList(),
    ) = CaregiverPatientDetailSource(
        id = "patient-test",
        displayName = "Paciente de prueba",
        bpm = bpm,
        anxiety = anxiety,
        recentEvents = events,
        recentAlerts = alerts,
    )
}

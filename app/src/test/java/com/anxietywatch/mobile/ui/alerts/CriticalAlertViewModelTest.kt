package com.anxietywatch.mobile.ui.alerts

import com.anxietywatch.mobile.ui.common.AsyncUiState
import org.junit.Assert.*
import org.junit.Test

class CriticalAlertViewModelTest {
    @Test
    fun initialState_isLoading() {
        val viewModel = CriticalAlertViewModel()

        assertSame(AsyncUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun loadAlert_finishesWithEmptyState() {
        val viewModel = CriticalAlertViewModel()

        viewModel.loadAlert("event-id")

        assertSame(AsyncUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun loadAlert_withPushPayload_finishesWithSuccessState() {
        val viewModel = CriticalAlertViewModel()
        val alert = CriticalAlertUiModel(
            patientName = "Paciente",
            message = "Solicitó apoyo.",
        )

        viewModel.loadAlert("event-id", alert)

        assertEquals(AsyncUiState.Success(alert), viewModel.uiState.value)
    }
}

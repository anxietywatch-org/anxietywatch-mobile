package com.anxietywatch.mobile.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.CaregiverEventDto
import com.anxietywatch.mobile.ui.common.AsyncUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EventDetailUiModel(
    val title: String,
    val category: String? = null,
    val occurredAt: String? = null,
    val summary: String? = null,
    val metrics: List<EventMetricUiModel> = emptyList(),
    val location: String? = null,
    val systemNotes: String? = null,
    val tags: List<String> = emptyList(),
)

data class EventMetricUiModel(
    val title: String,
    val value: String,
    val detail: String? = null,
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val api: AnxietyWatchApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AsyncUiState<EventDetailUiModel>>(AsyncUiState.Loading)
    val uiState: StateFlow<AsyncUiState<EventDetailUiModel>> = _uiState.asStateFlow()

    fun loadEvent(patientId: String, eventId: String) {
        _uiState.value = AsyncUiState.Loading
        if (patientId.isBlank() || eventId.isBlank()) {
            _uiState.value = AsyncUiState.Empty
            return
        }

        viewModelScope.launch {
            try {
                val event = api.getCaregiverPatientEvents(patientId)
                    .firstOrNull { it.eventId == eventId }
                _uiState.value = event?.let { AsyncUiState.Success(it.toUiModel()) }
                    ?: AsyncUiState.Empty
            } catch (error: CancellationException) {
                throw error
            } catch (error: HttpException) {
                _uiState.value = if (error.code() == 403 || error.code() == 404) {
                    AsyncUiState.Empty
                } else {
                    AsyncUiState.Error("No pudimos cargar el evento. Intenta de nuevo.")
                }
            } catch (_: Exception) {
                _uiState.value = AsyncUiState.Error(
                    "No pudimos cargar el evento. Revisa tu conexión e intenta de nuevo.",
                )
            }
        }
    }

    private fun CaregiverEventDto.toUiModel() = EventDetailUiModel(
        title = title,
        category = category,
        occurredAt = occurredAt?.let(::formatTimestamp),
        summary = summary ?: description,
        metrics = metrics.map { metric ->
            EventMetricUiModel(
                title = metric.title,
                value = metric.value,
                detail = metric.detail,
            )
        },
        location = location,
        systemNotes = systemNotes,
        tags = tags,
    )

    private fun formatTimestamp(value: String): String = runCatching {
        DATE_TIME_FORMATTER.format(Instant.parse(value).atZone(ZoneId.systemDefault()))
    }.getOrDefault(value)

    private companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm")
    }
}

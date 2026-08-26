package com.anxietywatch.mobile.ui.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.CaregiverEpisodeDto
import com.anxietywatch.mobile.data.remote.CaregiverEventDto
import com.anxietywatch.mobile.ui.common.AsyncUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import javax.inject.Inject

data class PatientDetailUiModel(
    val name: String,
    val status: String? = null,
    val heartRateSamples: List<HeartRateSampleUiModel> = emptyList(),
    val events: List<WellnessEventUiModel> = emptyList(),
)

data class HeartRateSampleUiModel(val label: String, val beatsPerMinute: Int)

data class WellnessEventUiModel(
    val id: String,
    val title: String,
    val description: String? = null,
    val time: String? = null,
    val type: WellnessEventType,
)

enum class WellnessEventType { Crisis, Breathing, ElevatedRhythm, Unknown }

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val api: AnxietyWatchApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AsyncUiState<PatientDetailUiModel>>(AsyncUiState.Loading)
    val uiState: StateFlow<AsyncUiState<PatientDetailUiModel>> = _uiState.asStateFlow()

    fun loadPatient(patientId: String) {
        _uiState.value = AsyncUiState.Loading
        if (patientId.isBlank()) {
            _uiState.value = AsyncUiState.Empty
            return
        }

        viewModelScope.launch {
            try {
                val patient = api.getCaregiverPatient(patientId)
                val (episodes, telemetry, events) = coroutineScope {
                    val episodesRequest = async { api.getCaregiverPatientEpisodes(patientId) }
                    val telemetryRequest = async {
                        try {
                            api.getCaregiverPatientLatestTelemetry(patientId)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    val eventsRequest = async { api.getCaregiverPatientEvents(patientId) }
                    Triple(episodesRequest.await(), telemetryRequest.await(), eventsRequest.await())
                }

                val recentEvents = (events.map(::mapEvent) + episodes.map(::mapEpisode))
                    .distinctBy(WellnessEventUiModel::id)
                val heartRateSamples = telemetry?.heartRateBpm
                    ?.takeIf { it.isFinite() && it > 0.0 }
                    ?.let { bpm ->
                        listOf(
                            HeartRateSampleUiModel(
                                label = formatTimestamp(telemetry.timestamp),
                                beatsPerMinute = bpm.roundToInt(),
                            ),
                        )
                    }
                    .orEmpty()

                _uiState.value = AsyncUiState.Success(
                    PatientDetailUiModel(
                        name = patient.fullName,
                        status = patient.status,
                        heartRateSamples = heartRateSamples,
                        events = recentEvents,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: HttpException) {
                _uiState.value = if (error.code() == 403 || error.code() == 404) {
                    AsyncUiState.Empty
                } else {
                    AsyncUiState.Error("No pudimos cargar la información del paciente. Intenta de nuevo.")
                }
            } catch (_: Exception) {
                _uiState.value = AsyncUiState.Error(
                    "No pudimos cargar la información del paciente. Revisa tu conexión e intenta de nuevo.",
                )
            }
        }
    }

    private fun mapEpisode(episode: CaregiverEpisodeDto) = WellnessEventUiModel(
        id = episode.id,
        title = "Episodio registrado",
        description = episode.notes ?: episode.symptoms.takeIf { it.isNotEmpty() }?.joinToString(),
        time = formatTimestamp(episode.date),
        type = WellnessEventType.Unknown,
    )

    private fun mapEvent(event: CaregiverEventDto) = WellnessEventUiModel(
        id = event.eventId,
        title = event.title,
        description = event.description,
        time = event.occurredAt?.let(::formatTimestamp),
        type = when (event.type?.lowercase()) {
            "crisis", "sos" -> WellnessEventType.Crisis
            "breathing", "respiracion", "respiración" -> WellnessEventType.Breathing
            "elevated_rhythm", "elevatedrhythm", "ritmo_elevado" -> WellnessEventType.ElevatedRhythm
            else -> WellnessEventType.Unknown
        },
    )

    private fun formatTimestamp(value: String): String = runCatching {
        DATE_TIME_FORMATTER.format(Instant.parse(value).atZone(ZoneId.systemDefault()))
    }.getOrDefault(value)

    private companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm")
    }
}

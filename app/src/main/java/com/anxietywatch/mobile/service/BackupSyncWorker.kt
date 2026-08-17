package com.anxietywatch.mobile.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.anxietywatch.mobile.data.local.AppDatabase
import com.anxietywatch.mobile.data.local.SyncStatus
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.CreateTelemetryBatchRequest
import com.anxietywatch.mobile.data.remote.SessionRepository
import com.anxietywatch.mobile.data.remote.TriggerSosRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Sync de respaldo real (seccion 4.8 del documento atomico): lee de Room lo que quedo
 * PENDING (porque el envio inmediato desde PhoneDataLayerListenerService fallo por falta
 * de red o error del servidor) y reintenta. NO hace deteccion de crisis -- eso ya paso
 * hace rato, 100% local, en el reloj/movil.
 */
@HiltWorker
class BackupSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sessionRepository: SessionRepository,
    private val database: AppDatabase,
    private val api: AnxietyWatchApi,
) : CoroutineWorker(context, params) {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        if (!sessionRepository.hasValidSession()) return Result.success()

        val dao = database.pendingUploadDao()

        dao.getSosEventsByStatus(SyncStatus.PENDING).forEach { pending ->
            runCatching {
                val request = json.decodeFromString<TriggerSosRequest>(pending.requestJson)
                api.triggerSos(request)
            }.onSuccess {
                dao.updateSosEventStatus(pending.eventId, SyncStatus.SYNCED)
            }.onFailure {
                dao.incrementSosAttempt(pending.eventId)
            }
        }

        dao.getTelemetryBatchesByStatus(SyncStatus.PENDING).forEach { pending ->
            runCatching {
                val request = json.decodeFromString<CreateTelemetryBatchRequest>(pending.requestJson)
                api.sendTelemetryBatch(request)
            }.onSuccess {
                dao.updateTelemetryBatchStatus(pending.batchId, SyncStatus.SYNCED)
            }.onFailure {
                dao.incrementTelemetryAttempt(pending.batchId)
            }
        }

        // Limpieza: filas ya sincronizadas hace mas de 7 dias no necesitan seguir en el
        // telefono -- el dato ya vive en el backend.
        dao.deleteSyncedOlderThan(olderThanMillis = System.currentTimeMillis() - SEVEN_DAYS_MILLIS)

        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "backup_sync_worker"
        private const val SEVEN_DAYS_MILLIS = 7 * 24 * 60 * 60 * 1000L

        fun constraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}

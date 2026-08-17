package com.anxietywatch.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Todas las queries son via anotaciones de Room (@Query con parametros nombrados, nunca
 * concatenacion de strings) -- eso es lo que hace que Room sea inmune a inyeccion SQL por
 * diseno: el valor de [batchId]/[eventId] SIEMPRE viaja como parametro enlazado (bind
 * parameter), nunca se pega directo al texto del SQL, sin importar que caracteres traiga.
 */
@Dao
interface PendingUploadDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // idempotente: mismo batchId no duplica fila
    suspend fun insertTelemetryBatch(entity: PendingTelemetryBatchEntity)

    @Query("SELECT * FROM pending_telemetry_batches WHERE syncStatus = :status ORDER BY createdAtMillis ASC")
    suspend fun getTelemetryBatchesByStatus(status: String = SyncStatus.PENDING): List<PendingTelemetryBatchEntity>

    @Query("UPDATE pending_telemetry_batches SET syncStatus = :status WHERE batchId = :batchId")
    suspend fun updateTelemetryBatchStatus(batchId: String, status: String)

    @Query("UPDATE pending_telemetry_batches SET attemptCount = attemptCount + 1 WHERE batchId = :batchId")
    suspend fun incrementTelemetryAttempt(batchId: String)

    @Query("DELETE FROM pending_telemetry_batches WHERE syncStatus = :status AND createdAtMillis < :olderThanMillis")
    suspend fun deleteSyncedOlderThan(status: String = SyncStatus.SYNCED, olderThanMillis: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSosEvent(entity: PendingSosEventEntity)

    @Query("SELECT * FROM pending_sos_events WHERE syncStatus = :status ORDER BY createdAtMillis ASC")
    suspend fun getSosEventsByStatus(status: String = SyncStatus.PENDING): List<PendingSosEventEntity>

    @Query("UPDATE pending_sos_events SET syncStatus = :status WHERE eventId = :eventId")
    suspend fun updateSosEventStatus(eventId: String, status: String)

    @Query("UPDATE pending_sos_events SET attemptCount = attemptCount + 1 WHERE eventId = :eventId")
    suspend fun incrementSosAttempt(eventId: String)
}

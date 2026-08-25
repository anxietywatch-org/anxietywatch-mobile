package com.anxietywatch.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM pending_telemetry_batches ORDER BY createdAtMillis DESC LIMIT 1")
    fun observeMostRecentTelemetryBatch(): Flow<PendingTelemetryBatchEntity?>

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSosCancelEvent(entity: PendingSosCancelEventEntity)

    @Query("SELECT * FROM pending_sos_cancel_events WHERE syncStatus = :status ORDER BY createdAtMillis ASC")
    suspend fun getSosCancelEventsByStatus(status: String = SyncStatus.PENDING): List<PendingSosCancelEventEntity>

    @Query("UPDATE pending_sos_cancel_events SET syncStatus = :status WHERE eventId = :eventId")
    suspend fun updateSosCancelEventStatus(eventId: String, status: String)

    @Query("UPDATE pending_sos_cancel_events SET attemptCount = attemptCount + 1 WHERE eventId = :eventId")
    suspend fun incrementSosCancelAttempt(eventId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSuspectedEvent(entity: PendingSuspectedEventEntity)

    @Query("SELECT * FROM pending_suspected_events WHERE syncStatus = :status ORDER BY createdAtMillis ASC")
    suspend fun getSuspectedEventsByStatus(status: String = SyncStatus.PENDING): List<PendingSuspectedEventEntity>

    @Query("UPDATE pending_suspected_events SET syncStatus = :status WHERE eventId = :eventId")
    suspend fun updateSuspectedEventStatus(eventId: String, status: String)

    @Query("UPDATE pending_suspected_events SET attemptCount = attemptCount + 1 WHERE eventId = :eventId")
    suspend fun incrementSuspectedAttempt(eventId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEventDecision(entity: PendingEventDecisionEntity)

    @Query("SELECT * FROM pending_event_decisions WHERE syncStatus = :status ORDER BY createdAtMillis ASC")
    suspend fun getEventDecisionsByStatus(status: String = SyncStatus.PENDING): List<PendingEventDecisionEntity>

    @Query("UPDATE pending_event_decisions SET syncStatus = :status WHERE eventId = :eventId")
    suspend fun updateEventDecisionStatus(eventId: String, status: String)

    @Query("UPDATE pending_event_decisions SET attemptCount = attemptCount + 1 WHERE eventId = :eventId")
    suspend fun incrementEventDecisionAttempt(eventId: String)
}

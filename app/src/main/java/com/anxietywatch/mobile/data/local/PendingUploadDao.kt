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
    suspend fun getTelemetryBatchesByStatus(status: String = SyncStatus.PENDING_HTTP): List<PendingTelemetryBatchEntity>

    @Query("SELECT * FROM pending_telemetry_batches WHERE batchId = :batchId LIMIT 1")
    suspend fun getTelemetryBatch(batchId: String): PendingTelemetryBatchEntity?

    @Query("UPDATE pending_telemetry_batches SET wearableDeviceId = :wearableDeviceId WHERE batchId = :batchId")
    suspend fun setTelemetryOwnership(batchId: String, wearableDeviceId: String)

    @Query("SELECT * FROM pending_telemetry_batches ORDER BY createdAtMillis DESC LIMIT 1")
    fun observeMostRecentTelemetryBatch(): Flow<PendingTelemetryBatchEntity?>

    @Query("UPDATE pending_telemetry_batches SET syncStatus = :status, lastError = :reason WHERE batchId = :batchId")
    suspend fun updateTelemetryBatchStatus(batchId: String, status: String, reason: String? = null)

    @Query("UPDATE pending_telemetry_batches SET attemptCount = attemptCount + 1, lastError = :reason WHERE batchId = :batchId")
    suspend fun incrementTelemetryAttempt(batchId: String, reason: String? = null)

    @Query("DELETE FROM pending_telemetry_batches WHERE syncStatus = :status AND createdAtMillis < :olderThanMillis")
    suspend fun deleteSyncedOlderThan(status: String = SyncStatus.DELIVERED, olderThanMillis: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSosEvent(entity: PendingSosEventEntity)

    @Query("SELECT * FROM pending_sos_events WHERE syncStatus = :status ORDER BY createdAtMillis ASC")
    suspend fun getSosEventsByStatus(status: String = SyncStatus.PENDING_HTTP): List<PendingSosEventEntity>

    @Query("SELECT * FROM pending_sos_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getSosEvent(eventId: String): PendingSosEventEntity?

    @Query("UPDATE pending_sos_events SET wearableDeviceId = :wearableDeviceId WHERE eventId = :eventId")
    suspend fun setSosOwnership(eventId: String, wearableDeviceId: String)

    @Query("UPDATE pending_sos_events SET syncStatus = :status, lastError = :reason WHERE eventId = :eventId")
    suspend fun updateSosEventStatus(eventId: String, status: String, reason: String? = null)

    @Query("UPDATE pending_sos_events SET attemptCount = attemptCount + 1, lastError = :reason WHERE eventId = :eventId")
    suspend fun incrementSosAttempt(eventId: String, reason: String? = null)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSosCancelEvent(entity: PendingSosCancelEventEntity)

    @Query("SELECT * FROM pending_sos_cancel_events WHERE syncStatus = :status ORDER BY createdAtMillis ASC")
    suspend fun getSosCancelEventsByStatus(status: String = SyncStatus.PENDING_HTTP): List<PendingSosCancelEventEntity>

    @Query("SELECT * FROM pending_sos_cancel_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getSosCancelEvent(eventId: String): PendingSosCancelEventEntity?

    @Query("UPDATE pending_sos_cancel_events SET wearableDeviceId = :wearableDeviceId WHERE eventId = :eventId")
    suspend fun setSosCancelOwnership(eventId: String, wearableDeviceId: String)

    @Query("UPDATE pending_sos_cancel_events SET syncStatus = :status, lastError = :reason WHERE eventId = :eventId")
    suspend fun updateSosCancelEventStatus(eventId: String, status: String, reason: String? = null)

    @Query("UPDATE pending_sos_cancel_events SET attemptCount = attemptCount + 1, lastError = :reason WHERE eventId = :eventId")
    suspend fun incrementSosCancelAttempt(eventId: String, reason: String? = null)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSuspectedEvent(entity: PendingSuspectedEventEntity)

    @Query("SELECT * FROM pending_suspected_events WHERE syncStatus = :status ORDER BY createdAtMillis ASC")
    suspend fun getSuspectedEventsByStatus(status: String = SyncStatus.PENDING_HTTP): List<PendingSuspectedEventEntity>

    @Query("SELECT * FROM pending_suspected_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getSuspectedEvent(eventId: String): PendingSuspectedEventEntity?

    @Query("UPDATE pending_suspected_events SET wearableDeviceId = :wearableDeviceId WHERE eventId = :eventId")
    suspend fun setSuspectedOwnership(eventId: String, wearableDeviceId: String)

    @Query("UPDATE pending_suspected_events SET syncStatus = :status, lastError = :reason WHERE eventId = :eventId")
    suspend fun updateSuspectedEventStatus(eventId: String, status: String, reason: String? = null)

    @Query("UPDATE pending_suspected_events SET attemptCount = attemptCount + 1, lastError = :reason WHERE eventId = :eventId")
    suspend fun incrementSuspectedAttempt(eventId: String, reason: String? = null)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEventDecision(entity: PendingEventDecisionEntity)

    @Query("SELECT * FROM pending_event_decisions WHERE syncStatus = :status ORDER BY createdAtMillis ASC")
    suspend fun getEventDecisionsByStatus(status: String = SyncStatus.PENDING_HTTP): List<PendingEventDecisionEntity>

    @Query("SELECT * FROM pending_event_decisions WHERE eventId = :eventId LIMIT 1")
    suspend fun getEventDecision(eventId: String): PendingEventDecisionEntity?

    @Query("UPDATE pending_event_decisions SET wearableDeviceId = :wearableDeviceId WHERE eventId = :eventId")
    suspend fun setDecisionOwnership(eventId: String, wearableDeviceId: String)

    @Query("UPDATE pending_event_decisions SET syncStatus = :status, lastError = :reason WHERE eventId = :eventId")
    suspend fun updateEventDecisionStatus(eventId: String, status: String, reason: String? = null)

    @Query("UPDATE pending_event_decisions SET attemptCount = attemptCount + 1, lastError = :reason WHERE eventId = :eventId")
    suspend fun incrementEventDecisionAttempt(eventId: String, reason: String? = null)
}

package com.anxietywatch.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cola local de "Log_Biometrico" (seccion 4 del documento atomico): un lote de telemetria
 * que se guarda ANTES de intentar subirlo, para que si no hay red -- o si la subida falla
 * por cualquier razon -- el dato no se pierda. syncStatus empieza en PENDING; el mismo
 * PhoneDataLayerListenerService lo marca SYNCED al confirmar 200/202/duplicado.
 *
 * requestJson guarda el CreateTelemetryBatchRequest completo ya serializado -- asi
 * BackupSyncWorker no necesita reconstruir el objeto desde cero, solo reenviar el mismo
 * JSON tal cual se hubiera mandado la primera vez.
 */
@Entity(tableName = "pending_telemetry_batches")
data class PendingTelemetryBatchEntity(
    @PrimaryKey val batchId: String,
    val requestJson: String,
    val createdAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING,
    val attemptCount: Int = 0,
)

/** Igual que la de telemetria, pero para eventos de crisis -- estos son mas criticos
 *  todavia: nunca deben quedar solo en memoria mientras se reintenta la red. */
@Entity(tableName = "pending_sos_events")
data class PendingSosEventEntity(
    @PrimaryKey val eventId: String,
    val requestJson: String,
    val createdAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING,
    val attemptCount: Int = 0,
)

@Entity(tableName = "pending_sos_cancel_events")
data class PendingSosCancelEventEntity(
    @PrimaryKey val eventId: String,
    val requestJson: String,
    val createdAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING,
    val attemptCount: Int = 0,
)

@Entity(tableName = "pending_suspected_events")
data class PendingSuspectedEventEntity(
    @PrimaryKey val eventId: String,
    val requestJson: String,
    val createdAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING,
    val attemptCount: Int = 0,
)

@Entity(tableName = "pending_event_decisions")
data class PendingEventDecisionEntity(
    @PrimaryKey val eventId: String,
    val requestJson: String,
    val createdAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING,
    val attemptCount: Int = 0,
)

object SyncStatus {
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
}

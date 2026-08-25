package com.anxietywatch.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * NOTA de seguridad: esta base de datos vive en el almacenamiento privado de la app
 * (/data/data/com.anxietywatch.mobile/databases/), inaccesible para otras apps sin root.
 * No se cifra con SQLCipher todavia -- si el equipo decide que hace falta cifrado a nivel
 * de archivo (recomendable para datos de salud), hay que agregar la dependencia de
 * SQLCipher y cambiar Room.databaseBuilder por el builder de SupportFactory cifrado.
 */
@Database(
    entities = [
        PendingTelemetryBatchEntity::class,
        PendingSosEventEntity::class,
        PendingSosCancelEventEntity::class,
        PendingSuspectedEventEntity::class,
        PendingEventDecisionEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingUploadDao(): PendingUploadDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "anxietywatch.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS pending_sos_cancel_events (eventId TEXT NOT NULL PRIMARY KEY, requestJson TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, syncStatus TEXT NOT NULL, attemptCount INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS pending_suspected_events (eventId TEXT NOT NULL PRIMARY KEY, requestJson TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, syncStatus TEXT NOT NULL, attemptCount INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS pending_event_decisions (eventId TEXT NOT NULL PRIMARY KEY, requestJson TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, syncStatus TEXT NOT NULL, attemptCount INTEGER NOT NULL)")
            }
        }
    }
}

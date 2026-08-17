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
    entities = [PendingTelemetryBatchEntity::class, PendingSosEventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingUploadDao(): PendingUploadDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "anxietywatch.db")
                .build()
    }
}

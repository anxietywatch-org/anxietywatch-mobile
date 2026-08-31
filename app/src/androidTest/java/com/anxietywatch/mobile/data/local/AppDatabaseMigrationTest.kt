package com.anxietywatch.mobile.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migration2To3PreservesLegacyPayloadAndNormalizesState() {
        helper.createDatabase(DATABASE_NAME, 1).apply {
            execSQL("CREATE TABLE IF NOT EXISTS pending_telemetry_batches (batchId TEXT NOT NULL PRIMARY KEY, requestJson TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, syncStatus TEXT NOT NULL, attemptCount INTEGER NOT NULL)")
            execSQL("INSERT INTO pending_telemetry_batches(batchId, requestJson, createdAtMillis, syncStatus, attemptCount) VALUES ('batch-1', '{\"deviceId\":\"123e4567-e89b-12d3-a456-426614174001\"}', 10, 'PENDING', 2)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            3,
            true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
        )
        migrated.query("SELECT requestJson, syncStatus, attemptCount, wearableDeviceId FROM pending_telemetry_batches WHERE batchId = 'batch-1'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("{\"deviceId\":\"123e4567-e89b-12d3-a456-426614174001\"}", cursor.getString(0))
            assertEquals(SyncStatus.PENDING_HTTP, cursor.getString(1))
            assertEquals(2, cursor.getInt(2))
            assertEquals("", cursor.getString(3))
        }
        assertNotNull(migrated)
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "migration-test.db"
    }
}

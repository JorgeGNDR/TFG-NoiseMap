package com.gandara.tfgjorgegandara.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MeasurementSession::class,
        AudioSample::class, 
        FrequencyBin::class, 
        SoundClassification::class
    ], 
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun measurementSessionDao(): MeasurementSessionDao
    abstract fun audioSampleDao(): AudioSampleDao
    abstract fun frequencyBinDao(): FrequencyBinDao
    abstract fun soundClassificationDao(): SoundClassificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noise_map_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE audio_samples ADD COLUMN aiExplanation TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS geo_tiles")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS measurement_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startTimestamp INTEGER NOT NULL,
                        endTimestamp INTEGER,
                        durationMs INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS audio_samples_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER,
                        timestamp INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL DEFAULT 3000,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        avgDb REAL NOT NULL,
                        peakDb REAL NOT NULL,
                        dominantFreq REAL NOT NULL DEFAULT 0,
                        weighting TEXT NOT NULL,
                        calibrationOffset REAL NOT NULL DEFAULT 90,
                        aiExplanation TEXT,
                        FOREIGN KEY(sessionId) REFERENCES measurement_sessions(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO audio_samples_new (
                        id,
                        timestamp,
                        latitude,
                        longitude,
                        avgDb,
                        peakDb,
                        dominantFreq,
                        weighting,
                        aiExplanation
                    )
                    SELECT
                        id,
                        timestamp,
                        latitude,
                        longitude,
                        avgDb,
                        peakDb,
                        dominantFreq,
                        weighting,
                        aiExplanation
                    FROM audio_samples
                """.trimIndent())

                db.execSQL("DROP TABLE audio_samples")
                db.execSQL("ALTER TABLE audio_samples_new RENAME TO audio_samples")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audio_samples_timestamp ON audio_samples(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audio_samples_latitude_longitude ON audio_samples(latitude, longitude)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audio_samples_sessionId ON audio_samples(sessionId)")
            }
        }
    }
}

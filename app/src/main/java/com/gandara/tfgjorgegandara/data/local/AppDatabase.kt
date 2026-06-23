package com.gandara.tfgjorgegandara.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AudioSample::class, 
        GeoTile::class, 
        FrequencyBin::class, 
        SoundClassification::class
    ], 
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun audioSampleDao(): AudioSampleDao
    abstract fun geoTileDao(): GeoTileDao
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
                .addMigrations(MIGRATION_5_6)
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
    }
}

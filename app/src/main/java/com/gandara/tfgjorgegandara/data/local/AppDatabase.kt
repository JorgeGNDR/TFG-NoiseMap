package com.gandara.tfgjorgegandara.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AudioSample::class, 
        GeoTile::class, 
        FrequencyBin::class, 
        SoundClassification::class
    ], 
    version = 4, 
    exportSchema = false
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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

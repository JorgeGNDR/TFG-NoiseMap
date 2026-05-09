package com.gandara.tfgjorgegandara.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SoundClassificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassifications(classifications: List<SoundClassification>)

    @Query("SELECT * FROM sound_classifications WHERE sampleId = :sampleId ORDER BY probability DESC")
    suspend fun getClassificationsForSample(sampleId: Long): List<SoundClassification>
}

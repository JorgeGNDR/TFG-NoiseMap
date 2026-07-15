package com.gandara.tfgjorgegandara.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MeasurementSessionDao {

    @Insert
    suspend fun insertSession(session: MeasurementSession): Long

    @Query("""
        UPDATE measurement_sessions
        SET endTimestamp = :endTimestamp,
            durationMs = :durationMs
        WHERE id = :sessionId
    """)
    suspend fun finishSession(sessionId: Long, endTimestamp: Long, durationMs: Long)
}

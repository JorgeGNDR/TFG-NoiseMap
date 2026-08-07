package com.gandara.tfgjorgegandara.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementSessionDao {

    @Insert
    suspend fun insertSession(session: MeasurementSession): Long

    @Query("SELECT * FROM measurement_sessions ORDER BY startTimestamp DESC")
    fun observeSessions(): Flow<List<MeasurementSession>>

    @Query("""
        UPDATE measurement_sessions
        SET endTimestamp = :endTimestamp,
            durationMs = :durationMs
        WHERE id = :sessionId
    """)
    suspend fun finishSession(sessionId: Long, endTimestamp: Long, durationMs: Long)

    @Query("DELETE FROM measurement_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)
}

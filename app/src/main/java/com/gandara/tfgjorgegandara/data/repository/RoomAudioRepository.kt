package com.gandara.tfgjorgegandara.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.gandara.tfgjorgegandara.data.local.AppDatabase
import com.gandara.tfgjorgegandara.data.local.AudioSample
import com.gandara.tfgjorgegandara.data.local.AudioSampleDao
import com.gandara.tfgjorgegandara.data.local.FrequencyBin
import com.gandara.tfgjorgegandara.data.local.FrequencyBinDao
import com.gandara.tfgjorgegandara.data.local.MeasurementSession
import com.gandara.tfgjorgegandara.data.local.MeasurementSessionDao
import com.gandara.tfgjorgegandara.data.local.SoundClassification
import com.gandara.tfgjorgegandara.data.local.SoundClassificationDao
import com.gandara.tfgjorgegandara.domain.audio.DecibelMath
import com.gandara.tfgjorgegandara.domain.model.HeatmapTile
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class RoomAudioRepository(
    private val database: AppDatabase,
    private val measurementSessionDao: MeasurementSessionDao,
    private val audioSampleDao: AudioSampleDao,
    private val frequencyBinDao: FrequencyBinDao,
    private val soundClassificationDao: SoundClassificationDao
) : AudioRepository {

    override suspend fun createMeasurementSession(startTimestamp: Long): Result<Long> {
        return withContext(Dispatchers.IO) {
            runCatching {
                measurementSessionDao.insertSession(
                    MeasurementSession(startTimestamp = startTimestamp)
                )
            }
        }
    }

    override suspend fun finishMeasurementSession(
        sessionId: Long,
        endTimestamp: Long,
        durationMs: Long
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                measurementSessionDao.finishSession(sessionId, endTimestamp, durationMs)
            }
        }
    }

    override suspend fun saveCompleteAudioSample(
        sessionId: Long?,
        timestamp: Long,
        durationMs: Long,
        avgDb: Float,
        peakDb: Float,
        latitude: Double,
        longitude: Double,
        spectralEnergy: FloatArray,
        labels: Map<String, Float>,
        dominantFreq: Float,
        weighting: String,
        calibrationOffset: Float
    ): Result<Long> {
        return withContext(Dispatchers.IO) {
            runCatching {
                database.withTransaction {
                    val sample = AudioSample(
                        sessionId = sessionId,
                        timestamp = timestamp,
                        durationMs = durationMs,
                        latitude = latitude,
                        longitude = longitude,
                        avgDb = avgDb,
                        peakDb = peakDb,
                        dominantFreq = dominantFreq,
                        weighting = weighting,
                        calibrationOffset = calibrationOffset
                    )
                    val sampleId = audioSampleDao.insertSampleAndGetId(sample)

                    val bins = spectralEnergy.mapIndexed { index, energy ->
                        FrequencyBin(sampleId = sampleId, band = index, energy = energy)
                    }
                    frequencyBinDao.insertBins(bins)

                    val classifications = labels.map { (label, prob) ->
                        SoundClassification(sampleId = sampleId, label = label, probability = prob)
                    }
                    soundClassificationDao.insertClassifications(classifications)

                    sampleId
                }
            }.onFailure { error ->
                Log.e("RoomAudioRepository", "Fallo en la operación de guardado", error)
            }
        }
    }

    override suspend fun getHeatmapData(
        octaveIndex: Int,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        startTimestamp: Long,
        endTimestamp: Long,
        startHour: Int?,
        endHour: Int?
    ): List<HeatmapTile> {
        return if (octaveIndex == -1) {
            audioSampleDao.getSamplesForHeatmap(
                startTimestamp,
                endTimestamp,
                minLat,
                maxLat,
                minLon,
                maxLon
            )
                .filter { row -> isInsideHourRange(row.timestamp, startHour, endHour) }
                .groupBy { row -> tileKey(row.latitude, row.longitude) }
                .map { (_, rows) ->
                    HeatmapTile(
                        lat = rows.map { it.latitude }.average(),
                        lon = rows.map { it.longitude }.average(),
                        avgDb = DecibelMath.energeticMeanDb(rows.map { it.avgDb })
                    )
                }
        } else {
            frequencyBinDao.getBandEnergiesForHeatmap(
                octaveIndex,
                startTimestamp,
                endTimestamp,
                minLat,
                maxLat,
                minLon,
                maxLon
            )
                .filter { row -> isInsideHourRange(row.timestamp, startHour, endHour) }
                .groupBy { row -> tileKey(row.latitude, row.longitude) }
                .map { (_, rows) ->
                    HeatmapTile(
                        lat = rows.map { it.latitude }.average(),
                        lon = rows.map { it.longitude }.average(),
                        avgDb = DecibelMath.energeticMeanDb(rows.map { it.energy })
                    )
                }
        }
    }

    private fun tileKey(latitude: Double, longitude: Double): Pair<Int, Int> {
        return Pair((latitude * 10000).toInt(), (longitude * 10000).toInt())
    }

    private fun isInsideHourRange(timestamp: Long, startHour: Int?, endHour: Int?): Boolean {
        if (startHour == null || endHour == null) return true
        val hour = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)
        return if (startHour <= endHour) {
            hour in startHour until endHour
        } else {
            hour >= startHour || hour < endHour
        }
    }
}

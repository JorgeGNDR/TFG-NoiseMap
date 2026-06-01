package com.gandara.tfgjorgegandara.data.repository

import android.util.Log
import com.gandara.tfgjorgegandara.data.local.AudioSample
import com.gandara.tfgjorgegandara.data.local.AudioSampleDao
import com.gandara.tfgjorgegandara.data.local.FrequencyBin
import com.gandara.tfgjorgegandara.data.local.FrequencyBinDao
import com.gandara.tfgjorgegandara.data.local.GeoTileDao
import com.gandara.tfgjorgegandara.data.local.SoundClassification
import com.gandara.tfgjorgegandara.data.local.SoundClassificationDao
import com.gandara.tfgjorgegandara.domain.model.HeatmapTile
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class RoomAudioRepository(
    private val audioSampleDao: AudioSampleDao,
    private val geoTileDao: GeoTileDao,
    private val frequencyBinDao: FrequencyBinDao,
    private val soundClassificationDao: SoundClassificationDao
) : AudioRepository {

    override suspend fun saveCompleteAudioSample(
        avgDb: Float,
        peakDb: Float,
        latitude: Double?,
        longitude: Double?,
        spectralEnergy: FloatArray,
        labels: Map<String, Float>,
        dominantFreq: Float,
        weighting: String
    ) {
        val timestamp = System.currentTimeMillis()

        withContext(Dispatchers.IO) {
            try {
                val sample = AudioSample(
                    timestamp = timestamp,
                    latitude = latitude ?: 0.0,
                    longitude = longitude ?: 0.0,
                    avgDb = avgDb,
                    peakDb = peakDb,
                    dominantFreq = dominantFreq,
                    weighting = weighting
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

                if (latitude != null && longitude != null) {
                    val tileId = calculateTileId(latitude, longitude)
                    val timeBucket = TimeUnit.MILLISECONDS.toHours(timestamp)
                    geoTileDao.upsertSampleToTile(tileId, timeBucket, avgDb.toDouble())
                }
            } catch (e: Exception) {
                Log.e("RoomAudioRepository", "Fallo en la operacion de guardado: ${e.message}")
            }
        }
    }

    override suspend fun getHeatmapData(
        octaveIndex: Int,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        sinceHoursAgo: Int
    ): List<HeatmapTile> {
        val sinceTimestamp = if (sinceHoursAgo <= 0) {
            0L
        } else {
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(sinceHoursAgo.toLong())
        }

        return if (octaveIndex == -1) {
            audioSampleDao.getAverageDbByLocationTile(
                sinceTimestamp,
                minLat,
                maxLat,
                minLon,
                maxLon
            ).map { tile ->
                HeatmapTile(tile.lat, tile.lon, tile.avgDb)
            }
        } else {
            frequencyBinDao.getEnergyByTileAndBand(octaveIndex, sinceTimestamp, minLat, maxLat, minLon, maxLon)
                .map { tile ->
                    HeatmapTile(tile.lat, tile.lon, tile.avgEnergy)
                }
        }
    }

    private fun calculateTileId(lat: Double, lon: Double): String {
        val latGrid = (lat * 10000).toInt()
        val lonGrid = (lon * 10000).toInt()
        return "${latGrid}_${lonGrid}"
    }
}

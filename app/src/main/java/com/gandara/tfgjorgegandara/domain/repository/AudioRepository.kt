package com.gandara.tfgjorgegandara.domain.repository

import android.location.Location
import android.util.Log
import com.gandara.tfgjorgegandara.data.local.AudioSample
import com.gandara.tfgjorgegandara.data.local.AudioSampleDao
import com.gandara.tfgjorgegandara.data.local.FrequencyBin
import com.gandara.tfgjorgegandara.data.local.FrequencyBinDao
import com.gandara.tfgjorgegandara.data.local.GeoTileDao
import com.gandara.tfgjorgegandara.data.local.SoundClassification
import com.gandara.tfgjorgegandara.data.local.SoundClassificationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Repositorio centralizado para la persistencia de datos acusticos y geograficos.
 */
class AudioRepository(
    private val audioSampleDao: AudioSampleDao,
    private val geoTileDao: GeoTileDao,
    private val frequencyBinDao: FrequencyBinDao,
    private val soundClassificationDao: SoundClassificationDao
) {

    /**
     * Almacena una medicion acustica completa, incluyendo metadatos, analisis espectral y clasificacion IA.
     */
    suspend fun saveCompleteAudioSample(
        avgDb: Float,
        peakDb: Float,
        location: Location?,
        spectralEnergy: FloatArray,
        labels: Map<String, Float>,
        dominantFreq: Float = 0f,
        weighting: String = "A"
    ) {
        val timestamp = System.currentTimeMillis()

        withContext(Dispatchers.IO) {
            try {
                val sample = AudioSample(
                    timestamp = timestamp,
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
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

                if (location != null) {
                    val tileId = calculateTileId(location.latitude, location.longitude)
                    val timeBucket = TimeUnit.MILLISECONDS.toHours(timestamp)
                    geoTileDao.upsertSampleToTile(tileId, timeBucket, avgDb.toDouble())
                }
            } catch (e: Exception) {
                Log.e("AudioRepository", "Fallo en la operacion de guardado: ${e.message}")
            }
        }
    }

    /**
     * Recupera los datos de intensidad sonora agregados para el mapa.
     * @param octaveIndex -1 para nivel medio (dB), 0-30 para las bandas estandar.
     */
    suspend fun getHeatmapData(
        octaveIndex: Int,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        sinceHoursAgo: Int = 24
    ): List<TileWithCoords> {
        val sinceTimestamp = if (sinceHoursAgo <= 0) {
            0L
        } else {
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(sinceHoursAgo.toLong())
        }

        return if (octaveIndex == -1) {
            getTilesInBounds(minLat, maxLat, minLon, maxLon, sinceHoursAgo)
        } else {
            frequencyBinDao.getEnergyByTileAndBand(octaveIndex, sinceTimestamp, minLat, maxLat, minLon, maxLon)
                .map { TileWithCoords(it.lat, it.lon, it.avgEnergy) }
        }
    }

    suspend fun getTilesInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        sinceHoursAgo: Int = 24
    ): List<TileWithCoords> {
        val sinceTimestamp = if (sinceHoursAgo <= 0) {
            0L
        } else {
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(sinceHoursAgo.toLong())
        }

        return audioSampleDao.getAverageDbByLocationTile(
            sinceTimestamp,
            minLat,
            maxLat,
            minLon,
            maxLon
        ).map { tile ->
            TileWithCoords(tile.lat, tile.lon, tile.avgDb)
        }
    }

    data class TileWithCoords(val lat: Double, val lon: Double, val avgDb: Double)

    private fun calculateTileId(lat: Double, lon: Double): String {
        val latGrid = (lat * 10000).toInt()
        val lonGrid = (lon * 10000).toInt()
        return "${latGrid}_${lonGrid}"
    }

    companion object {
        val THIRD_OCTAVE_FREQUENCIES = doubleArrayOf(
            16.0, 20.0, 25.0, 31.5, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0, 160.0,
            200.0, 250.0, 315.0, 400.0, 500.0, 630.0, 800.0, 1000.0, 1250.0,
            1600.0, 2000.0, 2500.0, 3150.0, 4000.0, 5000.0, 6300.0, 8000.0,
            10000.0, 12500.0, 16000.0
        )
    }
}

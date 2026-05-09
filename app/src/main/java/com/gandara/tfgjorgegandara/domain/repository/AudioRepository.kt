package com.gandara.tfgjorgegandara.domain.repository

import android.location.Location
import android.util.Log
import com.gandara.tfgjorgegandara.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Repositorio centralizado para la persistencia de datos acústicos y geográficos.
 */
class AudioRepository(
    private val audioSampleDao: AudioSampleDao,
    private val geoTileDao: GeoTileDao,
    private val frequencyBinDao: FrequencyBinDao,
    private val soundClassificationDao: SoundClassificationDao
) {

    /**
     * Almacena una medición acústica completa, incluyendo metadatos, análisis espectral y clasificación IA.
     */
    suspend fun saveCompleteAudioSample(
        avgDb: Float,
        peakDb: Float,
        location: Location?,
        spectralEnergy: FloatArray,
        labels: Map<String, Float>,
        weighting: String = "A"
    ) {
        val timestamp = System.currentTimeMillis()
        
        withContext(Dispatchers.IO) {
            try {
                // Registro principal de la muestra
                val sample = AudioSample(
                    timestamp = timestamp,
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
                    avgDb = avgDb,
                    peakDb = peakDb,
                    weighting = weighting
                )
                val sampleId = audioSampleDao.insertSampleAndGetId(sample)

                // Almacenamiento de la energía por bandas de frecuencia (FFT)
                val bins = spectralEnergy.mapIndexed { index, energy ->
                    FrequencyBin(sampleId = sampleId, band = index, energy = energy)
                }
                frequencyBinDao.insertBins(bins)

                // Almacenamiento de las etiquetas identificadas por el modelo de ML
                val classifications = labels.map { (label, prob) ->
                    SoundClassification(sampleId = sampleId, label = label, probability = prob)
                }
                soundClassificationDao.insertClassifications(classifications)

                // Actualización de la rejilla de agregación espacial (GeoTiles)
                if (location != null) {
                    val tileId = calculateTileId(location.latitude, location.longitude)
                    val timeBucket = TimeUnit.MILLISECONDS.toHours(timestamp)
                    geoTileDao.upsertSampleToTile(tileId, timeBucket, avgDb.toDouble())
                }
                
            } catch (e: Exception) {
                Log.e("AudioRepository", "Fallo en la operación de guardado persistente: ${e.message}")
            }
        }
    }

    /**
     * Recupera los datos de intensidad sonora agregados para una banda de frecuencia específica.
     */
    suspend fun getHeatmapByFrequency(
        band: Int,
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double
    ): List<TileWithCoords> {
        return frequencyBinDao.getEnergyByTileAndBand(band, minLat, maxLat, minLon, maxLon)
            .map { TileWithCoords(it.lat, it.lon, it.avgEnergy) }
    }

    /**
     * Recupera los datos de la rejilla de ruido dentro de unos límites geográficos y temporales.
     */
    suspend fun getTilesInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        sinceHoursAgo: Int = 24
    ): List<TileWithCoords> {
        val sinceBucket = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis()) - sinceHoursAgo
        
        val tiles = geoTileDao.getTilesInBounds(
            sinceBucket,
            (minLat * 10000).toInt(),
            (maxLat * 10000).toInt(),
            (minLon * 10000).toInt(),
            (maxLon * 10000).toInt()
        )

        return tiles.map { tile ->
            val parts = tile.tileId.split("_")
            val lat = parts[0].toDouble() / 10000.0
            val lon = parts[1].toDouble() / 10000.0
            TileWithCoords(lat, lon, tile.avgDb)
        }
    }

    /**
     * Representación simplificada de un área geográfica con su nivel de ruido asociado.
     */
    data class TileWithCoords(val lat: Double, val lon: Double, val avgDb: Double)

    /**
     * Calcula un identificador único para una celda de la rejilla basado en coordenadas (~11m de resolución).
     */
    private fun calculateTileId(lat: Double, lon: Double): String {
        val latGrid = (lat * 10000).toInt()
        val lonGrid = (lon * 10000).toInt()
        return "${latGrid}_${lonGrid}"
    }
}

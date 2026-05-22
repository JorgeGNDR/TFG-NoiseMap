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
        spectralEnergy: FloatArray, // Energías por bandas (ahora 1/3 octava)
        labels: Map<String, Float>,
        dominantFreq: Float = 0f,
        weighting: String = "A"
    ) {
        val timestamp = System.currentTimeMillis()
        
        withContext(Dispatchers.IO) {
            try {
                // Registro principal de la muestra con frecuencia dominante
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

                // Almacenamiento de la energía por bandas
                val bins = spectralEnergy.mapIndexed { index, energy ->
                    FrequencyBin(sampleId = sampleId, band = index, energy = energy)
                }
                frequencyBinDao.insertBins(bins)

                // Almacenamiento de las etiquetas identificadas
                val classifications = labels.map { (label, prob) ->
                    SoundClassification(sampleId = sampleId, label = label, probability = prob)
                }
                soundClassificationDao.insertClassifications(classifications)

                // Actualización de la rejilla de agregación espacial
                if (location != null) {
                    val tileId = calculateTileId(location.latitude, location.longitude)
                    val timeBucket = TimeUnit.MILLISECONDS.toHours(timestamp)
                    geoTileDao.upsertSampleToTile(tileId, timeBucket, avgDb.toDouble())
                }
                
            } catch (e: Exception) {
                Log.e("AudioRepository", "Fallo en la operación de guardado: ${e.message}")
            }
        }
    }

    /**
     * Prepara un contexto textual para Gemini basado en los datos de la muestra.
     */
    fun prepareGeminiPrompt(
        avgDb: Float, 
        peakDb: Float, 
        labels: Map<String, Float>, 
        dominantFreq: Float,
        lat: Double, 
        lon: Double
    ): String {
        val sounds = labels.entries.joinToString { "${it.key} (${(it.value * 100).toInt()}%)" }
        return """
            Analiza el siguiente entorno acústico:
            - Nivel medio: $avgDb dB
            - Pico máximo: $peakDb dB
            - Frecuencia dominante: ${dominantFreq.toInt()} Hz
            - Clasificaciones IA: $sounds
            - Ubicación: $lat, $lon
            
            Explica qué tipo de ruido es, si es perjudicial y qué podría estar causándolo en esa ubicación.
        """.trimIndent()
    }

    /**
     * Recupera los datos de intensidad sonora agregados para una banda de frecuencia específica.
     */
    suspend fun getHeatmapByFrequency(
        band: Int,
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double
    ): List<TileWithCoords> {
        return frequencyBinDao.getEnergyByTileAndBand(band, 0L, minLat, maxLat, minLon, maxLon)
            .map { TileWithCoords(it.lat, it.lon, it.avgEnergy) }
    }

    /**
     * Recupera los datos de intensidad sonora agregados para una banda de frecuencia específica (por tercio de octava).
     * @param octaveIndex -1 para nivel medio (dB), 0-30 para las bandas estándar.
     */
    suspend fun getHeatmapData(
        octaveIndex: Int,
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
        sinceHoursAgo: Int = 24
    ): List<TileWithCoords> {
        val sinceTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(sinceHoursAgo.toLong())
        
        return if (octaveIndex == -1) {
            getTilesInBounds(minLat, maxLat, minLon, maxLon, sinceHoursAgo)
        } else {
            // Ahora las bandas se guardan directamente como índices 0-30 de 1/3 Octava
            frequencyBinDao.getEnergyByTileAndBand(octaveIndex, sinceTimestamp, minLat, maxLat, minLon, maxLon)
                .map { TileWithCoords(it.lat, it.lon, it.avgEnergy) }
        }
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

    companion object {
        val THIRD_OCTAVE_FREQUENCIES = doubleArrayOf(
            16.0, 20.0, 25.0, 31.5, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0, 160.0, 200.0, 250.0, 315.0, 400.0, 500.0, 630.0, 800.0, 1000.0, 1250.0, 1600.0, 2000.0, 2500.0, 3150.0, 4000.0, 5000.0, 6300.0, 8000.0, 10000.0, 12500.0, 16000.0
        )
    }
}

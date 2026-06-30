package com.gandara.tfgjorgegandara.domain.audio

import org.jtransforms.fft.FloatFFT_1D
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class FFTCalculator(private val bufferSize: Int) {
    private val fft = FloatFFT_1D(bufferSize.toLong())
    private val halfSize = bufferSize / 2
    private val floatData = FloatArray(bufferSize)

    data class WeightedResults(val a: Double, val c: Double, val z: Double, val spectrum: FloatArray)

    /**
     * Calcula las ponderaciones A, C y Z y devuelve el espectro completo.
     */
    fun calculateWeightings(audioSamples: ShortArray, sampleRate: Int, offset: Float): WeightedResults {
        // 1. Preparar datos y ejecutar FFT
        performFFT(audioSamples)

        var energyA = 0.0
        var energyC = 0.0
        var energyZ = 0.0
        val spectrumZ = FloatArray(halfSize)
        val binSize = sampleRate.toDouble() / bufferSize

        for (i in 0 until halfSize) {
            val real = floatData[if (i == 0) 0 else 2 * i]
            val imag = if (i == 0) 0f else if (i == halfSize - 1) floatData[1] else floatData[2 * i + 1]

            val magnitude = sqrt(real * real + imag * imag)
            val amplitude = (magnitude * 4.0f) / bufferSize 
            val freq = i * binSize

            if (amplitude > 1e-12) {
                val dbZ = 20 * log10(amplitude.toDouble()) + offset
                spectrumZ[i] = dbZ.toFloat()

                // Acumulación de energía con ponderaciones
                val weightA = getAWeight(freq)
                val weightC = getCWeight(freq)

                energyZ += 10.0.pow(dbZ / 10.0)
                energyA += 10.0.pow((dbZ + weightA) / 10.0)
                energyC += 10.0.pow((dbZ + weightC) / 10.0)
            } else {
                spectrumZ[i] = -20f
            }
        }

        return WeightedResults(
            a = if (energyA > 0) 10 * log10(energyA) else 0.0,
            c = if (energyC > 0) 10 * log10(energyC) else 0.0,
            z = if (energyZ > 0) 10 * log10(energyZ) else 0.0,
            spectrum = spectrumZ
        )
    }

    private fun performFFT(audioSamples: ShortArray) {
        for (i in audioSamples.indices) {
            floatData[i] = audioSamples[i].toFloat() / 32768.0f
        }
        WindowingFunctions.applyHannWindow(floatData)
        fft.realForward(floatData)
    }

    fun getAWeight(f: Double): Double {
        if (f < 20.0) return -50.0
        val f2 = f * f
        val rA = (12194.0.pow(2) * f.pow(4)) / 
                ((f2 + 20.6.pow(2)) * sqrt((f2 + 107.7.pow(2)) * (f2 + 737.9.pow(2))) * (f2 + 12194.0.pow(2)))
        return 20 * log10(rA) + 2.0
    }
    
    fun getCWeight(f: Double): Double {
        if (f < 20.0) return -0.8
        val f2 = f * f
        val rC = (12194.0.pow(2) * f2) / ((f2 + 20.6.pow(2)) * (f2 + 12194.0.pow(2)))
        return 20 * log10(rC) + 0.06
    }
}

package com.gandara.tfgjorgegandara.domain.audio

import com.gandara.tfgjorgegandara.domain.model.ThirdOctaveBands
import kotlin.math.log10
import kotlin.math.pow

object ThirdOctaveCalculator {
    fun calculateBands(
        fftSpectrum: FloatArray,
        sampleRate: Int,
        fftSize: Int
    ): FloatArray {
        val bands = FloatArray(ThirdOctaveBands.CENTER_FREQUENCIES_HZ.size)
        val binSize = sampleRate.toDouble() / fftSize.toDouble()

        ThirdOctaveBands.CENTER_FREQUENCIES_HZ.forEachIndexed { index, centerFreq ->
            val lowerFreq = centerFreq / 1.122
            val upperFreq = centerFreq * 1.122

            var energySum = 0.0
            var count = 0

            fftSpectrum.forEachIndexed { bin, dbValue ->
                val freq = bin * binSize
                if (freq in lowerFreq..upperFreq) {
                    energySum += 10.0.pow(dbValue / 10.0)
                    count++
                }
            }

            bands[index] = if (count > 0) {
                (10.0 * log10(energySum / count)).toFloat()
            } else {
                -20f
            }
        }

        return bands
    }

    fun dominantFrequency(
        fftSpectrum: FloatArray,
        sampleRate: Int,
        fftSize: Int
    ): Float {
        if (fftSpectrum.isEmpty()) return 0f

        val maxIndex = fftSpectrum.indices.maxByOrNull { fftSpectrum[it] } ?: return 0f
        return maxIndex * sampleRate.toFloat() / fftSize.toFloat()
    }
}

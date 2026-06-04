package com.gandara.tfgjorgegandara.dsp

import com.gandara.tfgjorgegandara.domain.model.WeightingType

object SpectrumWeighting {
    fun applyVisualWeighting(
        spectrum: FloatArray,
        weightingType: WeightingType,
        fftCalculator: FFTCalculator,
        sampleRate: Int,
        fftSize: Int
    ): FloatArray {
        val weightedSpectrum = spectrum.clone()

        if (weightingType == WeightingType.Z) return weightedSpectrum

        val binSize = sampleRate.toDouble() / fftSize.toDouble()
        for (i in weightedSpectrum.indices) {
            val weight = when (weightingType) {
                WeightingType.A -> fftCalculator.getAWeight(i * binSize)
                WeightingType.C -> fftCalculator.getCWeight(i * binSize)
                WeightingType.Z -> 0.0
            }
            weightedSpectrum[i] = (weightedSpectrum[i] + weight).toFloat()
        }

        return weightedSpectrum
    }
}

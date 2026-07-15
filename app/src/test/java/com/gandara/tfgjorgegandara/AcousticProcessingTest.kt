package com.gandara.tfgjorgegandara

import com.gandara.tfgjorgegandara.data.ml.StreamingLinearResampler
import com.gandara.tfgjorgegandara.domain.model.ThirdOctaveBands
import com.gandara.tfgjorgegandara.domain.audio.DecibelMath
import com.gandara.tfgjorgegandara.domain.audio.FFTCalculator
import com.gandara.tfgjorgegandara.domain.audio.ThirdOctaveCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AcousticProcessingTest {
    @Test
    fun energeticMeanDoesNotAverageDecibelsArithmetically() {
        val mean = DecibelMath.energeticMeanDb(listOf(40.0, 60.0))

        assertEquals(57.03, mean, 0.01)
    }

    @Test
    fun fftFindsTheFrequencyOfASyntheticTone() {
        val sampleRate = 44_100
        val fftSize = 4_096
        val toneFrequency = 1_000.0
        val samples = sineWave(toneFrequency, sampleRate, fftSize)
        val spectrum = FFTCalculator(fftSize)
            .calculateWeightings(samples, sampleRate, offset = 90f)
            .spectrum

        val dominantFrequency = ThirdOctaveCalculator.dominantFrequency(
            spectrum,
            sampleRate,
            fftSize
        )

        val binWidth = sampleRate.toDouble() / fftSize
        assertEquals(toneFrequency, dominantFrequency.toDouble(), binWidth)
    }

    @Test
    fun oneKilohertzToneFallsInTheExpectedThirdOctaveBand() {
        val sampleRate = 44_100
        val fftSize = 4_096
        val spectrum = FFTCalculator(fftSize)
            .calculateWeightings(sineWave(1_000.0, sampleRate, fftSize), sampleRate, 90f)
            .spectrum

        val bands = ThirdOctaveCalculator.calculateBands(spectrum, sampleRate, fftSize)
        val strongestBand = bands.indices.maxByOrNull { bands[it] }

        assertEquals(
            1_000.0,
            ThirdOctaveBands.CENTER_FREQUENCIES_HZ[strongestBand!!],
            0.0
        )
    }

    @Test
    fun streamingResamplerProducesOneSecondAtSixteenKilohertz() {
        val source = sineWave(1_000.0, sampleRate = 44_100, sampleCount = 44_100)
        val resampler = StreamingLinearResampler(44_100, 16_000)
        var outputSamples = 0

        source.asList().chunked(1_024).forEach { chunk ->
            outputSamples += resampler.process(chunk.toShortArray()).size
        }

        assertTrue(outputSamples in 15_999..16_001)
    }

    private fun sineWave(frequency: Double, sampleRate: Int, sampleCount: Int): ShortArray {
        return ShortArray(sampleCount) { index ->
            (sin(2.0 * PI * frequency * index / sampleRate) * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
    }
}

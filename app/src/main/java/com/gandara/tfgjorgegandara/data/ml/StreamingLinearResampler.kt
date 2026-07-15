package com.gandara.tfgjorgegandara.data.ml

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Remuestreador lineal con continuidad entre bloques consecutivos de audio PCM.
 */
class StreamingLinearResampler(
    private val sourceSampleRate: Int,
    private val targetSampleRate: Int
) {
    private val sourceStep = sourceSampleRate.toDouble() / targetSampleRate.toDouble()
    private var previousSample: Short? = null
    private var nextSourcePosition = 0.0

    init {
        require(sourceSampleRate > 0 && targetSampleRate > 0)
    }

    fun process(input: ShortArray): FloatArray {
        if (input.isEmpty()) return FloatArray(0)

        val hasPreviousSample = previousSample != null
        val combinedSize = input.size + if (hasPreviousSample) 1 else 0
        val outputCapacity = ceil(combinedSize / sourceStep).toInt() + 1
        val output = FloatArray(outputCapacity)
        var outputCount = 0

        fun sampleAt(index: Int): Double {
            val sample = if (hasPreviousSample) {
                if (index == 0) previousSample!! else input[index - 1]
            } else {
                input[index]
            }
            return sample / 32768.0
        }

        while (nextSourcePosition < combinedSize - 1) {
            val lowerIndex = floor(nextSourcePosition).toInt()
            val fraction = nextSourcePosition - lowerIndex
            val lower = sampleAt(lowerIndex)
            val upper = sampleAt(lowerIndex + 1)
            output[outputCount++] = (lower + (upper - lower) * fraction).toFloat()
            nextSourcePosition += sourceStep
        }

        nextSourcePosition -= (combinedSize - 1).toDouble()
        previousSample = input.last()
        return output.copyOf(outputCount)
    }
}

package com.gandara.tfgjorgegandara.domain.audio

import kotlin.math.log10
import kotlin.math.pow

object DecibelMath {
    fun dbToEnergy(db: Double): Double = 10.0.pow(db / 10.0)

    fun energyToDb(energy: Double, fallbackDb: Double = 0.0): Double {
        return if (energy > 0.0) 10.0 * log10(energy) else fallbackDb
    }

    fun energeticMeanDb(values: Iterable<Double>, fallbackDb: Double = 0.0): Double {
        var energySum = 0.0
        var count = 0
        values.forEach { value ->
            energySum += dbToEnergy(value)
            count++
        }
        return if (count > 0) energyToDb(energySum / count, fallbackDb) else fallbackDb
    }

    fun durationWeightedMeanDb(
        values: Iterable<Pair<Double, Long>>,
        fallbackDb: Double = 0.0
    ): Double {
        var weightedEnergySum = 0.0
        var totalDurationMs = 0L
        values.forEach { (db, durationMs) ->
            if (durationMs > 0L) {
                weightedEnergySum += dbToEnergy(db) * durationMs
                totalDurationMs += durationMs
            }
        }
        return if (totalDurationMs > 0L) {
            energyToDb(weightedEnergySum / totalDurationMs, fallbackDb)
        } else {
            fallbackDb
        }
    }
}

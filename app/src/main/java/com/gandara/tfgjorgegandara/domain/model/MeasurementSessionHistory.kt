package com.gandara.tfgjorgegandara.domain.model

import com.gandara.tfgjorgegandara.domain.audio.DecibelMath

data class MeasurementSessionRecord(
    val id: Long,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val durationMs: Long
)

data class MeasurementSessionHistory(
    val session: MeasurementSessionRecord,
    val samples: List<AudioSampleRecord>,
    val averageDb: Float?,
    val peakDb: Float?,
    val weighting: String?
) {
    val effectiveDurationMs: Long
        get() = session.durationMs.takeIf { it > 0L }
            ?: samples.sumOf { it.durationMs }

    companion object {
        fun create(
            session: MeasurementSessionRecord,
            samples: List<AudioSampleRecord>
        ): MeasurementSessionHistory {
            val orderedSamples = samples.sortedBy { it.timestamp }
            val commonWeighting = orderedSamples
                .map { it.weighting }
                .distinct()
                .singleOrNull()

            val average = commonWeighting?.let {
                DecibelMath.durationWeightedMeanDb(
                    orderedSamples.map { sample ->
                        sample.avgDb.toDouble() to sample.durationMs
                    }
                ).toFloat()
            }
            val peak = commonWeighting?.let {
                orderedSamples.maxOfOrNull { sample -> sample.peakDb }
            }

            return MeasurementSessionHistory(
                session = session,
                samples = orderedSamples,
                averageDb = average,
                peakDb = peak,
                weighting = commonWeighting
            )
        }
    }
}

data class HistoryContent(
    val sessions: List<MeasurementSessionHistory> = emptyList(),
    val standaloneSamples: List<AudioSampleRecord> = emptyList()
) {
    val allSamples: List<AudioSampleRecord>
        get() = (sessions.flatMap { it.samples } + standaloneSamples)
            .sortedByDescending { it.timestamp }
}

package com.gandara.tfgjorgegandara.domain.settings

import kotlinx.coroutines.flow.StateFlow

data class AnalyzerSettings(
    val spectrumBufferSize: Int = 4096,
    val calibrationOffset: Float = 90f
)

interface SettingsRepository {
    val state: StateFlow<AnalyzerSettings>
    val availableBufferSizes: List<Int>

    fun setSpectrumBufferSize(size: Int)
    fun setCalibrationOffset(offset: Float)
}

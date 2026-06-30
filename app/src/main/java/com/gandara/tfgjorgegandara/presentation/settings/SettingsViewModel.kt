package com.gandara.tfgjorgegandara.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.gandara.tfgjorgegandara.di.AppContainer
import com.gandara.tfgjorgegandara.domain.settings.AnalyzerSettings
import com.gandara.tfgjorgegandara.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository: SettingsRepository =
        AppContainer.settings(application.applicationContext)

    val settings: StateFlow<AnalyzerSettings> = settingsRepository.state
    val availableBufferSizes: List<Int> = settingsRepository.availableBufferSizes

    fun setSpectrumBufferSize(size: Int) {
        settingsRepository.setSpectrumBufferSize(size)
    }

    fun setCalibrationOffset(offset: Float) {
        settingsRepository.setCalibrationOffset(offset)
    }
}

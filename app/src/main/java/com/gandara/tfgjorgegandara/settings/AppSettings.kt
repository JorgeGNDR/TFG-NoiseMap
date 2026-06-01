package com.gandara.tfgjorgegandara.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppSettingsState(
    val spectrumBufferSize: Int = 4096,
    val calibrationOffset: Float = 90f
)

object AppSettings {
    private const val PREFS_NAME = "noise_map_settings"
    private const val KEY_BUFFER_SIZE = "spectrum_buffer_size"
    private const val KEY_OFFSET = "calibration_offset"

    val availableBufferSizes = listOf(1024, 2048, 4096, 8192)

    private val _state = MutableStateFlow(AppSettingsState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext != null) return

        appContext = context.applicationContext
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bufferSize = prefs.getInt(KEY_BUFFER_SIZE, 4096).takeIf { it in availableBufferSizes } ?: 4096
        val offset = prefs.getFloat(KEY_OFFSET, 90f)

        _state.value = AppSettingsState(
            spectrumBufferSize = bufferSize,
            calibrationOffset = offset
        )
    }

    fun setSpectrumBufferSize(size: Int) {
        if (size !in availableBufferSizes) return
        _state.update { it.copy(spectrumBufferSize = size) }
        persistInt(KEY_BUFFER_SIZE, size)
    }

    fun setCalibrationOffset(offset: Float) {
        val normalizedOffset = offset.coerceIn(60f, 120f)
        _state.update { it.copy(calibrationOffset = normalizedOffset) }
        persistFloat(KEY_OFFSET, normalizedOffset)
    }

    private fun persistInt(key: String, value: Int) {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putInt(key, value)
            ?.apply()
    }

    private fun persistFloat(key: String, value: Float) {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putFloat(key, value)
            ?.apply()
    }
}

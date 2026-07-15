package com.gandara.tfgjorgegandara.domain.audio

interface AudioCaptureSource {
    fun start(onAudioData: (ShortArray) -> Unit)
    fun stop()
}

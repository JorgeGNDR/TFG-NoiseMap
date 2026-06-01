package com.gandara.tfgjorgegandara.data.repository

import android.content.Context
import com.gandara.tfgjorgegandara.data.local.AppDatabase
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository

object RepositoryProvider {
    fun audioRepository(context: Context): AudioRepository {
        val db = AppDatabase.getDatabase(context.applicationContext)
        return RoomAudioRepository(
            db.audioSampleDao(),
            db.geoTileDao(),
            db.frequencyBinDao(),
            db.soundClassificationDao()
        )
    }
}

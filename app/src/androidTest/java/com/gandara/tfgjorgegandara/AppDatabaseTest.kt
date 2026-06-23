package com.gandara.tfgjorgegandara

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gandara.tfgjorgegandara.data.local.AppDatabase
import com.gandara.tfgjorgegandara.data.local.AudioSample
import com.gandara.tfgjorgegandara.data.local.FrequencyBin
import com.gandara.tfgjorgegandara.data.local.SoundClassification
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun completeSampleIsStoredAndRelationsAreDeletedByCascade() = runBlocking {
        val sampleId = database.audioSampleDao().insertSampleAndGetId(
            AudioSample(
                timestamp = 1_000L,
                latitude = 39.48,
                longitude = -0.34,
                avgDb = 55f,
                peakDb = 62f,
                dominantFreq = 1_000f,
                weighting = "A"
            )
        )
        database.frequencyBinDao().insertBins(listOf(FrequencyBin(sampleId, 19, 50f)))
        database.soundClassificationDao().insertClassifications(
            listOf(SoundClassification(sampleId, "Vehicle", 0.82f))
        )

        val storedSample = database.audioSampleDao().getAllSamples().first().single()
        assertEquals(55f, storedSample.avgDb)
        assertEquals(19, database.frequencyBinDao().getBinsForSample(sampleId).single().band)
        assertEquals(
            0.82f,
            database.soundClassificationDao().getClassificationsForSample(sampleId).single().probability
        )

        database.audioSampleDao().deleteSample(storedSample)

        assertTrue(database.frequencyBinDao().getBinsForSample(sampleId).isEmpty())
        assertTrue(database.soundClassificationDao().getClassificationsForSample(sampleId).isEmpty())
    }
}

package com.gandara.tfgjorgegandara.presentation.map

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoiseSurfaceRendererTest {

    private val bounds = MapViewModel.MapBounds(
        minLat = 0.0,
        maxLat = 1.0,
        minLon = 0.0,
        maxLon = 1.0
    )

    @Test
    fun repeatedMeasurementsAtSameLevelDoNotIncreaseDisplayedLevel() {
        val point = MapViewModel.NoiseMapPoint(
            geoPoint = MapViewModel.GeoPoint(latitude = 0.5, longitude = 0.5),
            rawDb = 50.0
        )

        val singleMeasurement = renderCenterPixel(listOf(point))
        val repeatedMeasurements = renderCenterPixel(List(100) { point })

        assertEquals(singleMeasurement, repeatedMeasurements)
    }

    @Test
    fun centerColorDependsOnAcousticLevel() {
        val quietPixel = renderCenterPixel(listOf(pointAt(50.0)))
        val veryHarmfulPixel = renderCenterPixel(listOf(pointAt(90.0)))

        assertTrue(Color.green(quietPixel) > Color.red(quietPixel))
        assertTrue(Color.red(veryHarmfulPixel) > Color.green(veryHarmfulPixel))
        assertTrue(Color.red(quietPixel) != Color.red(veryHarmfulPixel))
    }

    private fun pointAt(db: Double) = MapViewModel.NoiseMapPoint(
        geoPoint = MapViewModel.GeoPoint(latitude = 0.5, longitude = 0.5),
        rawDb = db
    )

    private fun renderCenterPixel(points: List<MapViewModel.NoiseMapPoint>): Int {
        val bitmap = NoiseSurfaceRenderer.render(
            surface = MapViewModel.NoiseSurfaceData(bounds = bounds, points = points),
            viewportWidth = 100,
            viewportHeight = 200
        )
        return bitmap.getPixel(95, 191)
    }
}

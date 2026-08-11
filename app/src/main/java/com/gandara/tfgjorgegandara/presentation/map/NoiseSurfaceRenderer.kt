package com.gandara.tfgjorgegandara.presentation.map

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.tan

/**
 * Genera una superficie ráster a partir de niveles acústicos georreferenciados.
 *
 * El nivel de cada píxel se obtiene mediante una interpolación espacial ponderada.
 * La cantidad de muestras modifica la estimación local, pero no incrementa el color
 * como ocurre con la densidad acumulada de HeatmapLayer.
 */
object NoiseSurfaceRenderer {
    const val GREEN_ANCHOR_DB = 40.0
    const val YELLOW_ANCHOR_DB = 55.0
    const val ORANGE_ANCHOR_DB = 65.0
    const val RED_ANCHOR_DB = 80.0

    val GREEN_COLOR: Int = Color.rgb(67, 160, 71)
    val YELLOW_COLOR: Int = Color.rgb(253, 216, 53)
    val ORANGE_COLOR: Int = Color.rgb(245, 124, 0)
    val RED_COLOR: Int = Color.rgb(211, 47, 47)

    private const val MAX_BITMAP_WIDTH = 192
    private const val MAX_BITMAP_HEIGHT = 384
    private const val MIN_BITMAP_HEIGHT = 128
    private const val INFLUENCE_RADIUS_METERS = 100.0
    private const val EARTH_RADIUS_METERS = 6_378_137.0
    private const val MAX_SURFACE_OPACITY = 0.82

    fun render(
        surface: MapViewModel.NoiseSurfaceData,
        viewportWidth: Int,
        viewportHeight: Int
    ): Bitmap {
        val width = MAX_BITMAP_WIDTH
        val aspectRatio = if (viewportWidth > 0 && viewportHeight > 0) {
            viewportHeight.toDouble() / viewportWidth.toDouble()
        } else {
            1.8
        }
        val height = (width * aspectRatio)
            .toInt()
            .coerceIn(MIN_BITMAP_HEIGHT, MAX_BITMAP_HEIGHT)

        val pixels = IntArray(width * height)
        if (surface.points.isEmpty()) {
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }

        val weightedLevelSum = DoubleArray(pixels.size)
        val weightSum = DoubleArray(pixels.size)
        val strongestInfluence = DoubleArray(pixels.size)
        val bounds = surface.bounds

        val longitudeRange = (bounds.maxLon - bounds.minLon).coerceAtLeast(1e-9)
        val northMercator = mercatorY(bounds.maxLat)
        val southMercator = mercatorY(bounds.minLat)
        val mercatorRange = (northMercator - southMercator).coerceAtLeast(1e-9)
        val centerLatitudeRadians = (
            (bounds.minLat + bounds.maxLat) / 2.0 * PI / 180.0
            )
        val groundScale = cos(centerLatitudeRadians).coerceAtLeast(0.01)
        val projectedWidthMeters =
            EARTH_RADIUS_METERS * longitudeRange * PI / 180.0
        val projectedHeightMeters = EARTH_RADIUS_METERS * mercatorRange
        val radiusX = (
            INFLUENCE_RADIUS_METERS * (width - 1) /
                (projectedWidthMeters * groundScale)
            ).toInt().coerceIn(1, width - 1)
        val radiusY = (
            INFLUENCE_RADIUS_METERS * (height - 1) /
                (projectedHeightMeters * groundScale)
            ).toInt().coerceIn(1, height - 1)
        val sigmaRatio = 1.0 / 2.4
        val twoSigmaRatioSquared = 2.0 * sigmaRatio * sigmaRatio

        surface.points.forEach { point ->
            val centerX = (
                (point.geoPoint.longitude - bounds.minLon) /
                    longitudeRange * (width - 1)
                ).toInt()
            val centerY = (
                (northMercator - mercatorY(point.geoPoint.latitude)) /
                    mercatorRange * (height - 1)
                ).toInt()

            val minX = (centerX - radiusX).coerceAtLeast(0)
            val maxX = (centerX + radiusX).coerceAtMost(width - 1)
            val minY = (centerY - radiusY).coerceAtLeast(0)
            val maxY = (centerY + radiusY).coerceAtMost(height - 1)

            for (y in minY..maxY) {
                val normalizedY = (y - centerY).toDouble() / radiusY
                for (x in minX..maxX) {
                    val normalizedX = (x - centerX).toDouble() / radiusX
                    val normalizedDistanceSquared =
                        normalizedX * normalizedX + normalizedY * normalizedY
                    if (normalizedDistanceSquared > 1.0) continue

                    val influence = exp(
                        -normalizedDistanceSquared / twoSigmaRatioSquared
                    )
                    val index = y * width + x
                    weightedLevelSum[index] += influence * point.rawDb
                    weightSum[index] += influence
                    strongestInfluence[index] = max(strongestInfluence[index], influence)
                }
            }
        }

        pixels.indices.forEach { index ->
            val totalWeight = weightSum[index]
            if (totalWeight <= 1e-9) {
                pixels[index] = Color.TRANSPARENT
            } else {
                val interpolatedDb = weightedLevelSum[index] / totalWeight
                val opacity = strongestInfluence[index]
                    .pow(0.45)
                    .times(MAX_SURFACE_OPACITY)
                pixels[index] = colorForLevel(interpolatedDb, opacity)
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun mercatorY(latitude: Double): Double {
        val safeLatitude = latitude.coerceIn(-85.0, 85.0)
        val radians = safeLatitude * PI / 180.0
        return ln(tan(PI / 4.0 + radians / 2.0))
    }

    private fun colorForLevel(db: Double, opacity: Double): Int {
        val baseColor = when {
            db <= GREEN_ANCHOR_DB -> GREEN_COLOR
            db < YELLOW_ANCHOR_DB -> blend(
                GREEN_COLOR,
                YELLOW_COLOR,
                (db - GREEN_ANCHOR_DB) / (YELLOW_ANCHOR_DB - GREEN_ANCHOR_DB)
            )
            db < ORANGE_ANCHOR_DB -> blend(
                YELLOW_COLOR,
                ORANGE_COLOR,
                (db - YELLOW_ANCHOR_DB) / (ORANGE_ANCHOR_DB - YELLOW_ANCHOR_DB)
            )
            db < RED_ANCHOR_DB -> blend(
                ORANGE_COLOR,
                RED_COLOR,
                (db - ORANGE_ANCHOR_DB) / (RED_ANCHOR_DB - ORANGE_ANCHOR_DB)
            )
            else -> RED_COLOR
        }
        val alpha = (opacity.coerceIn(0.0, 1.0) * 255.0).toInt()
        return Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
    }

    private fun blend(startColor: Int, endColor: Int, progress: Double): Int {
        val amount = progress.coerceIn(0.0, 1.0)
        val red = (Color.red(startColor) + (Color.red(endColor) - Color.red(startColor)) * amount).toInt()
        val green = (
            Color.green(startColor) +
                (Color.green(endColor) - Color.green(startColor)) * amount
            ).toInt()
        val blue = (
            Color.blue(startColor) +
                (Color.blue(endColor) - Color.blue(startColor)) * amount
            ).toInt()
        return Color.rgb(red, green, blue)
    }
}

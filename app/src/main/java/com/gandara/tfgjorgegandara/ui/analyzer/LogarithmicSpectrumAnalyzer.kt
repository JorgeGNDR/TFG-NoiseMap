package com.gandara.tfgjorgegandara.ui.analyzer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gandara.tfgjorgegandara.ui.theme.*
import kotlin.math.log10

private val MIN_FREQ_LOG = log10(20f)
private val MAX_FREQ_LOG = log10(20000f)
private const val MIN_DB = -20f
private const val MAX_DB = 120f
private const val RANGE_DB = MAX_DB - MIN_DB

@Composable
fun LogarithmicSpectrumAnalyzer(
    amplitudesDB : FloatArray,
    peakHoldDB: FloatArray = FloatArray(0),
    modifier: Modifier = Modifier,
    sampleRate: Float = 44100f
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = TextDark.copy(alpha = 0.3f), fontSize = 8.sp)
    val peakLabelStyle = TextStyle(color = TextDark.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)

    val majorHz = listOf(62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    val majorDb = listOf(-20f, 0f, 20f, 40f, 60f, 80f, 100f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val marginV = 20.dp.toPx()
        val marginH = 4.dp.toPx()
        val chartWidth = width - (marginH * 2)
        val chartHeight = height - (marginV * 2)
        val chartLeft = marginH
        val chartTop = marginV

        val majorGridColor = TextDark.copy(alpha = 0.12f)

        // 1. Ejes Hz
        majorHz.forEach { freq ->
            val x = chartLeft + ((log10(freq) - MIN_FREQ_LOG) / (MAX_FREQ_LOG - MIN_FREQ_LOG)) * chartWidth
            drawLine(color = majorGridColor, start = Offset(x, chartTop), end = Offset(x, chartTop + chartHeight))
            val label = (if (freq >= 1000f) "${(freq / 1000).toInt()}k" else "${freq.toInt()}") + (if (freq == 62f) " Hz" else "")
            val textLayout = textMeasurer.measure(label, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                style = labelStyle,
                topLeft = Offset(x - textLayout.size.width / 2, chartTop - textLayout.size.height - 4f)
            )
        }

        // 2. Ejes dB
        majorDb.forEach { db ->
            val y = chartTop + chartHeight - (((db - MIN_DB) / RANGE_DB) * chartHeight)
            drawLine(color = majorGridColor, start = Offset(chartLeft, y), end = Offset(chartLeft + chartWidth, y))
            val label = "${db.toInt()}" + (if (db == -20f) " dB(A)" else "")
            val textLayout = textMeasurer.measure(label, labelStyle)
            val yOffset = if (db == 120f) 2f else -textLayout.size.height - 2f
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                style = labelStyle,
                topLeft = Offset(chartLeft + 4f, y + yOffset)
            )
        }

        // 3. Función auxiliar para crear Path
        fun createPath(data: FloatArray): Path? {
            if (data.isEmpty()) return null
            val path = Path()
            val binSize = sampleRate / (data.size * 2)
            val pixelWidth = chartWidth.toInt()
            val pixelMaxDb = FloatArray(pixelWidth) { -100f }
            
            for (i in 1 until data.size) {
                val freq = i * binSize
                if (freq in 20f..20000f) {
                    val x = (((log10(freq) - MIN_FREQ_LOG) / (MAX_FREQ_LOG - MIN_FREQ_LOG)) * (pixelWidth - 1)).toInt()
                    if (data[i] > pixelMaxDb[x]) pixelMaxDb[x] = data[i]
                }
            }

            var started = false
            for (x in 0 until pixelWidth) {
                if (pixelMaxDb[x] > -100f) {
                    val xPos = chartLeft + x.toFloat()
                    val yPos = chartTop + chartHeight - (((pixelMaxDb[x].coerceIn(MIN_DB, MAX_DB) - MIN_DB) / RANGE_DB) * chartHeight)
                    if (!started) { path.moveTo(xPos, yPos); started = true } else path.lineTo(xPos, yPos)
                }
            }
            return if (started) path else null
        }

        // 4. Dibujar Curva de Picos (Peak Hold)
        createPath(peakHoldDB)?.let {
            drawPath(path = it, color = PeakHoldBlue.copy(alpha = 0.65f), style = Stroke(width = 1.2.dp.toPx()))
        }

        // 5. Dibujar Curva Tiempo Real y Seguimiento de Pico Texto
        createPath(amplitudesDB)?.let {
            drawPath(path = it, color = PowerOrange, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))
            
            var maxDb = -100f
            var maxFreq = 0f
            val binSize = sampleRate / (amplitudesDB.size * 2)
            for (i in 1 until amplitudesDB.size) {
                if (amplitudesDB[i] > maxDb) { maxDb = amplitudesDB[i]; maxFreq = i * binSize }
            }
            if (maxFreq > 0) {
                val label = "%.1f Hz".format(maxFreq)
                val layout = textMeasurer.measure(label, peakLabelStyle)
                val x = chartLeft + ((log10(maxFreq.coerceIn(20f, 20000f)) - MIN_FREQ_LOG) / (MAX_FREQ_LOG - MIN_FREQ_LOG)) * chartWidth
                val y = chartTop + chartHeight - (((maxDb.coerceIn(MIN_DB, MAX_DB) - MIN_DB) / RANGE_DB) * chartHeight)
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    style = peakLabelStyle,
                    topLeft = Offset(
                        (x - layout.size.width / 2).coerceIn(chartLeft, chartLeft + chartWidth - layout.size.width),
                        (y - layout.size.height - 10f).coerceIn(chartTop, chartTop + chartHeight)
                    )
                )
            }
        }
    }
}

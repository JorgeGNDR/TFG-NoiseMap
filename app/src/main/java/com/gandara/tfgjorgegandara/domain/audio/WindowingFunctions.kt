package com.gandara.tfgjorgegandara.domain.audio

import kotlin.math.cos
import kotlin.math.PI

object WindowingFunctions {
    // Aplica la Ventana de Hann a un array de datos (float)
    // Modificacion del array original por referencia para ahorrar memoria RAM
    fun applyHannWindow(samples: FloatArray) {
        val n = samples.size
        for (i in 0 until n) {
            // Fórmula de la Ventana de Hann
            val multuplier = 0.5 * (1.0 - cos(2.0 * PI * i / (n - 1.0)))

            // Multiplicamos la muestra de audio original por nuestra curva suavizadora
            samples[i] = (samples[i] * multuplier).toFloat()
        }
    }
}

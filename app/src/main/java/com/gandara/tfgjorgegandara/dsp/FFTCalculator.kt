package com.gandara.tfgjorgegandara.dsp

import org.jtransforms.fft.FloatFFT_1D
import kotlin.math.log10
import kotlin.math.sqrt

class FFTCalculator(private val bufferSize: Int) {
    // Instanciamos el motor matemático.
    private val fft = FloatFFT_1D(bufferSize.toLong())

    /**
     * Recibe un bloque de audio bruto (PCM 16-bit) y devuelve un array con los niveles
     * en Decibelios (dB) para cada "banda" de frecuencia.
     */
    fun calculateFFT(audioSamples: ShortArray, offset: Float = 90f): FloatArray {
        // 1. Convertir y normalizar los datos de audio a valores de punto flotante.
        // Pasamos de Short a Float y dividimos por 32768.0 para normalizar.
        val floatData = FloatArray(bufferSize)
        for (i in audioSamples.indices) {
            floatData[i] = audioSamples[i].toFloat() / 32768.0f
        }

        // 2. Aplicar la ventana de Hann a los datos.
        WindowingFunctions.applyHannWindow(floatData)

        // 3. Calcular la FFT.
        fft.realForward(floatData)

        // 4. Calcular los niveles en decibelios (dB) para cada banda de frecuencia.
        // Teorema de Nyquist: de 1024 muestras, solo obtenemos 512 frecuencias.
        val halfSize = bufferSize / 2
        val levels = FloatArray(halfSize)

        // JTransforms empaqueta los datos en un array de números complejos:
        // floatData[0] = Parte real de 0 Hz
        // floatData[1] = Parte real de la máxima frecuencia
        // A partir del indice 2 van en parejas: [Real, Imaginario, Real, Imaginario...]

        // Calculamos la primera barra (0 Hz o Corriente Continua)
        levels[0] = calculateMagnitudInDb(floatData[0], 0f, offset)

        // Calculamos las demás barras (Frecuencias)
        for (i in 1 until halfSize) {
            val realPart = floatData[2 * i]
            val imaginaryPart = floatData[2 * i + 1]
            levels[i] = calculateMagnitudInDb(realPart, imaginaryPart, offset)
        }

        return levels // Array que enviamos a la UI
    }

    private fun calculateMagnitudInDb(real: Float, imaginary: Float, offset: Float): Float {
        // Magnitud de un número complejo: sqrt(a^2 + b^2)
        val magnitude = sqrt(real * real + imaginary * imaginary)
        // Decibeles (dB): 20 * log10(magnitud) + offset
        return if (magnitude > 0) {
            var db = 20f * log10(magnitude) + offset
            if (db < 0f) db = 0f
            db
        } else {
            0f
        }
    }
}
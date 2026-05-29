package com.gandara.tfgjorgegandara.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Esquema de colores para el Modo Oscuro (Opcional, pero buena práctica)
// Para el Neumorfismo puro, a veces se usa el mismo gris, o se crea un gris más oscuro.
// Por ahora, aplicamos tus colores base.
private val DarkColorScheme = darkColorScheme(
    primary = RecordRed,
    background = NeumorphicBackground,
    surface = NeumorphicBackground,
    onPrimary = NeumorphicBackground,
    onBackground = TextDark,
    onSurface = TextDark
)

// Esquema de colores para el Modo Claro (El de tu Figma)
private val LightColorScheme = lightColorScheme(
    primary = PowerOrange,
    background = NeumorphicBackground, // El fondo gris de tu Figma
    surface = NeumorphicBackground,    // Tarjetas y barras de navegación
    onPrimary = NeumorphicBackground,  // Texto/Iconos sobre el color primario (Rojo)
    onBackground = TextDark,           // Texto principal sobre el fondo
    onSurface = TextDark               // Texto sobre tarjetas/barras
)

@Composable
fun NoiseMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // IMPORTANTE: Ponemos dynamicColor a FALSE por defecto para que Android 12+
    // no nos cambie el fondo y rompa el efecto Neumórfico.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Asegúrate de que Type.kt existe y no tiene errores
        content = content
    )
}
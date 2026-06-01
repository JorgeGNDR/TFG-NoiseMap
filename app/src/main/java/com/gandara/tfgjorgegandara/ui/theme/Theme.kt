package com.gandara.tfgjorgegandara.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PowerOrange,
    onPrimary = DeepBlack,
    primaryContainer = Color(0xFFFFD8C8),
    onPrimaryContainer = DeepBlack,
    secondary = SoftPanel,
    onSecondary = DeepBlack,
    secondaryContainer = InkBlack,
    onSecondaryContainer = WarmWhite,
    tertiary = WarmWhite,
    onTertiary = DeepBlack,
    background = NeumorphicBackground,
    onBackground = TextDark,
    surface = NeumorphicBackground,
    onSurface = TextDark,
    surfaceVariant = SoftPanel,
    onSurfaceVariant = TextDark,
    outline = TextGray,
    outlineVariant = Color(0xFFC8CFCD),
    error = PowerOrange,
    onError = DeepBlack,
    errorContainer = PowerOrange,
    onErrorContainer = DeepBlack
)

private val LightColorScheme = lightColorScheme(
    primary = PowerOrange,
    onPrimary = DeepBlack,
    primaryContainer = Color(0xFFFFD8C8),
    onPrimaryContainer = DeepBlack,
    secondary = InkBlack,
    onSecondary = WarmWhite,
    secondaryContainer = SoftPanel,
    onSecondaryContainer = DeepBlack,
    tertiary = SoftPanel,
    onTertiary = DeepBlack,
    background = NeumorphicBackground,
    onBackground = TextDark,
    surface = NeumorphicBackground,
    onSurface = TextDark,
    surfaceVariant = SoftPanel,
    onSurfaceVariant = TextDark,
    outline = TextGray,
    outlineVariant = Color(0xFFC8CFCD),
    inverseSurface = DeepBlack,
    inverseOnSurface = WarmWhite,
    inversePrimary = PowerOrange,
    error = PowerOrange,
    onError = DeepBlack,
    errorContainer = PowerOrange,
    onErrorContainer = DeepBlack
)

@Composable
fun NoiseMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        typography = Typography,
        content = content
    )
}

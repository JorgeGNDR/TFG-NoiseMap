package com.gandara.tfgjorgegandara.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neumorphic(
    cornerRadius: Dp = 24.dp,
    lightShadowColor: Color = LightShadow,
    darkShadowColor: Color = DarkShadow
) = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = android.graphics.Color.TRANSPARENT

        // Sombra Oscura (Figma: X=28, Y=28, Blur=50)
        frameworkPaint.maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.NORMAL)
        canvas.save()
        canvas.translate(28f, 28f)
        frameworkPaint.setShadowLayer(50f, 0f, 0f, darkShadowColor.toArgb())
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            cornerRadius.toPx(), cornerRadius.toPx(), paint
        )
        canvas.restore()

        // Sombra Clara (Figma: X=-23, Y=-23, Blur=45)
        frameworkPaint.maskFilter = BlurMaskFilter(45f, BlurMaskFilter.Blur.NORMAL)
        canvas.save()
        canvas.translate(-23f, -23f)
        frameworkPaint.setShadowLayer(45f, 0f, 0f, lightShadowColor.toArgb())
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            cornerRadius.toPx(), cornerRadius.toPx(), paint
        )
        canvas.restore()
    }
}
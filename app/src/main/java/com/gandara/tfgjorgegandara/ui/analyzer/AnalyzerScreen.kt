package com.gandara.tfgjorgegandara.ui.analyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.gandara.tfgjorgegandara.R
import com.gandara.tfgjorgegandara.ui.viewmodels.AnalyzerViewModel
import com.gandara.tfgjorgegandara.ui.viewmodels.WeightingType
import com.gandara.tfgjorgegandara.ui.theme.*
import com.gandara.tfgjorgegandara.ui.viewmodels.AnalyzerState

@Composable
fun AnalyzerScreen(
    viewModel: AnalyzerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicBackground)
            .padding(horizontal = 12.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Indicadores Superiores
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(85.dp).clickable(interactionSource, null) { viewModel.resetAvg() }
            ) {
                Text(text = "${state.avg.toInt()}", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = "AVG", fontSize = 14.sp, color = TextGray)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).clickable(interactionSource, null) { viewModel.resetCurrentDb() }
            ) {
                Text(text = "${state.decibels.toInt()}", fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                Text(text = "dB(${state.selectedWeighting})", fontSize = 18.sp, color = TextGray)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(85.dp).clickable(interactionSource, null) { viewModel.resetPeak() }
            ) {
                Text(text = "${state.peak.toInt()}", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = "PEAK", fontSize = 14.sp, color = TextGray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Selector Ponderación + Botón Pausa
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .neumorphic(cornerRadius = 22.dp)
                    .background(NeumorphicBackground, shape = RoundedCornerShape(22.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeightingType.values().forEach { type ->
                    val isSelected = state.selectedWeighting == type
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) RecordRed.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { viewModel.setWeighting(type) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = type.name, color = if (isSelected) RecordRed else TextGray, fontSize = 13.sp)
                    }
                }
            }

            // Botón de Pausa (Freeze) usando Drawables
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .neumorphic(cornerRadius = 22.dp)
                    .background(NeumorphicBackground, shape = CircleShape)
                    .clickable { viewModel.togglePause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (state.isPaused) R.drawable.play_arrow_24px else R.drawable.pause_24px
                    ),
                    contentDescription = "Pausa",
                    tint = if (state.isPaused) RecordRed else TextDark,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Contenedor de la Gráfica
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .neumorphic(cornerRadius = 16.dp)
                .background(NeumorphicBackground, shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            LogarithmicSpectrumAnalyzer(
                amplitudesDB = state.spectrum,
                peakHoldDB = state.peakHoldSpectrum
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cuadro de estado dinámico
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .neumorphic(cornerRadius = 16.dp)
                .background(NeumorphicBackground, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                val statusText = when {
                    state.isCapturing -> "Capturando muestra: ${(state.captureProgress * 100).toInt()}%"
                    state.isPaused -> "Señal congelada (Pausa)"
                    else -> "Identificando: ${state.detectedSound.uppercase()}"
                }
                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isCapturing) RecordRed else TextDark
                )

                if (!state.isCapturing && !state.isPaused) {
                    Text(
                        text = "Escucha activa ambiental...",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SoundDebugPanel(state = state)

        Spacer(modifier = Modifier.weight(1f))

        // Botón de Captura (5 Segundos)
        Box(
            modifier = Modifier
                .size(85.dp)
                .neumorphic(cornerRadius = 42.dp)
                .background(NeumorphicBackground, shape = CircleShape)
                .clickable { viewModel.startCaptureSession() },
            contentAlignment = Alignment.Center
        ) {
            // Círculo interno que cambia según si está capturando
            Box(
                modifier = Modifier
                    .size(if (state.isCapturing) 30.dp else 45.dp)
                    .clip(if (state.isCapturing) RoundedCornerShape(6.dp) else CircleShape)
                    .background(if (state.isCapturing) TextGray else RecordRed)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("CAPTURAR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGray)
    }
}

@Composable
fun SoundDebugPanel(state: AnalyzerState) {Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp)) // Más oscuro y redondeado
        .padding(16.dp)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (state.isPaused) Color.Gray else Color.Red)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "ANÁLISIS EN TIEMPO REAL",
            color = Color.Yellow,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    val displayText = if (state.detectedSound.isEmpty()) "Escuchando..." else state.detectedSound

    Text(
        text = displayText,
        color = Color.White,
        fontSize = 22.sp, // Más grande para que destaque
        fontWeight = FontWeight.Bold
    )

    Text(
        text = if (state.isPaused) "Análisis pausado" else "Detectando patrones sonoros...",
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 11.sp
    )
}
}
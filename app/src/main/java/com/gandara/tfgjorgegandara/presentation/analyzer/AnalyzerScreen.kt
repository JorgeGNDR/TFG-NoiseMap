package com.gandara.tfgjorgegandara.presentation.analyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gandara.tfgjorgegandara.presentation.common.LocationViewModel

import com.gandara.tfgjorgegandara.R
import com.gandara.tfgjorgegandara.domain.model.WeightingType
import com.gandara.tfgjorgegandara.presentation.theme.*

/**
 * Pantalla principal del analizador acústico.
 * Visualiza niveles de presión sonora (AVG, dB(), PEAK), espectro de frecuencias (logarítmico) e identificación de sonidos (YAMNET).
 * Botón de grabación de muestras, pausa, selector de ponderación
 */
@Composable
fun AnalyzerScreen(
    viewModel: AnalyzerViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    onRequestLocationPermission: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    var showWeightingInfo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicBackground)
            .padding(horizontal = 12.dp)
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Indicadores de arriba (AVG, dB actual, PEAK)
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

        // Controles de configuración (Ponderación y Congelación de señal)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
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
                                .background(if (isSelected) PowerOrange.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { viewModel.setWeighting(type) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = type.name, color = if (isSelected) PowerOrange else TextGray, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .neumorphic(cornerRadius = 18.dp)
                        .background(NeumorphicBackground, shape = CircleShape)
                        .clickable { showWeightingInfo = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "?",
                        color = TextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
                    contentDescription = "Control de pausa",
                    tint = if (state.isPaused) PowerOrange else TextDark,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Visualización del espectrograma en tiempo real
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .neumorphic(cornerRadius = 16.dp)
                .background(NeumorphicBackground, shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            LogarithmicSpectrumAnalyzer(
                amplitudesDB = state.spectrum,
                peakHoldDB = state.peakHoldSpectrum
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Panel de información de estado, clasificación y diagnóstico GPS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 8.dp)
                .neumorphic(cornerRadius = 16.dp)
                .background(NeumorphicBackground, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                val statusText = when {
                    state.isCapturing -> "GRABANDO MUESTRA: ${(state.captureProgress * 100).toInt()}%"
                    state.isSaving -> "GUARDANDO MUESTRA..."
                    state.captureFeedback != null -> state.captureFeedback.orEmpty().uppercase()
                    state.isPaused -> "SEÑAL CONGELADA"
                    else -> state.detectedSound.ifEmpty { "Escuchando..." }.uppercase()
                }
                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isCapturing || state.isSaving) PowerOrange else TextDark
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Boton de capturar muestra de 3s
        Box(
            modifier = Modifier
                .size(65.dp)
                .neumorphic(cornerRadius = 42.dp)
                .background(NeumorphicBackground, shape = CircleShape)
                .clickable(enabled = !state.isCapturing && !state.isSaving) {
                    if (currentLocation == null) {
                        onRequestLocationPermission()
                    }
                    viewModel.startCaptureSession(currentLocation)
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (state.isCapturing) 30.dp else 45.dp)
                    .clip(if (state.isCapturing) RoundedCornerShape(6.dp) else CircleShape)
                    .background(if (state.isCapturing) TextGray else PowerOrange)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showWeightingInfo) {
        WeightingInfoDialog(onDismiss = { showWeightingInfo = false })
    }
}

@Composable
private fun WeightingInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ponderaciones acústicas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WeightingInfoLine(
                    title = "A",
                    body = "Aproxima cómo percibe el oído humano. Reduce mucho los graves y los agudos extremos. Es la más habitual para medir ruido ambiental."
                )
                WeightingInfoLine(
                    title = "C",
                    body = "Mantiene más peso en las bajas frecuencias. Es útil para sonidos fuertes, graves o impactos."
                )
                WeightingInfoLine(
                    title = "Z",
                    body = "No aplica corrección perceptiva. Muestra la energía de la señal de forma más plana y técnica."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}

@Composable
private fun WeightingInfoLine(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Ponderación $title",
            color = PowerOrange,
            fontWeight = FontWeight.Bold
        )
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}

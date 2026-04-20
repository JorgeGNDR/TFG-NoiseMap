package com.gandara.tfgjorgegandara.ui.analyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.gandara.tfgjorgegandara.ui.viewmodels.AnalyzerViewModel
import com.gandara.tfgjorgegandara.ui.theme.*

@Composable
fun AnalyzerScreen(
    viewModel: AnalyzerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicBackground)
            .padding(horizontal = 12.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Indicadores Superiores
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${state.avg.toInt()}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = "AVG", fontSize = 16.sp, color = TextGray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${state.decibels.toInt()}", fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                Text(text = "dB", fontSize = 24.sp, color = TextGray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${state.peak.toInt()}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = "PEAK", fontSize = 16.sp, color = TextGray)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Contenedor de la Gráfica
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .neumorphic(cornerRadius = 16.dp)
                .background(NeumorphicBackground, shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            LogarithmicSpectrumAnalyzer(
                amplitudesDB = state.spectrum,
                peakHoldDB = state.peakHoldSpectrum // Pasamos la nueva curva de picos
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Cuadro de estado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .neumorphic(cornerRadius = 16.dp)
                .background(NeumorphicBackground, shape = RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                text = if (state.isRecording) "Grabando..." else "Pulsa el botón para tomar muestra.\nEsperando...",
                fontSize = 16.sp, 
                color = TextDark
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón Circular de Grabación
        Box(
            modifier = Modifier
                .size(80.dp)
                .neumorphic(cornerRadius = 40.dp)
                .background(NeumorphicBackground, shape = CircleShape)
                .clickable { viewModel.toggleRecording() },
            contentAlignment = Alignment.Center
        ) {
            val centerColor = if (state.isRecording) TextGray else RecordRed
            val centerShape = if (state.isRecording) RoundedCornerShape(8.dp) else CircleShape
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(centerShape)
                    .background(centerColor)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
package com.gandara.tfgjorgegandara.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gandara.tfgjorgegandara.domain.settings.AnalyzerSettings
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()

    SettingsScreen(
        settings = settings,
        availableBufferSizes = viewModel.availableBufferSizes,
        onBufferSizeChange = viewModel::setSpectrumBufferSize,
        onCalibrationOffsetChange = viewModel::setCalibrationOffset
    )
}

@Composable
fun SettingsScreen(
    settings: AnalyzerSettings,
    availableBufferSizes: List<Int>,
    onBufferSizeChange: (Int) -> Unit,
    onCalibrationOffsetChange: (Float) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        SettingsPanel(title = "Analisis del espectro") {
            BufferSizeDropdown(
                selectedBufferSize = settings.spectrumBufferSize,
                availableBufferSizes = availableBufferSizes,
                onBufferSizeChange = onBufferSizeChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = bufferSizeDescription(settings.spectrumBufferSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsPanel(title = "Calibracion") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Offset dB", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${settings.calibrationOffset.roundToInt()} dB",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = settings.calibrationOffset,
                onValueChange = onCalibrationOffsetChange,
                valueRange = 60f..120f,
                steps = 59,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Text(
                text = "Sube o baja el offset si comparas la app con una referencia externa. Afecta al nivel dB mostrado y a las muestras nuevas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsPanel(title = "Como usar la app") {
            InfoLine("Analizador", "Muestra el nivel actual, la media, el pico, el espectro y el sonido detectado.")
            InfoLine("Capturar", "Pulsa el boton circular para guardar una muestra de 3 segundos con ubicacion.")
            InfoLine("Mapa", "Visualiza las muestras por nivel global o por tercio de octava. Usa el filtro de tiempo para ver datos antiguos.")
            InfoLine("Historial", "Revisa cada medicion, borra muestras y genera una explicacion breve con IA.")
            InfoLine("Calibracion", "Usa el offset solo si tienes una referencia fiable, como un sonometro o una medicion conocida.")
        }
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun BufferSizeDropdown(
    selectedBufferSize: Int,
    availableBufferSizes: List<Int>,
    onBufferSizeChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "$selectedBufferSize muestras",
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableBufferSizes.forEach { size ->
                DropdownMenuItem(
                    text = { Text("$size muestras") },
                    onClick = {
                        expanded = false
                        onBufferSizeChange(size)
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoLine(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun bufferSizeDescription(bufferSize: Int): String {
    return when (bufferSize) {
        1024 -> "Respuesta muy rápida y baja latencia, con menos detalle espectral y graves menos precisos."
        2048 -> "Respuesta más rápida y visualización más ágil, con menos precisión en bajas frecuencias."
        4096 -> "Equilibrio recomendado entre fluidez y detalle espectral."
        else -> "Tamaño de buffer personalizado."
    }
}

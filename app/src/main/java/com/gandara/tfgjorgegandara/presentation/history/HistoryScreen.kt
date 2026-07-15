package com.gandara.tfgjorgegandara.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gandara.tfgjorgegandara.domain.model.AudioSampleRecord
import com.gandara.tfgjorgegandara.domain.model.FullAudioSample
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de histórico que permite explorar las mediciones pasadas y sus metadatos detallados.
 */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val samples by viewModel.samples.collectAsState()
    val details by viewModel.selectedSampleDetails.collectAsState()
    val explainingSampleId by viewModel.explainingSampleId.collectAsState()
    val explanationError by viewModel.explanationError.collectAsState()
    var sampleToDelete by remember { mutableStateOf<AudioSampleRecord?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Historial de Muestras",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(samples) { sample ->
                SampleItem(
                    sample = sample,
                    isSelected = details?.sample?.id == sample.id,
                    onClick = { viewModel.loadSampleDetails(sample) },
                    onDelete = { sampleToDelete = sample },
                    onExplain = { viewModel.explainSample(sample) },
                    isExplaining = explainingSampleId == sample.id,
                    details = if (details?.sample?.id == sample.id) details else null
                )
            }
        }
    }

    if (sampleToDelete != null) {
        AlertDialog(
            onDismissRequest = { sampleToDelete = null },
            title = { Text("Eliminar muestra") },
            text = { Text("Estás seguro de que quieres eliminar esta medición? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    sampleToDelete?.let { viewModel.deleteSample(it) }
                    sampleToDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sampleToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    explanationError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExplanationError() },
            title = { Text("Explicación no disponible") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearExplanationError() }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

/**
 * Representación visual de una medición individual dentro del listado.
 */
@Composable
fun SampleItem(
    sample: AudioSampleRecord,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExplain: () -> Unit,
    isExplaining: Boolean,
    details: FullAudioSample?
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val dateString = dateFormat.format(Date(sample.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ID: ${sample.id}", style = MaterialTheme.typography.labelLarge)
                Text(dateString, style = MaterialTheme.typography.bodySmall)
            }

            Text(
                text = "Nivel: ${"%.1f".format(sample.avgDb)} dB (${sample.weighting})",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            AnimatedVisibility(visible = isSelected) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Ubicación: ${sample.latitude}, ${sample.longitude}", style = MaterialTheme.typography.bodyMedium)
                    Text("Nivel pico: ${"%.1f".format(sample.peakDb)} dB", style = MaterialTheme.typography.bodyMedium)

                    details?.let { data ->
                        if (data.classifications.isNotEmpty()) {
                            val classificationText = data.classifications.joinToString {
                                "${it.label} (${"%.0f".format(it.probability * 100)}%)"
                            }
                            Text("IA: $classificationText", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (data.bins.isNotEmpty()) {
                            Text("Resolucion espectral: ${data.bins.size} bandas registradas", style = MaterialTheme.typography.bodyMedium)
                        }

                        val explanation = data.sample.aiExplanation
                        if (!explanation.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Explicación IA",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(explanation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onExplain,
                        enabled = !isExplaining,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isExplaining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generando explicación...")
                        } else {
                            Text(if (sample.aiExplanation.isNullOrBlank()) "Explicar muestra" else "Actualizar explicación")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Borrar muestra")
                    }
                }
            }
        }
    }
}

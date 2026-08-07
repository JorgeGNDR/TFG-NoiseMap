package com.gandara.tfgjorgegandara.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gandara.tfgjorgegandara.domain.model.AudioSampleRecord
import com.gandara.tfgjorgegandara.domain.model.FullAudioSample
import com.gandara.tfgjorgegandara.domain.model.MeasurementSessionHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryMode {
    SESSIONS,
    SAMPLES
}

/**
 * Pantalla de histórico que permite consultar las sesiones y sus muestras individuales.
 */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val history by viewModel.historyContent.collectAsState()
    val samples by viewModel.samples.collectAsState()
    val details by viewModel.selectedSampleDetails.collectAsState()
    val explainingSampleId by viewModel.explainingSampleId.collectAsState()
    val explanationError by viewModel.explanationError.collectAsState()
    val deletionError by viewModel.deletionError.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedSampleIds by viewModel.selectedSampleIds.collectAsState()
    val isDeletingSamples by viewModel.isDeletingSamples.collectAsState()

    var historyModeName by rememberSaveable { mutableStateOf(HistoryMode.SESSIONS.name) }
    val historyMode = HistoryMode.valueOf(historyModeName)
    var expandedSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sampleToDelete by remember { mutableStateOf<AudioSampleRecord?>(null) }
    var sessionToDelete by remember { mutableStateOf<MeasurementSessionHistory?>(null) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        HistoryHeader(
            historyMode = historyMode,
            isSelectionMode = isSelectionMode,
            selectedCount = selectedSampleIds.size,
            sampleCount = samples.size,
            isDeletingSamples = isDeletingSamples,
            onSelect = viewModel::enterSelectionMode,
            onSelectAll = viewModel::selectAllSamples,
            onDeleteSelected = { showDeleteSelectedDialog = true },
            onCancelSelection = viewModel::exitSelectionMode
        )

        Spacer(modifier = Modifier.height(8.dp))

        TabRow(selectedTabIndex = historyMode.ordinal) {
            Tab(
                selected = historyMode == HistoryMode.SESSIONS,
                onClick = {
                    historyModeName = HistoryMode.SESSIONS.name
                    viewModel.exitSelectionMode()
                },
                text = { Text("Sesiones (${history.sessions.size})") }
            )
            Tab(
                selected = historyMode == HistoryMode.SAMPLES,
                onClick = { historyModeName = HistoryMode.SAMPLES.name },
                text = { Text("Muestras (${samples.size})") }
            )
        }

        if (isDeletingSamples) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (historyMode == HistoryMode.SESSIONS) {
            SessionHistoryList(
                sessions = history.sessions,
                standaloneSamples = history.standaloneSamples,
                expandedSessionId = expandedSessionId,
                details = details,
                explainingSampleId = explainingSampleId,
                onSessionClick = { sessionId ->
                    expandedSessionId = if (expandedSessionId == sessionId) null else sessionId
                },
                onDeleteSession = { sessionToDelete = it },
                onSampleClick = viewModel::loadSampleDetails,
                onDeleteSample = { sampleToDelete = it },
                onExplainSample = viewModel::explainSample,
                modifier = Modifier.weight(1f)
            )
        } else {
            SampleHistoryList(
                samples = samples,
                details = details,
                explainingSampleId = explainingSampleId,
                isSelectionMode = isSelectionMode,
                selectedSampleIds = selectedSampleIds,
                onSampleClick = viewModel::loadSampleDetails,
                onToggleSelection = viewModel::toggleSampleSelection,
                onDeleteSample = { sampleToDelete = it },
                onExplainSample = viewModel::explainSample,
                modifier = Modifier.weight(1f)
            )
        }
    }

    sampleToDelete?.let { sample ->
        AlertDialog(
            onDismissRequest = { sampleToDelete = null },
            title = { Text("Eliminar muestra") },
            text = { Text("¿Estás seguro de que quieres eliminar esta medición? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSample(sample)
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

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Eliminar sesión") },
            text = {
                Text(
                    "Se eliminarán la sesión y sus ${session.samples.size} muestras. " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(session)
                    if (expandedSessionId == session.session.id) expandedSessionId = null
                    sessionToDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteSelectedDialog) {
        val selectedCount = selectedSampleIds.size
        val selectedText = if (selectedCount == 1) {
            "la muestra seleccionada"
        } else {
            "las $selectedCount muestras seleccionadas"
        }
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text(if (selectedCount == 1) "Eliminar muestra" else "Eliminar muestras") },
            text = { Text("¿Quieres eliminar $selectedText? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSelectedDialog = false
                        viewModel.deleteSelectedSamples()
                    },
                    enabled = selectedCount > 0
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    explanationError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearExplanationError,
            title = { Text("Explicación no disponible") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearExplanationError) {
                    Text("Aceptar")
                }
            }
        )
    }

    deletionError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearDeletionError,
            title = { Text("No se pudo completar el borrado") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearDeletionError) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
private fun HistoryHeader(
    historyMode: HistoryMode,
    isSelectionMode: Boolean,
    selectedCount: Int,
    sampleCount: Int,
    isDeletingSamples: Boolean,
    onSelect: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCancelSelection: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isSelectionMode) selectionLabel(selectedCount) else "Historial",
            modifier = Modifier.weight(1f),
            style = if (isSelectionMode) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.headlineMedium
            },
            color = MaterialTheme.colorScheme.primary
        )

        if (historyMode == HistoryMode.SAMPLES) {
            if (isSelectionMode) {
                TextButton(
                    onClick = onSelectAll,
                    enabled = sampleCount > 0 && selectedCount < sampleCount
                ) {
                    Text("Todas")
                }
                IconButton(
                    onClick = onDeleteSelected,
                    enabled = selectedCount > 0 && !isDeletingSamples
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar muestras seleccionadas",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                IconButton(onClick = onCancelSelection, enabled = !isDeletingSamples) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar selección")
                }
            } else {
                TextButton(onClick = onSelect, enabled = sampleCount > 0) {
                    Text("Seleccionar")
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryList(
    sessions: List<MeasurementSessionHistory>,
    standaloneSamples: List<AudioSampleRecord>,
    expandedSessionId: Long?,
    details: FullAudioSample?,
    explainingSampleId: Long?,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (MeasurementSessionHistory) -> Unit,
    onSampleClick: (AudioSampleRecord) -> Unit,
    onDeleteSample: (AudioSampleRecord) -> Unit,
    onExplainSample: (AudioSampleRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        if (sessions.isEmpty() && standaloneSamples.isEmpty()) {
            item {
                Text(
                    text = "Todavía no hay mediciones guardadas.",
                    modifier = Modifier.padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        items(sessions, key = { "session-${it.session.id}" }) { session ->
            SessionItem(
                session = session,
                isExpanded = expandedSessionId == session.session.id,
                details = details,
                explainingSampleId = explainingSampleId,
                onClick = { onSessionClick(session.session.id) },
                onDeleteSession = { onDeleteSession(session) },
                onSampleClick = onSampleClick,
                onDeleteSample = onDeleteSample,
                onExplainSample = onExplainSample
            )
        }

        if (standaloneSamples.isNotEmpty()) {
            item {
                Text(
                    text = "Muestras anteriores sin sesión",
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(standaloneSamples, key = { "standalone-${it.id}" }) { sample ->
                SampleItem(
                    sample = sample,
                    isExpanded = details?.sample?.id == sample.id,
                    isSelectionMode = false,
                    isChecked = false,
                    onClick = { onSampleClick(sample) },
                    onSelectionToggle = {},
                    onDelete = { onDeleteSample(sample) },
                    onExplain = { onExplainSample(sample) },
                    isExplaining = explainingSampleId == sample.id,
                    details = details.takeIf { it?.sample?.id == sample.id }
                )
            }
        }
    }
}

@Composable
private fun SampleHistoryList(
    samples: List<AudioSampleRecord>,
    details: FullAudioSample?,
    explainingSampleId: Long?,
    isSelectionMode: Boolean,
    selectedSampleIds: Set<Long>,
    onSampleClick: (AudioSampleRecord) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onDeleteSample: (AudioSampleRecord) -> Unit,
    onExplainSample: (AudioSampleRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        if (samples.isEmpty()) {
            item {
                Text(
                    text = "Todavía no hay muestras guardadas.",
                    modifier = Modifier.padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        items(samples, key = { it.id }) { sample ->
            SampleItem(
                sample = sample,
                isExpanded = !isSelectionMode && details?.sample?.id == sample.id,
                isSelectionMode = isSelectionMode,
                isChecked = sample.id in selectedSampleIds,
                onClick = {
                    if (isSelectionMode) onToggleSelection(sample.id) else onSampleClick(sample)
                },
                onSelectionToggle = { onToggleSelection(sample.id) },
                onDelete = { onDeleteSample(sample) },
                onExplain = { onExplainSample(sample) },
                isExplaining = explainingSampleId == sample.id,
                details = details.takeIf { it?.sample?.id == sample.id }
            )
        }
    }
}

@Composable
private fun SessionItem(
    session: MeasurementSessionHistory,
    isExpanded: Boolean,
    details: FullAudioSample?,
    explainingSampleId: Long?,
    onClick: () -> Unit,
    onDeleteSession: () -> Unit,
    onSampleClick: (AudioSampleRecord) -> Unit,
    onDeleteSample: (AudioSampleRecord) -> Unit,
    onExplainSample: (AudioSampleRecord) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(session.session.startTimestamp))
    val sampleLabel = if (session.samples.size == 1) "1 muestra" else "${session.samples.size} muestras"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sesión #${session.session.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(dateString, style = MaterialTheme.typography.bodySmall)
            }

            Text(
                text = "$sampleLabel · ${formatDuration(session.effectiveDurationMs)}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (session.averageDb != null && session.peakDb != null && session.weighting != null) {
                Text(
                    text = "Media: ${"%.1f".format(session.averageDb)} dB (${session.weighting}) · " +
                        "Pico: ${"%.1f".format(session.peakDb)} dB",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "La sesión contiene ponderaciones diferentes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = "Segmentos de la sesión",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    session.samples.forEach { sample ->
                        SampleItem(
                            sample = sample,
                            isExpanded = details?.sample?.id == sample.id,
                            isSelectionMode = false,
                            isChecked = false,
                            onClick = { onSampleClick(sample) },
                            onSelectionToggle = {},
                            onDelete = { onDeleteSample(sample) },
                            onExplain = { onExplainSample(sample) },
                            isExplaining = explainingSampleId == sample.id,
                            details = details.takeIf { it?.sample?.id == sample.id }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDeleteSession,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Borrar sesión completa")
                    }
                }
            }
        }
    }
}

private fun selectionLabel(count: Int): String {
    return if (count == 1) "1 seleccionada" else "$count seleccionadas"
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) "${minutes} min ${seconds} s" else "${seconds} s"
}

/** Representación visual de una medición individual dentro del listado. */
@Composable
fun SampleItem(
    sample: AudioSampleRecord,
    isExpanded: Boolean,
    isSelectionMode: Boolean,
    isChecked: Boolean,
    onClick: () -> Unit,
    onSelectionToggle: () -> Unit,
    onDelete: () -> Unit,
    onExplain: () -> Unit,
    isExplaining: Boolean,
    details: FullAudioSample?
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(sample.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isChecked -> MaterialTheme.colorScheme.secondaryContainer
                isExpanded -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onSelectionToggle() }
                    )
                }

                Text(
                    text = "ID: ${sample.id}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(dateString, style = MaterialTheme.typography.bodySmall)
            }

            Text(
                text = "Nivel: ${"%.1f".format(sample.avgDb)} dB (${sample.weighting})",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Duración: ${formatDuration(sample.durationMs)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Ubicación: ${sample.latitude}, ${sample.longitude}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Nivel pico: ${"%.1f".format(sample.peakDb)} dB",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    details?.let { data ->
                        if (data.classifications.isNotEmpty()) {
                            val classificationText = data.classifications.joinToString {
                                "${it.label} (${"%.0f".format(it.probability * 100)}%)"
                            }
                            Text("IA: $classificationText", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (data.bins.isNotEmpty()) {
                            Text(
                                text = "Resolución espectral: ${data.bins.size} bandas registradas",
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Generando explicación...")
                        } else {
                            Text(
                                if (sample.aiExplanation.isNullOrBlank()) {
                                    "Explicar muestra"
                                } else {
                                    "Actualizar explicación"
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Borrar muestra")
                    }
                }
            }
        }
    }
}

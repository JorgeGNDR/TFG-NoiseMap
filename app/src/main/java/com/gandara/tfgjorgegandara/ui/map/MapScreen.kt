package com.gandara.tfgjorgegandara.ui.map

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gandara.tfgjorgegandara.ui.common.LocationViewModel
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/**
 * Pantalla del mapa que visualiza los datos de intensidad sonora geolocalizados.
 */
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    mapViewModel: MapViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val heatmapPoints by mapViewModel.heatmapPoints.collectAsState()
    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()
    
    val selectedTime by mapViewModel.selectedTimeRange.collectAsState()
    val selectedBand by mapViewModel.selectedFrequencyBand.collectAsState()

    val mapView = remember { MapView(context) }
    
    // Gestión del centrado automático del mapa en la posición actual del usuario
    var hasCenteredInitially by remember { mutableStateOf(false) }
    LaunchedEffect(currentLocation) {
        if (currentLocation != null && !hasCenteredInitially) {
            mapView.controller.animateTo(GeoPoint(currentLocation!!.latitude, currentLocation!!.longitude))
            hasCenteredInitially = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Integración del componente MapView de OSMDroid
        AndroidView(
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.5)
                    
                    // Listener diferido para refrescar los datos al finalizar el movimiento o zoom
                    addMapListener(DelayedMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            updateHeatmap(mapView, mapViewModel)
                            return true
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            updateHeatmap(mapView, mapViewModel)
                            return true
                        }
                    }, 500))
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                // Actualización de la capa del marcador del usuario
                mv.overlays.removeAll { it is Marker && it.id == "user_loc" }
                currentLocation?.let {
                    val userMarker = Marker(mv).apply {
                        position = GeoPoint(it.latitude, it.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Ubicación actual"
                        id = "user_loc"
                    }
                    mv.overlays.add(userMarker)
                }

                // Actualización de la capa del mapa de calor
                mv.overlays.removeAll { it is Polygon && (it.id == "heat_point") }
                heatmapPoints.forEach { point ->
                    val color = when {
                        point.intensity < 0.4 -> AndroidColor.argb(130, 76, 175, 80)
                        point.intensity < 0.7 -> AndroidColor.argb(130, 255, 235, 59)
                        else -> AndroidColor.argb(130, 244, 67, 54)
                    }
                    val circle = Polygon(mv).apply {
                        id = "heat_point"
                        points = Polygon.pointsAsCircle(point.geoPoint, 12.0)
                        fillPaint.color = color
                        outlinePaint.strokeWidth = 0f
                    }
                    mv.overlays.add(circle)
                }
                mv.invalidate()
            }
        )

        // Superposición de filtros de visualización
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Selección de banda de frecuencia
            FilterRow(
                options = listOf("Global", "Bajos", "Medios", "Agudos"),
                selected = selectedBand,
                onSelected = { mapViewModel.setFrequencyBand(it) }
            )

            // Selección de periodo temporal
            val timeOptions = mapOf(24 to "24H", 168 to "1 SEM", 8760 to "TODO")
            FilterRow(
                options = timeOptions.values.toList(),
                selected = timeOptions[selectedTime] ?: "24H",
                onSelected = { label -> 
                    val hours = timeOptions.entries.find { it.value == label }?.key ?: 24
                    mapViewModel.setTimeRange(hours)
                }
            )
        }

        // Botón flotante para reposicionar el mapa en el usuario
        FloatingActionButton(
            onClick = {
                currentLocation?.let {
                    mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Centrar en mi ubicación")
        }

        // Barra de progreso superior para procesos de carga asíncronos
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Componente para renderizar una fila de botones de filtro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterRow(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(options) { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(option, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.Transparent,
                    enabled = true,
                    selected = selected == option
                )
            )
        }
    }
}

/**
 * Actualiza los puntos de calor del ViewModel basándose en los límites visibles actuales.
 */
private fun updateHeatmap(mapView: MapView, viewModel: MapViewModel) {
    val bounds = mapView.boundingBox
    viewModel.updateHeatMap(bounds.latSouth, bounds.latNorth, bounds.lonWest, bounds.lonEast)
}

package com.gandara.tfgjorgegandara.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository
import com.gandara.tfgjorgegandara.ui.common.LocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.HeatmapLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Pantalla del mapa profesional utilizando MapLibre GL.
 * Visualiza datos acústicos mediante capas de calor vectoriales de alto rendimiento.
 */
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    mapViewModel: MapViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val heatmapPoints by mapViewModel.heatmapPoints.collectAsState()
    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()
    val selectedBandIndex by mapViewModel.selectedBandIndex.collectAsState()

    // Inicializar MapLibre (necesario una sola vez)
    LaunchedEffect(Unit) {
        MapLibre.getInstance(context)
    }

    val mapView = remember { MapView(context) }
    var mapLibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }

    // Gestión del centrado automático (Corregido para reaccionar cuando el mapa esté listo)
    var hasCenteredInitially by remember { mutableStateOf(false) }
    LaunchedEffect(currentLocation, mapLibreMap) {
        val map = mapLibreMap
        val loc = currentLocation
        if (loc != null && map != null && !hasCenteredInitially) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(loc.latitude, loc.longitude), 
                    16.0
                )
            )
            hasCenteredInitially = true
        }
    }

    // Actualización de datos en el mapa (Optimizado con Dispatchers.Default)
    LaunchedEffect(heatmapPoints, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        
        // Procesar la colección de features en un hilo de fondo para evitar jank en la UI
        val featureCollection = withContext(Dispatchers.Default) {
            val features = heatmapPoints.map { point ->
                Feature.fromGeometry(
                    Point.fromLngLat(point.geoPoint.longitude, point.geoPoint.latitude)
                ).apply {
                    addNumberProperty("db", point.rawDb)
                    addNumberProperty("intensity", point.intensity)
                }
            }
            FeatureCollection.fromFeatures(features)
        }

        map.getStyle { style ->
            style.getSourceAs<GeoJsonSource>("noise-source")?.setGeoJson(featureCollection)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    onCreate(null) // MapLibre requiere llamadas explícitas al ciclo de vida
                    getMapAsync { map ->
                        mapLibreMap = map
                        
                        // Usamos un estilo que consume OpenStreetMap (vía MapTiler)
                        map.setStyle(Style.Builder().fromUri("https://api.maptiler.com/maps/dataviz-v4/style.json?key=u1kPuvsJUJUIxnU1Ost0")) { style ->
                            setupHeatmapLayer(style)

                            // Configuración del indicador de ubicación (Punto Azul)
                            map.locationComponent.apply {
                                activateLocationComponent(
                                    org.maplibre.android.location.LocationComponentActivationOptions
                                        .builder(context, style)
                                        .build()
                                )
                                isLocationComponentEnabled = true
                                renderMode = org.maplibre.android.location.modes.RenderMode.COMPASS
                            }
                            
                            // Listener para actualizaciones de cámara
                            map.addOnCameraIdleListener {
                                val bounds = map.projection.visibleRegion.latLngBounds
                                mapViewModel.updateHeatMap(
                                    bounds.latitudeSouth, bounds.latitudeNorth, 
                                    bounds.longitudeWest, bounds.longitudeEast
                                )
                            }
                        }
                    }
                }
            },
            update = { mv ->
                // Sincronización del ciclo de vida con Compose
            },
            onRelease = { mv ->
                mv.onStop()
                mv.onDestroy()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Observador de ciclo de vida para MapLibre
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // UI de Control (Slicer de 1/3 Octava)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrequencySlicerCard(
                selectedIndex = selectedBandIndex,
                onIndexChange = { mapViewModel.setFrequencyBandIndex(it) }
            )
        }

        // Botón de centrado
        FloatingActionButton(
            onClick = {
                currentLocation?.let {
                    mapLibreMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude),
                            16.0 // Zoom cercano estándar al centrar
                        )
                    )
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

/**
 * Configura la capa de calor profesional de MapLibre.
 */
private fun setupHeatmapLayer(style: Style) {
    val source = GeoJsonSource("noise-source")
    style.addSource(source)

    val layer = HeatmapLayer("noise-heatmap", "noise-source")
    layer.setProperties(
        // El peso determina la contribución de cada punto. 
        // Al usar la intensidad (0.1 a 1.0), los puntos ruidosos "dominan" el color.
        PropertyFactory.heatmapWeight(
            Expression.get("intensity")
        ),
        // FIJAMOS la intensidad del mapa de calor a 1.
        // Esto evita que al alejar el zoom o acumular puntos, los colores se "sumen" y cambien a rojo por densidad.
        PropertyFactory.heatmapIntensity(1f),
        
        // Radio de difusión que se ajusta con el zoom para que las manchas no se solapen demasiado
        PropertyFactory.heatmapRadius(
            Expression.interpolate(
                Expression.linear(), Expression.zoom(),
                Expression.stop(0, 2),
                Expression.stop(10, 15),
                Expression.stop(20, 40)
            )
        ),
        // Rampa de color basada en la intensidad real del punto (o conjunto de puntos cercanos)
        PropertyFactory.heatmapColor(
            Expression.interpolate(
                Expression.linear(), Expression.heatmapDensity(),
                Expression.stop(0.0, Expression.color(Color.Transparent.toArgb())),
                Expression.stop(0.1, Expression.color(Color(0xFF4CAF50).toArgb())), // Verde (~30-40 dB)
                Expression.stop(0.3, Expression.color(Color(0xFF8BC34A).toArgb())), // Verde Lima
                Expression.stop(0.5, Expression.color(Color(0xFFFBC02D).toArgb())), // Amarillo (~60 dB)
                Expression.stop(0.7, Expression.color(Color(0xFFF57C00).toArgb())), // Naranja (~75 dB)
                Expression.stop(1.0, Expression.color(Color(0xFFD32F2F).toArgb()))  // Rojo (90+ dB)
            )
        ),
        PropertyFactory.heatmapOpacity(0.8f)
    )
    style.addLayer(layer)
}

@Composable
fun FrequencySlicerCard(selectedIndex: Int, onIndexChange: (Int) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val label = if (selectedIndex == -1) "Nivel Global (dB)" 
                        else "${AudioRepository.THIRD_OCTAVE_FREQUENCIES[selectedIndex].toInt()} Hz (1/3 Oct)"
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Slider(
                value = selectedIndex.toFloat(),
                onValueChange = { onIndexChange(it.toInt()) },
                valueRange = -1f..30f,
                steps = 30
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("16Hz", style = MaterialTheme.typography.bodySmall)
                Text("1kHz", style = MaterialTheme.typography.bodySmall)
                Text("16kHz", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

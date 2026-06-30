package com.gandara.tfgjorgegandara.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.gandara.tfgjorgegandara.BuildConfig
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gandara.tfgjorgegandara.domain.model.ThirdOctaveBands
import com.gandara.tfgjorgegandara.presentation.common.LocationViewModel
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
 * Visualiza datos acusticos mediante capas de calor vectoriales de alto rendimiento.
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
    val errorMessage by mapViewModel.errorMessage.collectAsState()
    val selectedBandIndex by mapViewModel.selectedBandIndex.collectAsState()
    val selectedTimeRange by mapViewModel.selectedTimeRange.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            mapViewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        MapLibre.getInstance(context)
    }

    val mapView = remember { MapView(context) }
    var mapLibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }

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

    LaunchedEffect(heatmapPoints, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect

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
                    onCreate(null)
                    getMapAsync { map ->
                        mapLibreMap = map

                        val mapStyleUrl = "https://api.maptiler.com/maps/dataviz-v4/style.json" +
                            "?key=${BuildConfig.MAPTILER_API_KEY}"
                        map.setStyle(Style.Builder().fromUri(mapStyleUrl)) { style ->
                            setupHeatmapLayer(style)

                            if (context.hasLocationPermission()) {
                                map.locationComponent.apply {
                                    activateLocationComponent(
                                        org.maplibre.android.location.LocationComponentActivationOptions
                                            .builder(context, style)
                                            .build()
                                    )
                                    isLocationComponentEnabled = true
                                    renderMode = org.maplibre.android.location.modes.RenderMode.COMPASS
                                }
                            }

                            map.addOnCameraIdleListener {
                                val bounds = map.projection.visibleRegion.latLngBounds
                                mapViewModel.updateHeatMap(
                                    bounds.latitudeSouth,
                                    bounds.latitudeNorth,
                                    bounds.longitudeWest,
                                    bounds.longitudeEast
                                )
                            }
                        }
                    }
                }
            },
            update = {},
            onRelease = { mv ->
                mv.onStop()
                mv.onDestroy()
            },
            modifier = Modifier.fillMaxSize()
        )

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

        FrequencySlicerCard(
            selectedIndex = selectedBandIndex,
            onIndexChange = { mapViewModel.setFrequencyBandIndex(it) },
            selectedTimeRange = selectedTimeRange,
            onTimeRangeChange = { mapViewModel.setTimeRange(it) },
            onLocateClick = {
                currentLocation?.let {
                    mapLibreMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude),
                            16.0
                        )
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp)
        )

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )
    }
}

/**
 * Configura la capa de calor de MapLibre.
 */
private fun setupHeatmapLayer(style: Style) {
    val source = GeoJsonSource("noise-source")
    style.addSource(source)

    val layer = HeatmapLayer("noise-heatmap", "noise-source")
    layer.setProperties(
        PropertyFactory.heatmapWeight(Expression.get("intensity")),
        PropertyFactory.heatmapIntensity(
            Expression.interpolate(
                Expression.linear(), Expression.zoom(),
                Expression.stop(0, 0.05f),
                Expression.stop(10, 0.3f),
                Expression.stop(15, 1.0f)
            )
        ),
        PropertyFactory.heatmapRadius(
            Expression.interpolate(
                Expression.linear(), Expression.zoom(),
                Expression.stop(0, 2),
                Expression.stop(10, 15),
                Expression.stop(20, 40)
            )
        ),
        PropertyFactory.heatmapColor(
            Expression.interpolate(
                Expression.linear(), Expression.heatmapDensity(),
                Expression.stop(0.0, Expression.color(Color.Transparent.toArgb())),
                Expression.stop(0.1, Expression.color(Color(0xFF4CAF50).toArgb())),
                Expression.stop(0.4, Expression.color(Color(0xFFFBC02D).toArgb())),
                Expression.stop(0.6, Expression.color(Color(0xFFF57C00).toArgb())),
                Expression.stop(0.8, Expression.color(Color(0xFFD32F2F).toArgb())),
                Expression.stop(1.0, Expression.color(Color(0xFFB71C1C).toArgb()))
            )
        ),
        PropertyFactory.heatmapOpacity(0.8f)
    )
    style.addLayer(layer)
}

@Composable
fun FrequencySlicerCard(
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    selectedTimeRange: Int,
    onTimeRangeChange: (Int) -> Unit,
    onLocateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = frequencyLabel(selectedIndex),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TimeRangeDropdown(
                    selectedTimeRange = selectedTimeRange,
                    onTimeRangeChange = onTimeRangeChange
                )

                FilledIconButton(
                    onClick = onLocateClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Ubicacion actual")
                }
            }

            Slider(
                value = selectedIndex.toFloat(),
                onValueChange = { onIndexChange(it.toInt()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                valueRange = -1f..30f,
                steps = 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Global", style = MaterialTheme.typography.labelSmall)
                Text("1kHz", style = MaterialTheme.typography.labelSmall)
                Text("16kHz", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TimeRangeDropdown(
    selectedTimeRange: Int,
    onTimeRangeChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        24 to "24h",
        24 * 7 to "7d",
        24 * 30 to "30d",
        -1 to "Todo"
    )
    val selectedLabel = options.firstOrNull { it.first == selectedTimeRange }?.second ?: "24h"

    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
        ) {
            Text(selectedLabel)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (hours, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onTimeRangeChange(hours)
                    }
                )
            }
        }
    }
}

private fun frequencyLabel(selectedIndex: Int): String {
    return if (selectedIndex == -1) {
        "Nivel Global"
    } else {
        "${ThirdOctaveBands.CENTER_FREQUENCIES_HZ[selectedIndex].toInt()} Hz"
    }
}

private fun Context.hasLocationPermission(): Boolean {
    val fineLocationGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseLocationGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fineLocationGranted || coarseLocationGranted
}

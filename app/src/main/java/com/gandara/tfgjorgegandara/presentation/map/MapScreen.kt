package com.gandara.tfgjorgegandara.presentation.map

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gandara.tfgjorgegandara.BuildConfig
import com.gandara.tfgjorgegandara.domain.model.ThirdOctaveBands
import com.gandara.tfgjorgegandara.presentation.common.LocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngQuad
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.ImageSource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val NoiseGreen = Color(NoiseSurfaceRenderer.GREEN_COLOR)
private val NoiseYellow = Color(NoiseSurfaceRenderer.YELLOW_COLOR)
private val NoiseOrange = Color(NoiseSurfaceRenderer.ORANGE_COLOR)
private val NoiseRed = Color(NoiseSurfaceRenderer.RED_COLOR)
private const val NOISE_SURFACE_SOURCE_ID = "noise-surface-source"
private const val NOISE_SURFACE_LAYER_ID = "noise-surface-layer"

@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    mapViewModel: MapViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val noiseSurface by mapViewModel.noiseSurface.collectAsState()
    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()
    val errorMessage by mapViewModel.errorMessage.collectAsState()
    val selectedBandIndex by mapViewModel.selectedBandIndex.collectAsState()
    val dateFilter by mapViewModel.dateFilter.collectAsState()
    val hourFilter by mapViewModel.hourFilter.collectAsState()
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

    LaunchedEffect(noiseSurface, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val surface = noiseSurface ?: return@LaunchedEffect
        val bitmap = withContext(Dispatchers.Default) {
            NoiseSurfaceRenderer.render(
                surface = surface,
                viewportWidth = mapView.width,
                viewportHeight = mapView.height
            )
        }
        val coordinates = surface.bounds.toLatLngQuad()

        map.getStyle { style ->
            style.getSourceAs<ImageSource>(NOISE_SURFACE_SOURCE_ID)?.apply {
                setCoordinates(coordinates)
                setImage(bitmap)
            }
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
                            setupNoiseSurfaceLayer(style)

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
            dateFilter = dateFilter,
            onDateFilterModeChange = { mapViewModel.setDateFilterMode(it) },
            onSingleDateChange = { mapViewModel.setSingleDate(it) },
            onRangeStartDateChange = { mapViewModel.setRangeStartDate(it) },
            onRangeEndDateChange = { mapViewModel.setRangeEndDate(it) },
            hourFilter = hourFilter,
            onAllDayChange = { mapViewModel.setAllDayFilter(it) },
            onStartHourChange = { mapViewModel.setStartHour(it) },
            onEndHourChange = { mapViewModel.setEndHour(it) },
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

private fun setupNoiseSurfaceLayer(style: Style) {
    val initialCoordinates = LatLngQuad(
        LatLng(0.001, -0.001),
        LatLng(0.001, 0.001),
        LatLng(-0.001, 0.001),
        LatLng(-0.001, -0.001)
    )
    val emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val source = ImageSource(NOISE_SURFACE_SOURCE_ID, initialCoordinates, emptyBitmap)
    style.addSource(source)

    val layer = RasterLayer(NOISE_SURFACE_LAYER_ID, NOISE_SURFACE_SOURCE_ID)
    layer.setProperties(
        PropertyFactory.rasterOpacity(1.0f),
        PropertyFactory.rasterFadeDuration(0.0f),
        PropertyFactory.rasterResampling(Property.RASTER_RESAMPLING_LINEAR)
    )
    style.addLayer(layer)
}

private fun MapViewModel.MapBounds.toLatLngQuad(): LatLngQuad {
    return LatLngQuad(
        LatLng(maxLat, minLon),
        LatLng(maxLat, maxLon),
        LatLng(minLat, maxLon),
        LatLng(minLat, minLon)
    )
}

@Composable
fun FrequencySlicerCard(
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    dateFilter: MapViewModel.DateFilter,
    onDateFilterModeChange: (MapViewModel.DateFilterMode) -> Unit,
    onSingleDateChange: (Long) -> Unit,
    onRangeStartDateChange: (Long) -> Unit,
    onRangeEndDateChange: (Long) -> Unit,
    hourFilter: MapViewModel.HourFilter,
    onAllDayChange: (Boolean) -> Unit,
    onStartHourChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit,
    onLocateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMapInfo by remember { mutableStateOf(false) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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

                FilledIconButton(
                    onClick = { showMapInfo = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .semantics { contentDescription = "Información sobre el mapa" },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                    Icon(Icons.Default.LocationOn, contentDescription = "Ubicación actual")
                }
            }

            TextButton(
                onClick = { filtersExpanded = !filtersExpanded },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Filtros del mapa",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (filtersExpanded) "Ocultar" else "Mostrar",
                    style = MaterialTheme.typography.labelMedium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    modifier = Modifier.rotate(if (filtersExpanded) 180f else 0f),
                    contentDescription = if (filtersExpanded) "Plegar filtros" else "Desplegar filtros"
                )
            }

            if (!filtersExpanded) {
                Text(
                    text = "${dateFilter.label} · " +
                        if (hourFilter.allDay) "Todo el día" else hourFilter.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            AnimatedVisibility(visible = filtersExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Global", style = MaterialTheme.typography.labelSmall)
                        Text("1 kHz", style = MaterialTheme.typography.labelSmall)
                        Text("16 kHz", style = MaterialTheme.typography.labelSmall)
                    }

                    MapFilterControls(
                        dateFilter = dateFilter,
                        onDateFilterModeChange = onDateFilterModeChange,
                        onSingleDateChange = onSingleDateChange,
                        onRangeStartDateChange = onRangeStartDateChange,
                        onRangeEndDateChange = onRangeEndDateChange,
                        hourFilter = hourFilter,
                        onAllDayChange = onAllDayChange,
                        onStartHourChange = onStartHourChange,
                        onEndHourChange = onEndHourChange
                    )
                }
            }
        }
    }

    if (showMapInfo) {
        MapInfoDialog(onDismiss = { showMapInfo = false })
    }
}

@Composable
private fun MapInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Información del mapa") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Escala acústica orientativa",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to NoiseGreen,
                                    0.375f to NoiseYellow,
                                    0.625f to NoiseOrange,
                                    1.0f to NoiseRed
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("40 dB", style = MaterialTheme.typography.labelSmall)
                    Text("55 dB", style = MaterialTheme.typography.labelSmall)
                    Text("65 dB", style = MaterialTheme.typography.labelSmall)
                    Text("80+ dB", style = MaterialTheme.typography.labelSmall)
                }

                Text(
                    text = "Verde: nivel bajo · Amarillo: moderado · " +
                        "Naranja: alto · Rojo: muy alto",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Como referencia para zonas residenciales urbanizadas, " +
                        "la normativa española establece 55 dB durante la noche y " +
                        "65 dB durante el día y la tarde.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "El color se obtiene a partir del nivel estimado para cada zona. Repetir varias mediciones con el mismo nivel no hace que la zona pase automáticamente a rojo.",
                    style = MaterialTheme.typography.bodySmall
                )

                MapInfoLine(
                    title = "Periodo",
                    body = "Limita las mediciones a las últimas horas o días, una fecha concreta, un rango o todo el historial."
                )
                MapInfoLine(
                    title = "Horario",
                    body = "Muestra todo el día o únicamente las mediciones realizadas entre las horas seleccionadas."
                )
                MapInfoLine(
                    title = "Frecuencia",
                    body = "El nivel global representa el valor medio de la muestra. Al elegir una banda se muestra solo esa zona del espectro."
                )

                Text(
                    text = "Los objetivos normativos se evalúan durante periodos prolongados. " +
                        "Una medición breve no determina por sí sola su cumplimiento. " +
                        "La superficie entre puntos es una interpolación visual y las " +
                        "zonas alejadas de cualquier medición permanecen transparentes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun MapInfoLine(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun MapFilterControls(
    dateFilter: MapViewModel.DateFilter,
    onDateFilterModeChange: (MapViewModel.DateFilterMode) -> Unit,
    onSingleDateChange: (Long) -> Unit,
    onRangeStartDateChange: (Long) -> Unit,
    onRangeEndDateChange: (Long) -> Unit,
    hourFilter: MapViewModel.HourFilter,
    onAllDayChange: (Boolean) -> Unit,
    onStartHourChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val dateModeOptions = MapViewModel.DateFilterMode.entries.map { mode ->
        FilterOption(mode, mode.label)
    }
    val hourModeOptions = listOf(
        FilterOption(true, "Todo el día"),
        FilterOption(false, "Horario personalizado")
    )
    val startHourOptions = (0..23).map { FilterOption(it, it.toHourLabel()) }
    val endHourOptions = (1..24).map { FilterOption(it, it.toHourLabel()) }

    fun showDatePicker(initialDateMillis: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    clear()
                    set(year, month, dayOfMonth, 0, 0, 0)
                }.timeInMillis
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterDropdown(
                label = "Periodo",
                selectedText = dateFilter.label,
                options = dateModeOptions,
                onOptionSelected = onDateFilterModeChange,
                modifier = Modifier.weight(1f)
            )

            FilterDropdown(
                label = "Horario",
                selectedText = if (hourFilter.allDay) "Todo el día" else hourFilter.label,
                options = hourModeOptions,
                onOptionSelected = onAllDayChange,
                modifier = Modifier.weight(1f)
            )
        }

        when (dateFilter.mode) {
            MapViewModel.DateFilterMode.SINGLE_DAY -> {
                DateField(
                    label = "Fecha",
                    selectedText = dateFilter.singleDayMillis.formatDate(),
                    onClick = { showDatePicker(dateFilter.singleDayMillis, onSingleDateChange) }
                )
            }

            MapViewModel.DateFilterMode.DATE_RANGE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateField(
                        label = "Desde",
                        selectedText = dateFilter.rangeStartMillis.formatDate(),
                        onClick = { showDatePicker(dateFilter.rangeStartMillis, onRangeStartDateChange) },
                        modifier = Modifier.weight(1f)
                    )

                    DateField(
                        label = "Hasta",
                        selectedText = dateFilter.rangeEndMillis.formatDate(),
                        onClick = { showDatePicker(dateFilter.rangeEndMillis, onRangeEndDateChange) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            else -> Unit
        }

        if (!hourFilter.allDay) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Desde",
                    selectedText = hourFilter.startHour.toHourLabel(),
                    options = startHourOptions,
                    onOptionSelected = onStartHourChange,
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Hasta",
                    selectedText = hourFilter.endHour.toHourLabel(),
                    options = endHourOptions,
                    onOptionSelected = onEndHourChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    selectedText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                text = selectedText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun <T> FilterDropdown(
    label: String,
    selectedText: String,
    options: List<FilterOption<T>>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = selectedText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            expanded = false
                            onOptionSelected(option.value)
                        }
                    )
                }
            }
        }
    }
}

private data class FilterOption<T>(
    val value: T,
    val label: String
)

private fun frequencyLabel(selectedIndex: Int): String {
    return if (selectedIndex == -1) {
        "Nivel global"
    } else {
        "${ThirdOctaveBands.CENTER_FREQUENCIES_HZ[selectedIndex].toInt()} Hz"
    }
}

private fun Long.formatDate(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(this)
}

private fun Int.toHourLabel(): String = if (this == 24) "24:00" else "%02d:00".format(this)

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

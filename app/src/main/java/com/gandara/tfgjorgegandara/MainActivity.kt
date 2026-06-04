package com.gandara.tfgjorgegandara

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gandara.tfgjorgegandara.data.settings.AppSettings
import com.gandara.tfgjorgegandara.ui.analyzer.AnalyzerScreen
import com.gandara.tfgjorgegandara.ui.common.LocationViewModel
import com.gandara.tfgjorgegandara.ui.history.HistoryScreen
import com.gandara.tfgjorgegandara.ui.history.HistoryViewModel
import com.gandara.tfgjorgegandara.ui.map.MapScreen
import com.gandara.tfgjorgegandara.ui.settings.SettingsScreen
import com.gandara.tfgjorgegandara.ui.theme.NoiseMapTheme
import org.maplibre.android.MapLibre

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Analyzer : Screen("analyzer", "Analizador", Icons.Default.Home)
    object Map : Screen("map", "Mapa", Icons.Default.Place)
    object History : Screen("history", "Historial", Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa MapLibre y Configuración
        MapLibre.getInstance(this)
        AppSettings.init(this)

        setContent {
            NoiseMapTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                // Declaración de los permisos necesarios
                val permissions = arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

                var permissionsGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    )
                }

                // Lanzador de solicitud de permisos
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { result ->
                        val audioOk = result[Manifest.permission.RECORD_AUDIO] ?: false
                        val locationOk = (result[Manifest.permission.ACCESS_FINE_LOCATION] ?: false) ||
                            (result[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)
                        permissionsGranted = audioOk && locationOk
                    }
                )

                // Comprobamos si tenemos permisos ya o si todavía no tenemos. Si no, se solicitan
                LaunchedEffect(Unit) {
                    if (!permissionsGranted) {
                        permissionLauncher.launch(permissions)
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val audioOk = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            val locationOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                            permissionsGranted = audioOk && locationOk
                            if (!permissionsGranted) {
                                permissionLauncher.launch(permissions)
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val locationViewModel: LocationViewModel = viewModel()
                LaunchedEffect(permissionsGranted) {
                    if (permissionsGranted) {
                        locationViewModel.startLocationUpdates()
                    }
                }

                // Menú de navegación de abajo (Analizador/Mapa/Historial/Ajustes)
                Scaffold(
                    bottomBar = { BottomBar(navController) }
                ) { innerPadding ->
                    if (permissionsGranted) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Analyzer.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Analyzer.route) {
                                AnalyzerScreen(locationViewModel = locationViewModel)
                            }
                            composable(Screen.Map.route) {
                                MapScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    locationViewModel = locationViewModel
                                )
                            }
                            composable(Screen.History.route) {
                                val historyViewModel: HistoryViewModel = viewModel()
                                HistoryScreen(viewModel = historyViewModel)
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen()
                            }
                        }
                    } else {
                        PermissionWaitingScreen(
                            modifier = Modifier.padding(innerPadding),
                            onRequestPermissions = { permissionLauncher.launch(permissions) }
                        )
                    }
                }
            }
        }
    }
}

// Pantalla mientras se solicitan los permisos
@Composable
private fun PermissionWaitingScreen(
    modifier: Modifier = Modifier,
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permisos necesarios",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "La app necesita micrófono y ubicación para analizar y guardar muestras de ruido.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermissions) {
            Text("Conceder permisos")
        }
    }
}

@Composable
fun BottomBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Analyzer,
        Screen.Map,
        Screen.History,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 0.dp,
            shadowElevation = 10.dp
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    }
}

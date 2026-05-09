package com.gandara.tfgjorgegandara

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.osmdroid.config.Configuration

import com.gandara.tfgjorgegandara.ui.analyzer.AnalyzerScreen
import com.gandara.tfgjorgegandara.ui.map.MapScreen
import com.gandara.tfgjorgegandara.ui.history.HistoryScreen
import com.gandara.tfgjorgegandara.ui.history.HistoryViewModel
import com.gandara.tfgjorgegandara.ui.common.LocationViewModel
import com.gandara.tfgjorgegandara.ui.theme.NoiseMapTheme

/**
 * Enumeración de las pantallas principales de la aplicación para la navegación.
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Analyzer : Screen("analyzer", "Analizador", Icons.Default.Home)
    object Map : Screen("map", "Mapa", Icons.Default.Place)
    object History : Screen("history", "Historial", Icons.Default.List)
    object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
}

/**
 * Actividad principal que gestiona el punto de entrada, la navegación y los permisos globales.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración necesaria para el correcto funcionamiento de OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContent {
            NoiseMapTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                val permissions = arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

                // Estado reactivo para el control de permisos de Micro y Ubicación
                var permissionsGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                        (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                         ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { result ->
                        val audioOk = result[Manifest.permission.RECORD_AUDIO] ?: false
                        val locationOk = (result[Manifest.permission.ACCESS_FINE_LOCATION] ?: false) ||
                                       (result[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)
                        permissionsGranted = audioOk && locationOk
                    }
                )

                // Disparo automático de la solicitud de permisos al arrancar
                LaunchedEffect(Unit) {
                    if (!permissionsGranted) {
                        permissionLauncher.launch(permissions)
                    }
                }

                // Observador para actualizar el estado cuando el usuario vuelve desde Ajustes del Sistema
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

                Scaffold(
                    bottomBar = { BottomBar(navController) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Analyzer.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Analyzer.route) {
                            AnalyzerScreen(locationViewModel = locationViewModel)
                        }
                        composable(Screen.Map.route) { MapScreen(onNavigateBack = { navController.popBackStack() }) }
                        composable(Screen.History.route) {
                            val historyViewModel: HistoryViewModel = viewModel()
                            HistoryScreen(viewModel = historyViewModel)
                        }
                        composable(Screen.Settings.route) { 
                            Surface(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "Pantalla de Ajustes y Calibración",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Componente que representa la barra de navegación inferior sincronizada con el controlador de navegación.
 */
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

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
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

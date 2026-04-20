package com.gandara.tfgjorgegandara

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.gandara.tfgjorgegandara.ui.analyzer.AnalyzerScreen
import com.gandara.tfgjorgegandara.ui.map.MapScreen
import com.gandara.tfgjorgegandara.ui.theme.NoiseMapTheme

// Definimos las pantallas de la aplicación
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Analyzer : Screen("analyzer", "Analizador", Icons.Default.Home)
    object Map : Screen("map", "Mapa", Icons.Default.Place)
    object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoiseMapTheme {
                // Navegación
                val navController = rememberNavController()
                val context = LocalContext.current

                var hasMicPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        hasMicPermission = isGranted
                    }
                )

                LaunchedEffect(Unit) {
                    if(!hasMicPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                if(hasMicPermission) {
                    // Scaffold con BottomBar
                    Scaffold(
                        bottomBar = { BottomBar(navController) }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Analyzer.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Analyzer.route) { AnalyzerScreen() }
                            composable(Screen.Map.route) { MapScreen(onNavigateBack = { navController.popBackStack() }) }
                            composable(Screen.Settings.route) { Text("Pantalla de Ajustes y Calibración")} //SettingsScreen()
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Necesitamos el permiso para acceder a tu micrófono",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

// Barra de navegación inferior
@Composable
fun BottomBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Analyzer,
        Screen.Map,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        screens.forEach { pantalla ->
            NavigationBarItem(
                icon = { Icon(imageVector = pantalla.icon, contentDescription = pantalla.title) },
                label = { Text(pantalla.title) },
                selected = rutaActual == pantalla.route,
                onClick = {
                    navController.navigate(pantalla.route) {
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
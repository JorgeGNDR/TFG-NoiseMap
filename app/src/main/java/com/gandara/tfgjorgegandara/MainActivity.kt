package com.gandara.tfgjorgegandara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.gandara.tfgjorgegandara.ui.analyzer.AnalyzerScreen
import com.gandara.tfgjorgegandara.ui.map.MapScreen
import com.gandara.tfgjorgegandara.ui.theme.TFGJORGEGANDARATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TFGJORGEGANDARATheme {
                Surface (
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // El controlador que gestiona dónde estamos
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "analyzer") {

                        composable("analyzer") {
                            AnalyzerScreen(onNavigateToMap = { navController.navigate("map") })
                        }
                        composable("map") {
                            MapScreen(onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TFGJORGEGANDARATheme {
        Greeting("Android")
    }
}
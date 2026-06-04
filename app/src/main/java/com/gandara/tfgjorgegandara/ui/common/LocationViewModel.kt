package com.gandara.tfgjorgegandara.ui.common

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.data.location.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel compartido para la gestión centralizada de la ubicación geográfica del usuario.
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationHelper = LocationHelper(application)
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    /**
     * Inicializa el flujo de actualizaciones de ubicación y recupera la posición inicial.
     */
    fun startLocationUpdates() {
        Log.d("LocationViewModel", "Iniciando servicio de geoposicionamiento...")
        
        // Estrategia de recuperación rápida mediante caché
        locationHelper.getLastLocation { location ->
            location?.let { 
                if (_currentLocation.value == null) {
                    _currentLocation.value = it 
                }
            }
        }

        // Estrategia de recuperación forzada para mayor precisión inicial
        locationHelper.getCurrentLocation { location ->
            location?.let {
                _currentLocation.value = it
            }
        }

        // Suscripción al flujo continuo de actualizaciones
        viewModelScope.launch {
            locationHelper.getLocationUpdates(intervalInSeconds = 5).collect { location ->
                if (location != null) {
                    _currentLocation.value = location
                }
            }
        }
    }
}

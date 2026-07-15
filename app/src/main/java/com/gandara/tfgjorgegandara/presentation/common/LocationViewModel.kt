package com.gandara.tfgjorgegandara.presentation.common

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.di.AppContainer
import com.gandara.tfgjorgegandara.domain.location.GeoLocation
import com.gandara.tfgjorgegandara.domain.location.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewModel compartido para la gestión centralizada de la ubicación geográfica del usuario.
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationTracker: LocationTracker = AppContainer.locationTracker(application)
    
    private val _currentLocation = MutableStateFlow<GeoLocation?>(null)
    val currentLocation: StateFlow<GeoLocation?> = _currentLocation.asStateFlow()
    private var locationUpdatesJob: Job? = null

    /**
     * Inicializa el flujo de actualizaciones de ubicación y recupera la posición inicial.
     */
    fun startLocationUpdates() {
        if (locationUpdatesJob?.isActive == true) return
        Log.d("LocationViewModel", "Iniciando servicio de geoposicionamiento...")
        
        // Primera lectura a partir de la última ubicación disponible.
        locationTracker.getLastLocation { location ->
            location?.let { 
                if (_currentLocation.value == null) {
                    _currentLocation.value = it 
                }
            }
        }

        // Solicitud puntual para actualizar la posición inicial.
        locationTracker.getCurrentLocation { location ->
            location?.let {
                _currentLocation.value = it
            }
        }

        // Suscripción al flujo continuo de actualizaciones
        locationUpdatesJob = viewModelScope.launch {
            locationTracker.updates(intervalInSeconds = 5).collect { location ->
                if (location != null) {
                    _currentLocation.value = location
                }
            }
        }
    }
}

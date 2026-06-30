package com.gandara.tfgjorgegandara.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.gandara.tfgjorgegandara.domain.location.GeoLocation
import com.gandara.tfgjorgegandara.domain.location.LocationTracker
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit

/**
 * Clase de utilidad para interactuar con la API de Google Play Services Fused Location Provider.
 */
class LocationHelper(private val context: Context) : LocationTracker {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Define los parámetros de la solicitud de ubicación basándose en la precisión disponible.
     */
    private fun createLocationRequest(intervalInSeconds: Long = 5): LocationRequest {
        val priority = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        return LocationRequest.Builder(
            priority,
            TimeUnit.SECONDS.toMillis(intervalInSeconds)
        ).apply {
            setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(intervalInSeconds / 2))
        }.build()
    }

    /**
     * Expone un flujo de actualizaciones de ubicación utilizando callbackFlow.
     */
    @SuppressLint("MissingPermission")
    override fun updates(intervalInSeconds: Long): Flow<GeoLocation?> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                location?.let { trySend(GeoLocation(it.latitude, it.longitude)) }
            }
            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d("LocationHelper", "Disponibilidad del sensor GPS: ${availability.isLocationAvailable}")
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                createLocationRequest(intervalInSeconds),
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                Log.e("LocationHelper", "Fallo en la suscripción de actualizaciones: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error crítico en el servicio de ubicación: ${e.message}")
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Realiza una solicitud única para obtener la ubicación actual con alta prioridad.
     */
    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(onResult: (GeoLocation?) -> Unit) {
        val priority = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        fusedLocationClient.getCurrentLocation(
            CurrentLocationRequest.Builder()
                .setPriority(priority)
                .setDurationMillis(10000)
                .build(),
            null
        ).addOnSuccessListener { location ->
            onResult(location?.let { GeoLocation(it.latitude, it.longitude) })
        }.addOnFailureListener { e ->
            Log.e("LocationHelper", "No se pudo obtener la ubicación actual: ${e.message}")
            onResult(null)
        }
    }

    /**
     * Recupera de forma inmediata la última ubicación registrada por el sistema (caché).
     */
    @SuppressLint("MissingPermission")
    override fun getLastLocation(onResult: (GeoLocation?) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            onResult(location?.let { GeoLocation(it.latitude, it.longitude) })
        }
    }
}

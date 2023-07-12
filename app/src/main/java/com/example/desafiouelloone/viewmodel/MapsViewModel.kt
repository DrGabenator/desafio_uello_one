package com.example.desafiouelloone.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.desafiouelloone.data.models.MarkerEntity
import com.example.desafiouelloone.data.repository.MarkerRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class MapsViewModel(private val markerRepository: MarkerRepository) : ViewModel() {
    private val _markers = MutableLiveData<List<MarkerEntity>>()
    val markers: LiveData<List<MarkerEntity>> get() = _markers

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        fusedLocationClient: FusedLocationProviderClient,
        locationCallback: LocationCallback
    ) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    fun getAllMarkers() {
        viewModelScope.launch {
            try {
                val result = markerRepository.getAllMarkers()
                _markers.postValue(result)
            } catch (_: Exception) {
            }
        }
    }

    fun insertMarker(marker: MarkerEntity) {
        viewModelScope.launch {
            try {
                markerRepository.insertMarker(marker)
            } catch (_: Exception) {
            }
        }
    }

    fun deleteAllMarkers() {
        viewModelScope.launch {
            try {
                markerRepository.deleteAllMarkers()
            } catch (_: Exception) {
            }
        }
    }

    fun centerMapToUserLocation(map: GoogleMap, userLocationMarker: Marker?, defaultZoom: Float) {
        userLocationMarker?.position?.let { latLng ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, defaultZoom))
        }
    }

    fun calculateDistance(startLatLng: LatLng, endLatLng: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            startLatLng.latitude,
            startLatLng.longitude,
            endLatLng.latitude,
            endLatLng.longitude,
            results
        )
        return results[0]
    }

    fun convertMetersToKilometers(meters: Float): String? {
        val decimalFormat = DecimalFormat("0.#")
        val result = meters / 1000.0f
        return decimalFormat.format(result)
    }
}
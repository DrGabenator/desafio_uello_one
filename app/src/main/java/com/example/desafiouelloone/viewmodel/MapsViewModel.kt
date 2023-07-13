package com.example.desafiouelloone.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.desafiouelloone.data.db.MarkersDatabase
import com.example.desafiouelloone.data.models.MarkerEntity
import com.example.desafiouelloone.data.repository.MarkerRepository
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class MapsViewModel(application: Application) : AndroidViewModel(application) {
    private val _markers = MutableLiveData<List<MarkerEntity>>()
    val markers: LiveData<List<MarkerEntity>> get() = _markers

    private var markerRepository: MarkerRepository

    private var mutableMarkers: MutableList<Marker> = mutableListOf()

    private val defaultZoom = 15f

    init {
        val database = MarkersDatabase.getInstance(application)
        val markerDao = database.markerDao()
        markerRepository = MarkerRepository(markerDao)
    }

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

    fun centerMapToUserLocation(map: GoogleMap, userLocationMarker: Marker?) {
        userLocationMarker?.position?.let { latLng ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, defaultZoom))
        }
    }

    fun clearMarkers() {
        for (marker in mutableMarkers) {
            marker.remove()
        }
        mutableMarkers.clear()

        deleteAllMarkers()
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

    fun onMapLongClick(latLng: LatLng, userLocationMarker: Marker?, map: GoogleMap) {
        val latitude = latLng.latitude
        val longitude = latLng.longitude

        val userLocation = userLocationMarker?.position
        val distance = userLocation?.let { calculateDistance(latLng, it) }
        val distanceSnippet =
            distance?.let { "Distância: ${convertMetersToKilometers(it)} km" } ?: ""

        val markerOptions = MarkerOptions()
            .position(latLng)
            .title("Marcador")
            .snippet("Latitude: $latitude, Longitude: $longitude\n$distanceSnippet")
        val mapMarker = map.addMarker(markerOptions)
        if (mapMarker != null) {
            this.mutableMarkers.add(mapMarker)
            mapMarker.showInfoWindow()
        }

        val markerEntity = MarkerEntity(
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            distance = distance ?: 0f
        )
        insertMarker(markerEntity)
    }

    fun addSavedMarkersToMap(markers: List<MarkerEntity>, map: GoogleMap) {
        for (marker in markers) {
            val latLng = LatLng(marker.latitude, marker.longitude)
            val distanceSnippet =
                "Distância: ${convertMetersToKilometers(marker.distance)} km"
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title("Marcador")
                .snippet(
                    "Latitude: ${latLng.latitude}, Longitude: ${latLng.longitude}\n$distanceSnippet"
                )
            val mapMarker = map.addMarker(markerOptions)
            if (mapMarker != null) {
                this.mutableMarkers.add(mapMarker)
            }
        }
    }

    fun convertMetersToKilometers(meters: Float): String? {
        val decimalFormat = DecimalFormat("0.#")
        val result = meters / 1000.0f
        return decimalFormat.format(result)
    }
}
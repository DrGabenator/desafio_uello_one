package com.example.desafiouelloone.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.desafiouelloone.R
import com.example.desafiouelloone.data.db.MarkersDatabase
import com.example.desafiouelloone.data.models.MarkerEntity
import com.example.desafiouelloone.data.repository.MarkerRepository
import com.example.desafiouelloone.databinding.ActivityMapsBinding
import com.example.desafiouelloone.viewmodel.MapsViewModel
import com.example.desafiouelloone.viewmodel.MapsViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.text.DecimalFormat

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var viewModel: MapsViewModel

    private lateinit var binding: ActivityMapsBinding

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var markerRepository: MarkerRepository

    private var userLocationMarker: Marker? = null
    private var markers: MutableList<Marker> = mutableListOf()

    private val defaultZoom = 15f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val database = MarkersDatabase.getInstance(this)
        val markerDao = database.markerDao()
        markerRepository = MarkerRepository(markerDao)

        viewModel = ViewModelProvider(
            this,
            MapsViewModelFactory(markerRepository)
        )[MapsViewModel::class.java]
        viewModel.markers.observe(this, Observer { markers ->
            addSavedMarkersToMap(markers)
        })

        binding.deleteButton.setOnClickListener {
            clearMarkers()
        }

        binding.centerButton.setOnClickListener {
            viewModel.centerMapToUserLocation(map, userLocationMarker, defaultZoom)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let { location ->
                    showUserLocation(location)
                }
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setInfoWindowAdapter(MarkerInfoWindowAdapter())

        viewModel.getAllMarkers()

        map.setOnMapLongClickListener {
            onMapLongClick(it)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(
                    this,
                    "A permissão de localização foi negada.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun onMapLongClick(latLng: LatLng) {
        val latitude = latLng.latitude
        val longitude = latLng.longitude

        val userLocation = userLocationMarker?.position
        val distance = userLocation?.let { viewModel.calculateDistance(latLng, it) }
        val distanceSnippet =
            distance?.let { "Distância: ${viewModel.convertMetersToKilometers(it)} km" } ?: ""

        val markerOptions = MarkerOptions()
            .position(latLng)
            .title("Marcador")
            .snippet("Latitude: $latitude, Longitude: $longitude\n$distanceSnippet")
        val mapMarker = map.addMarker(markerOptions)
        if (mapMarker != null) {
            this.markers.add(mapMarker)
            mapMarker.showInfoWindow()
        }

        val markerEntity = MarkerEntity(
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            distance = distance ?: 0f
        )
        viewModel.insertMarker(markerEntity)
    }

    private fun addSavedMarkersToMap(markers: List<MarkerEntity>) {
        for (marker in markers) {
            val latLng = LatLng(marker.latitude, marker.longitude)
            val distanceSnippet = "Distância: ${viewModel.convertMetersToKilometers(marker.distance)} km"
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title("Marcador")
                .snippet(
                    "Latitude: ${latLng.latitude}, Longitude: ${latLng.longitude}\n$distanceSnippet"
                )
            val mapMarker = map.addMarker(markerOptions)
            if (mapMarker != null) {
                this.markers.add(mapMarker)
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            viewModel.startLocationUpdates(fusedLocationClient, locationCallback)
        }
    }

    private fun showUserLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)

        if (userLocationMarker == null) {
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title("Localização atual")
            userLocationMarker = map.addMarker(markerOptions)
        } else {
            userLocationMarker?.position = latLng
        }

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, defaultZoom))
    }

    private inner class MarkerInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        private val windowView: View =
            LayoutInflater.from(this@MapsActivity).inflate(R.layout.marker_info_window, null)
        private val decimalFormat = DecimalFormat("0.######")

        override fun getInfoWindow(marker: Marker): View? {
            return null
        }

        override fun getInfoContents(marker: Marker): View {
            val titleTextView = windowView.findViewById<TextView>(R.id.titleTextView)
            val snippetTextView = windowView.findViewById<TextView>(R.id.snippetTextView)

            titleTextView.text = marker.title

            val distance =
                userLocationMarker?.position?.let {
                    viewModel.calculateDistance(
                        it,
                        marker.position
                    )
                }

            val snippet =
                "Latitude: ${decimalFormat.format(marker.position.latitude)}, Longitude: ${
                    decimalFormat.format(marker.position.longitude)
                }\n Distância: ${
                    distance?.let {
                        viewModel.convertMetersToKilometers(
                            it
                        )
                    }
                } km"
            snippetTextView.text = snippet

            return windowView
        }
    }


    private fun clearMarkers() {
        for (marker in markers) {
            marker.remove()
        }
        markers.clear()

        viewModel.deleteAllMarkers()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }
}
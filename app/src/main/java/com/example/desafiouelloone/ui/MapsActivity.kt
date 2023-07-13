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
import androidx.lifecycle.ViewModelProvider
import com.example.desafiouelloone.R
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

    private var userLocationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel = ViewModelProvider(
            this,
            MapsViewModelFactory(application)
        )[MapsViewModel::class.java]
        viewModel.markers.observe(this) { markers ->
            viewModel.addSavedMarkersToMap(markers, map)
        }

        binding.deleteButton.setOnClickListener {
            viewModel.clearMarkers()
        }

        binding.centerButton.setOnClickListener {
            viewModel.centerMapToUserLocation(map, userLocationMarker)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                showUserLocation(locationResult.lastLocation)
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setInfoWindowAdapter(MarkerInfoWindowAdapter())

        viewModel.getAllMarkers()

        map.setOnMapLongClickListener {
            viewModel.onMapLongClick(it, userLocationMarker, map)
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

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }
}
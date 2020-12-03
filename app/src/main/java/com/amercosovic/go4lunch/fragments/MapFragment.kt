package com.amercosovic.go4lunch.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.amercosovic.go4lunch.NearbyPlacesState
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.retrofit.ApiClient
import com.amercosovic.go4lunch.viewmodels.MapFragmentViewModel
import com.amercosovic.mapfragmentwithmvvmldemo.utility.Constants
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapFragment : Fragment() , OnMapReadyCallback{

    private var viewModel = MapFragmentViewModel()
    private lateinit var map: GoogleMap
    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private val INTERVAL: Long = 10000
    private val FASTEST_INTERVAL: Long = 5000
    private lateinit var locationRequest: LocationRequest
    private val LOCATION_PERMISSION_REQUEST = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {        return inflater.inflate(R.layout.fragment_map, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationRequest = LocationRequest()
        val navDrawer: DrawerLayout = requireActivity().mainActivityDrawerLayout
        navDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        viewModel = ViewModelProvider(this).get(MapFragmentViewModel::class.java)
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationDisabledMessage()
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap != null) {
            map = googleMap
        }
            lifecycleScope.launch(IO) {
                getLocationAccess()
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProvider?.lastLocation
            ?.addOnSuccessListener { location : Location? ->
                if (location != null) {
                    viewModel.makeApiCall(location)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attachObservers()
    }



    suspend fun getLocationAccess() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            lifecycleScope.launch(Main) {
                map.isMyLocationEnabled = true
                withContext(IO) {
                    fusedLocationProvider?.lastLocation
                        ?.addOnSuccessListener { location : Location? ->
                            val position = location?.let { LatLng(it.latitude,location.longitude) }
                            map.addMarker(
                                position?.let {
                                    MarkerOptions().position(it).title("My Location")
                                        .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_RED)
                                        )
                                }
                            )
                            if (location != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude,location.longitude),15f))
                            }
                        }
                    startLocationUpdates()
                }
            }
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            withContext(Main) {
                map.isMyLocationEnabled = false
                val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                val mapContainer = mapFragment?.map
                mapContainer?.view?.visibility = View.GONE
                deniedPermissionMessage.visibility = View.VISIBLE
            }
        }
    }
   private fun locationDisabledMessage() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Your location is disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    , 11)
            }
        val alert: AlertDialog  = builder.create()
        alert.show()
            }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        var vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable!!.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        var bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        var canvas =  Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
   private fun startLocationUpdates() {
        // Create the location request to start receiving updates
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.setInterval(INTERVAL)
        locationRequest.setFastestInterval(FASTEST_INTERVAL)

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(requireContext())
        settingsClient.checkLocationSettings(locationSettingsRequest)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationProvider?.requestLocationUpdates(locationRequest, locationCallback,
            Looper.getMainLooper())
    }

    fun onLocationChanged(location: Location) {
        // New location has now been determined
            val position = LatLng(location.latitude,location.longitude)
            map.addMarker(
                MarkerOptions().position(position).title("My Location")
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude,location.longitude),15f))
        lifecycleScope.launch(IO) {
            val apiCall = ApiClient.getClient.getNearbyPlaces(
                location = "${location.latitude},${location.longitude}",
                key = Constants.GOOGLE_API_KEY,
                radius = Constants.RADIUS_1000,
                types = Constants.TYPE_RESTAURANT
            )
            val results = apiCall.results
            if (!results.isNullOrEmpty()) {
                stopLocationUpdates()
                withContext(Main) {
                    map.addMarker(
                        MarkerOptions().position(position).title("My Location")
                            .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_RED
                                )
                            )
                    )
                    val builder = LatLngBounds.Builder()
                    for (result in results) {
                        val newLatLng = LatLng(
                            result.geometry.location.lat,
                            result.geometry.location.lng
                        )

                        val markers = map.addMarker(
                            MarkerOptions().position(newLatLng)
                                .title(result.name).icon(bitmapDescriptorFromVector(requireContext(),R.drawable.mapmarker))
                        )
                        builder.include(markers.position)
                        val bounds = builder.build()
                        val width = resources.displayMetrics.widthPixels
                        val height = resources.displayMetrics.heightPixels
                        val padding = (width * 0.10).toInt() // offset from edges of the map 10% of screen

                        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(
                            bounds,
                            width,
                            height,
                            padding
                        )
                        map.animateCamera(cameraUpdate)
                    }
                }
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // do work here
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProvider!!.removeLocationUpdates(locationCallback)
    }

    private fun attachObservers() {
        viewModel.state.observe(this, Observer {state ->
            when(state) {
                is NearbyPlacesState.Loading -> {
                // To Do
                }
                is NearbyPlacesState.Success -> {
                    startLocationUpdates()
                }
                is Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val context = activity as AppCompatActivity
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    map.isMyLocationEnabled = false
                    val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                    val mapContainer = mapFragment?.map
                    mapContainer?.view?.visibility = View.GONE
                    deniedPermissionMessage.visibility = View.VISIBLE
                }
                startLocationUpdates()
                map.isMyLocationEnabled = true
            }
        }
    }
}
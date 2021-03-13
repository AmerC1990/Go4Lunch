package com.amercosovic.go4lunch.fragments

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.amercosovic.go4lunch.NearbyPlacesState
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.activities.RestaurantDetailsActivity
import com.amercosovic.go4lunch.utility.Translate.translate
import com.amercosovic.go4lunch.viewmodels.RestaurantsViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapFragment : BaseFragment(), OnMapReadyCallback {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var viewModel = RestaurantsViewModel(Application())
    private lateinit var map: GoogleMap
    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private val INTERVAL: Long = 10000
    private val FASTEST_INTERVAL: Long = 50000
    private lateinit var locationRequest: LocationRequest
    private val LOCATION_PERMISSION_REQUEST = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // set up toolbar
        val toolbarTitle = activity?.toolbarTitle
        toolbarTitle?.text = translate(english = "       I'm Hungry", spanish = "     Tengo hambre")
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationRequest = LocationRequest()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // initialize
        viewModel = ViewModelProvider(this).get(RestaurantsViewModel::class.java)
        // check is turned on user's device
        isLocationOn()
    }

    // prepare map and get location
    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap != null) {
            map = googleMap
        }
        lifecycleScope.launch(IO) {
            getLocationAccess()
        }
        // implement filter/search funcionality with search view
        val searchView = activity?.searchView
        searchView?.visibility = View.VISIBLE
        searchView?.queryHint =
            translate(english = "Search restaurants", spanish = "Buscar restaurantes")
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (viewModel.nearbyPlacesState.value is NearbyPlacesState.Success) {
                    val filteredData =
                        (viewModel.nearbyPlacesState.value as NearbyPlacesState.Success).nearbyPlacesResponse.restaurants.filter {
                            it.name.contains(
                                searchView.query,
                                ignoreCase = true
                            )
                        }
                    val builder = LatLngBounds.Builder()
                    for (item in filteredData) {
                        val newLatLng = item.geometry.location.lat.let {
                            item.geometry.location.lng.let { it1 ->
                                LatLng(
                                    it,
                                    it1
                                )
                            }
                        }
                        val markers = map.addMarker(
                            newLatLng.let {
                                MarkerOptions().position(it)
                                    .title(item.name).icon(
                                        bitmapDescriptorFromVector(
                                            requireContext(),
                                            R.drawable.customredmarker
                                        )
                                    )
                            }
                        )
                        val markerList = listOf(markers)
                        val reference = db.collection("users")
                        maintainMarkerColor(reference, markerList)

                        builder.include(markers.position)
                        val bounds = builder.build()
                        val width = resources.displayMetrics.widthPixels
                        val height = resources.displayMetrics.heightPixels
                        val padding = (width * 0.10).toInt()
                        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(
                            bounds,
                            width,
                            height,
                            padding
                        )
                        map.animateCamera(cameraUpdate)
                        geolocalizationButton.setOnClickListener {
                            map.animateCamera(cameraUpdate)
                        }
                    }
                } else {

                }
                return false
            }
        })
    }

    // attach observers on activity created
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        attachObservers()
    }

    // get user's location
    private suspend fun getLocationAccess() {
        val getLocation = checkPermissionAndGetLocation()
        getLocation?.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val position = LatLng(location.latitude, location.longitude)
                map.addMarker(
                    position.let {
                        MarkerOptions().position(it)
                            .title(translate(english = "My Location", spanish = "Mi ubicacion"))
                            .icon(
                                this.context?.let { it1 ->
                                    bitmapDescriptorFromVector(
                                        it1,
                                        R.drawable.custombluehomemarker
                                    )
                                }
                            )
                    }
                )
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            location.latitude,
                            location.longitude
                        ), 15f
                    )
                )
                viewModel.fetchNearbyPlacesData(
                    location.latitude.toString(),
                    location.longitude.toString()
                )
            }
            startLocationUpdates()
        }

        if (!checkPermission()) {
            withContext(Main) {
                map.isMyLocationEnabled = false
                val mapFragment =
                    childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                val mapContainer = mapFragment?.map
                mapContainer?.view?.visibility = View.GONE
                deniedPermissionMessage.visibility = View.VISIBLE
            }
        }
    }

    // create bitmap from drawable
    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable?.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = vectorDrawable?.intrinsicWidth?.let {
            Bitmap.createBitmap(
                it,
                vectorDrawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val canvas = bitmap?.let { Canvas(it) }
        if (canvas != null) {
            vectorDrawable.draw(canvas)
        }
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // start location updates
    private fun startLocationUpdates() {
        // Create the location request to start receiving updates
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(requireContext())
        settingsClient.checkLocationSettings(locationSettingsRequest)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProvider?.requestLocationUpdates(
            locationRequest, locationCallback,
            Looper.getMainLooper()
        )
    }

    // update camera and marker when location changes
    private fun onLocationChanged(location: Location) {
        val position = LatLng(location.latitude, location.longitude)
        map.addMarker(
            MarkerOptions().position(position)
                .title(translate(english = "My Location", spanish = "Mi ubicacion")).icon(
                    this.context?.let {
                        bitmapDescriptorFromVector(
                            it,
                            R.drawable.custombluehomemarker
                        )
                    }
                )
        )
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    location.latitude,
                    location.longitude
                ), 15f
            )
        )
        viewModel.fetchNearbyPlacesData(location.latitude.toString(), location.longitude.toString())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    // stop location updates
    private fun stopLocationUpdates() {
        fusedLocationProvider?.removeLocationUpdates(locationCallback)
    }

    // attach observers to draw markers with data
    private fun attachObservers() {
        viewModel.nearbyPlacesState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is NearbyPlacesState.Loading -> {
                    mapFraggmentProgressBar.visibility = View.VISIBLE
                }

                is NearbyPlacesState.Success -> {
                    mapFraggmentProgressBar.visibility = View.GONE
                    stopLocationUpdates()
                    val restaurantData = state.nearbyPlacesResponse.restaurants
                    val builder = LatLngBounds.Builder()
                    for (item in restaurantData) {
                        val newLatLng = item.geometry.location.lat.let {
                            item.geometry.location.lng.let { it1 ->
                                LatLng(
                                    it,
                                    it1
                                )
                            }
                        }
                        val markers = map.addMarker(
                            newLatLng.let {
                                MarkerOptions().position(it)
                                    .title(item.name).icon(
                                        this.context?.let { it1 ->
                                            bitmapDescriptorFromVector(
                                                it1,
                                                R.drawable.customredmarker
                                            )
                                        }
                                    )
                            }
                        )
                        val markerList = listOf(markers)
                        val reference = db.collection("users")
                        maintainMarkerColor(reference, markerList)

                        map.setOnMarkerClickListener { it ->
                            val markerName = it.title
                            if (markerName.toString() == translate(
                                    english = "My Location",
                                    spanish = "Mi ubicacion"
                                )
                            ) {
                                it.showInfoWindow()
                            } else {
                                val intent =
                                    Intent(this.context, RestaurantDetailsActivity::class.java)
                                intent.putExtra(
                                    "restaurantDataFromMap",
                                    restaurantData.find { it.name == markerName }
                                )
                                startActivity(intent)
                            }

                            true
                        }

                        builder.include(markers.position)
                        val bounds = builder.build()
                        val width = resources.displayMetrics.widthPixels
                        val height = resources.displayMetrics.heightPixels
                        val padding = (width * 0.10).toInt()
                        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(
                            bounds,
                            width,
                            height,
                            padding
                        )
                        map.animateCamera(cameraUpdate)
                        geolocalizationButton.setOnClickListener {
                            map.animateCamera(cameraUpdate)
                        }

                    }
                }
                is Error -> {
                    mapFraggmentProgressBar.visibility = View.GONE
                    Toast.makeText(this.context, state.message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    // check permission
    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    // make map invisible and prompt user to give permission in order to use their location
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val context = activity as AppCompatActivity
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    map.isMyLocationEnabled = false
                    val mapFragment =
                        childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                    val mapContainer = mapFragment?.map
                    mapContainer?.view?.visibility = View.GONE
                    deniedPermissionMessage.visibility = View.VISIBLE
                }
                map.isMyLocationEnabled = true
            }
        }
    }

    // maintain marker color if user/coworkers have/have not visited restaurant
    private fun maintainMarkerColor(reference: CollectionReference, markerList: List<Marker>) {
        reference.addSnapshotListener { value, error ->
            for (marker in markerList) {
                reference.whereEqualTo("userRestaurant", marker.title.toString()).get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            if (document.data.toString().contains(marker.title.toString())) {
                                marker.setIcon(this.context?.let {
                                    bitmapDescriptorFromVector(
                                        it,
                                        R.drawable.customgreenmarker
                                    )
                                })
                            } else if (!document.data.toString()
                                    .contains(marker.title.toString())
                            ) {
                                marker.setIcon(this.context?.let {
                                    bitmapDescriptorFromVector(
                                        it,
                                        R.drawable.customredmarker
                                    )
                                })
                            }
                        }
                    }
            }
        }
    }

}
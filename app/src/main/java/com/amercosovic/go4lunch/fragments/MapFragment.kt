package com.amercosovic.go4lunch.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.RetrofitClient
import com.amercosovic.mapfragmentwithmvvmldemo.utility.Constants
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FacebookAuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapFragment : Fragment() , OnMapReadyCallback{

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
//        val sydney = LatLng(-34.0, 151.0)
//        googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))


    }
    private lateinit var map: GoogleMap
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private val INTERVAL: Long = 2000
    private val FASTEST_INTERVAL: Long = 1000
    private lateinit var mLocationRequest: LocationRequest
    private val LOCATION_PERMISSION_REQUEST = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutInflater = inflater.inflate(R.layout.fragment_map, container, false)
        mLocationRequest = LocationRequest()
        val searchView: SearchView = layoutInflater.findViewById(R.id.map_fragment_search_icon)
        searchView.setOnQueryTextFocusChangeListener { _ , hasFocus ->
            if (hasFocus) {
                // searchView expanded
                map_fragment_title.visibility = View.GONE
            } else {
                // searchView not expanded
                map_fragment_title.visibility = View.VISIBLE
            }
        }
        val navDrawer: DrawerLayout = activity!!.drawer_layout
        navDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        val bottomNavBar: BottomNavigationView = activity!!.bottom_navigation
        bottomNavBar.visibility = View.VISIBLE
        val flWrapper: FrameLayout = activity!!.fl_wrapper
        flWrapper.layoutParams = LinearLayout.LayoutParams(720,1225)
        val actionBar: android.app.ActionBar? = activity!!.actionBar
        actionBar?.hide()
        activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        val mapFragment = childFragmentManager.fragments[0] as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        val locationManager = (activity as AppCompatActivity).getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }
        return layoutInflater
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap!!
        CoroutineScope(IO).launch {
            getLocationAccess()
        }
        val context = activity as AppCompatActivity
        MapsInitializer.initialize(context)

    }

    private suspend fun getLocationAccess() {
        val context = activity as AppCompatActivity
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            CoroutineScope(Main).launch {
//                map.isMyLocationEnabled = true
                withContext(IO) {
                    startLocationUpdates()
                }
            }
        }
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                withContext(Main) {
                    map.isMyLocationEnabled = false
                    val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                    val mapContainer = mapFragment?.map
                    mapContainer?.view?.visibility = View.GONE
                    denied_permission_message.visibility = View.VISIBLE
                }

            }
        }


     private fun startLocationUpdates() {
        // Create the location request to start receiving updates
         val context = activity as AppCompatActivity
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest!!.setInterval(INTERVAL)
        mLocationRequest!!.setFastestInterval(FASTEST_INTERVAL)

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(context)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback,
            Looper.getMainLooper())
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // do work here
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    fun onLocationChanged(location: Location) {
        // New location has now been determined
        lifecycleScope.launch(IO) {
            val position = LatLng(location.latitude,location.longitude)
            val apiCall = RetrofitClient.googleMethods().getNearbySearch(
                    location = "${location.latitude},${location.longitude}",
                    key = Constants.GOOGLE_API_KEY,
                    radius = Constants.RADIUS_1000,
                    types = Constants.TYPE_RESTAURANT
                    )
                    val results = apiCall?.results
                        if (!results.isNullOrEmpty()) {
                            stoplocationUpdates()
                            withContext(Main) {
                                val context = activity as AppCompatActivity
                                val zoomLevel = 16f
                                map.addMarker(
                                    MarkerOptions().position(position).title("My Location")
                                        .icon(
                                            BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_RED
                                            )
                                        )
                                )
                                val builder = LatLngBounds.Builder()
                                for (result in results!!) {
                                    val newLatLng = LatLng(
                                        result.geometry.location?.lat,
                                        result.geometry.location?.lng
                                    )

                                    val markers = map.addMarker(
                                        MarkerOptions().position(newLatLng)
                                            .title(result.name).icon(bitmapDescriptorFromVector(context, R.drawable.restauranticon))
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
                                    map.animateCamera(cameraUpdate);
//                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraUpdate,zoomLevel))
//                                    map.moveCamera(cameraUpdate)
//                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(markers.position,zoomLevel))
                                }
                            }
                        }
                        else {
                            withContext(Main) {
                                Toast.makeText(
                                    context,
                                    "Unable to get nearby places",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
        }
    }
     private fun stoplocationUpdates() {
        mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
    }

    private fun buildAlertMessageNoGps() {
        val context = activity as AppCompatActivity
        val builder = AlertDialog.Builder(context)
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
        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val context = activity as AppCompatActivity
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                startLocationUpdates()
                map.isMyLocationEnabled = true

            }
        }
    }
}
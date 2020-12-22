package com.amercosovic.go4lunch.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.amercosovic.go4lunch.NearbyPlacesState
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.adapters.RestaurantListAdapter
import com.amercosovic.go4lunch.viewmodels.MapFragmentViewModel
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.fragment_restaurantlist.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch


class RestaurantListFragment : Fragment() {

    lateinit var recyclerViewAdapter: RestaurantListAdapter
    private var viewModel = MapFragmentViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_restaurantlist, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        attachObservers()
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationDisabledMessage()
        } else {
            lifecycleScope.launch(IO) {
                getLocationAccess()
            }
        }
    }

    private fun getLocationAccess() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.create()
            locationRequest.interval = 60000
            locationRequest.fastestInterval = 5000
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation
                ?.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        viewModel.makeApiCall(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )
                        lifecycleScope.launch(Main) {
                            initRecyclerView(
                                location.latitude.toString(),
                                location.longitude.toString()
                            )
                        }

                    }
                }
        }
    }

    private fun initRecyclerView(latitude: String, longitude: String) {
        restaurantListRecyclerView.apply {
            recyclerViewAdapter = RestaurantListAdapter(latitude, longitude)
            adapter = recyclerViewAdapter
            val decoration =
                DividerItemDecoration(requireContext(), StaggeredGridLayoutManager.VERTICAL)
            addItemDecoration(decoration)
        }
    }

    private fun attachObservers() {
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is NearbyPlacesState.Loading -> {
                    restaurantListProgressBar.visibility = View.VISIBLE
                }
                is NearbyPlacesState.Success -> {
                    restaurantListProgressBar.visibility = View.GONE
                    recyclerViewAdapter.setListData(state.nearbyPlacesResponse.results)
                    recyclerViewAdapter.notifyDataSetChanged()
                }
                is Error -> {
                    restaurantListProgressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun locationDisabledMessage() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Your location is disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 11
                )
            }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

}
package com.amercosovic.go4lunch.fragments

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.amercosovic.go4lunch.NearbyPlacesState
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.adapters.RestaurantListAdapter
import com.amercosovic.go4lunch.viewmodels.MapFragmentViewModel
import kotlinx.android.synthetic.main.fragment_restaurantlist.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch


class RestaurantListFragment : BaseFragment() {

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
        isLocationOn()
        lifecycleScope.launch(IO) {
            getLocationAccess()
        }
        attachObservers()
    }

    private fun getLocationAccess() {
        val getLocation = checkPermissionAndGetLocation()
        getLocation?.addOnSuccessListener { location: Location? ->
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
                    recyclerViewAdapter.setListData(state.nearbyPlacesResponse.restaurants)
                    recyclerViewAdapter.notifyDataSetChanged()
                }
                is Error -> {
                    restaurantListProgressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }


}
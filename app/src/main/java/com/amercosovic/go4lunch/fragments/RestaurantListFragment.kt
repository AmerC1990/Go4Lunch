package com.amercosovic.go4lunch.fragments

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.amercosovic.go4lunch.NearbyPlacesState
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.adapters.RestaurantListAdapter
import com.amercosovic.go4lunch.viewmodels.MapFragmentViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_restaurantlist.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch


class RestaurantListFragment : BaseFragment() {

    private var viewModel = MapFragmentViewModel()
    lateinit var recyclerViewAdapter: RestaurantListAdapter

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
        val searchView = activity?.searchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (viewModel.state.value is NearbyPlacesState.Success) {
                    val filteredData =
                        (viewModel.state.value as NearbyPlacesState.Success).nearbyPlacesResponse.restaurants.filter {
                            it.name.contains(
                                searchView.query,
                                ignoreCase = true
                            )
                        }
                    recyclerViewAdapter.setListData(filteredData)
                    recyclerViewAdapter.notifyDataSetChanged()
                    when (searchView.query.toString()) {
                        "" -> {
                            recyclerViewAdapter.setListData((viewModel.state.value as NearbyPlacesState.Success).nearbyPlacesResponse.restaurants)
                            recyclerViewAdapter.notifyDataSetChanged()
                        }
                    }
                } else {
//                    Toast.makeText(requireContext(), "Failed to update List", Toast.LENGTH_LONG).show()
                }
                return false
            }
        })

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



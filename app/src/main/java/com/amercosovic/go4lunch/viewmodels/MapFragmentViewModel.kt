package com.amercosovic.go4lunch.viewmodels

import NearbySearch
import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amercosovic.go4lunch.NearbyPlacesState
import com.amercosovic.go4lunch.retrofit.ApiClient
import com.amercosovic.mapfragmentwithmvvmldemo.utility.Constants
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class MapFragmentViewModel: ViewModel() {

    val state: MutableLiveData<NearbyPlacesState> = MutableLiveData()

     fun makeApiCall(latitude: String, longitude: String) {
         state.value = NearbyPlacesState.Loading
         viewModelScope.launch(IO) {
             val response = ApiClient.getClient.getNearbyPlaces(
                 location = "${latitude},${longitude}",
                 key = Constants.GOOGLE_API_KEY,
                 radius = Constants.RADIUS_1000,
                 types = Constants.TYPE_RESTAURANT
             )
             if (!response.results.isEmpty()) {
                 state.postValue(NearbyPlacesState.Success(response))
            }
            else {
                state.postValue(NearbyPlacesState.Error("Error getting data"))
            }
        }

    }
}
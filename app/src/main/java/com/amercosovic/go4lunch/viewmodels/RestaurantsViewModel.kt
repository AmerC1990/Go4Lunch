package com.amercosovic.go4lunch.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amercosovic.go4lunch.NearbyPlacesState
import com.amercosovic.go4lunch.PlaceDetailsState
import com.amercosovic.go4lunch.retrofit.ApiClient
import com.amercosovic.mapfragmentwithmvvmldemo.utility.Constants
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class RestaurantsViewModel : ViewModel() {

    val state: MutableLiveData<NearbyPlacesState> = MutableLiveData()
    val state2: MutableLiveData<PlaceDetailsState> = MutableLiveData()

    fun fetchNearbyPlacesData(latitude: String, longitude: String) {
        state.value = NearbyPlacesState.Loading
        viewModelScope.launch(IO) {
            val response = ApiClient.getClient.getNearbyPlaces(
                location = "${latitude},${longitude}",
                key = Constants.GOOGLE_API_KEY,
                radius = Constants.RADIUS_1000,
                types = Constants.TYPE_RESTAURANT
            )
            if (!response.restaurants.isEmpty()) {
                state.postValue(NearbyPlacesState.Success(response))
            } else {
                state.postValue(NearbyPlacesState.Error("Error getting data"))
            }
        }
    }

    fun fetchWebsiteAndPhoneNumberData(data: String) {
        state2.value = PlaceDetailsState.Loading
        viewModelScope.launch(IO) {
            val response = ApiClient.getClient.getPlaceDetails(
                place_id = data.toString().substringAfter("placeId=").substringBefore(","),
                fields = "name,website,formatted_phone_number",
                key = Constants.GOOGLE_API_KEY
            )
            if (!response.result.toString().isEmpty()) {
                state2.postValue(PlaceDetailsState.Success(response))
            } else {
                state2.postValue(PlaceDetailsState.Error("Error getting data"))
            }
        }
    }

}
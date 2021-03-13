package com.amercosovic.go4lunch.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amercosovic.go4lunch.NearbyPlacesState
import com.amercosovic.go4lunch.PlaceDetailsState
import com.amercosovic.go4lunch.retrofit.ApiClient
import com.amercosovic.go4lunch.utility.Constants
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class RestaurantsViewModel(application: Application) : AndroidViewModel(application) {

    // initialize mutable live data objects
    val nearbyPlacesState: MutableLiveData<NearbyPlacesState> = MutableLiveData()
    val placeDetailsState: MutableLiveData<PlaceDetailsState> = MutableLiveData()

    // get data for nearby restaurants
    fun fetchNearbyPlacesData(latitude: String, longitude: String) {
        nearbyPlacesState.value = NearbyPlacesState.Loading
        viewModelScope.launch(IO) {
            val response = ApiClient.getClient.getNearbyPlaces(
                location = "${latitude},${longitude}",
                key = Constants.GOOGLE_API_KEY,
                radius = Constants.RADIUS_1000,
                types = Constants.TYPE_RESTAURANT
            )
            if (response.restaurants.isNotEmpty()) {
                nearbyPlacesState.postValue(NearbyPlacesState.Success(response))
            } else {
                nearbyPlacesState.postValue(NearbyPlacesState.Error("Error getting data"))
            }
        }
    }

    // get data for phone number and website url of a particular restaurant
    fun fetchWebsiteAndPhoneNumberData(data: String) {
        placeDetailsState.value = PlaceDetailsState.Loading
        viewModelScope.launch(IO) {
            val response = ApiClient.getClient.getPlaceDetails(
                place_id = data.toString().substringAfter("placeId=").substringBefore(","),
                fields = "name,website,formatted_phone_number",
                key = Constants.GOOGLE_API_KEY
            )
            if (response.result != null) {
                placeDetailsState.postValue(PlaceDetailsState.Success(response))
            } else {
                placeDetailsState.postValue(PlaceDetailsState.Error("Error getting data"))
            }
        }
    }
}

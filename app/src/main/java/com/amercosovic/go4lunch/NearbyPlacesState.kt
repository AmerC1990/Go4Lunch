package com.amercosovic.go4lunch

import com.amercosovic.go4lunch.model.NearbySearch
import com.amercosovic.go4lunch.model.PlaceDetails


sealed class NearbyPlacesState {
    data class Error(val errorMessage: String) : NearbyPlacesState()
    data class Success(val nearbyPlacesResponse: NearbySearch) : NearbyPlacesState()
    object Loading : NearbyPlacesState()
}

sealed class PlaceDetailsState {
    data class Error(val errorMessage: String) : PlaceDetailsState()
    data class Success(val placeDetailsResponse: PlaceDetails) : PlaceDetailsState()
    object Loading : PlaceDetailsState()
}
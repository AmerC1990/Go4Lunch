package com.amercosovic.go4lunch

import com.amercosovic.go4lunch.model.NearbySearch


sealed class NearbyPlacesState {
    data class Error(val errorMessage: String) : NearbyPlacesState()
    data class Success(val nearbyPlacesResponse: NearbySearch) : NearbyPlacesState()
    object Loading : NearbyPlacesState()
}
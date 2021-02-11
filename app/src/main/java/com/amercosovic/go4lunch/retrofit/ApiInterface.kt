package com.amercosovic.go4lunch.retrofit

import com.amercosovic.go4lunch.model.NearbySearch
import com.amercosovic.go4lunch.model.PlaceDetails
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    // functions with querys to make api call
    @GET("place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: String,
        @Query("type") types: String,
        @Query("key") key: String
    ): NearbySearch

    @GET("place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") place_id: String,
        @Query("fields") fields: String,
        @Query("key") key: String
    ): PlaceDetails
}

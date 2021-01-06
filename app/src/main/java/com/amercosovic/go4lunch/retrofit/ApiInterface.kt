package com.amercosovic.go4lunch.retrofit

import com.amercosovic.go4lunch.model.NearbySearch
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    @GET("place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: String,
        @Query("type") types: String,
        @Query("key") key: String
    ): NearbySearch
}
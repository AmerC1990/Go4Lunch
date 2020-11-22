package com.amercosovic.go4lunch

import NearbySearch
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleMethods {
    @GET("place/nearbysearch/json")
    suspend fun getNearbySearch(
        @Query("location") location: String,
        @Query("radius") radius: String,
        @Query("type") types: String,
        @Query("key") key: String
    ): NearbySearch
}
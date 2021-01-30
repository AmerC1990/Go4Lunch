package com.amercosovic.go4lunch.model


import com.google.gson.annotations.SerializedName

data class PlaceDetails(
    @SerializedName("html_attributions")
    val htmlAttributions: List<Any>,
    @SerializedName("result")
    val result: Result,
    @SerializedName("status")
    val status: String
)
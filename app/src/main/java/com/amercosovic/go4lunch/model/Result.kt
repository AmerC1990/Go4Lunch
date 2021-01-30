package com.amercosovic.go4lunch.model


import com.google.gson.annotations.SerializedName

data class Result(
    @SerializedName("formatted_phone_number")
    val formattedPhoneNumber: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("website")
    val website: String
)
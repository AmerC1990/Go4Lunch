package com.amercosovic.go4lunch.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NearbySearch(
    @SerializedName("results")
    val restaurants: List<Restaurant>
) : Parcelable
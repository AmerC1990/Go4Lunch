package com.amercosovic.go4lunch.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Restaurant(
    @SerializedName("business_status")
    val businessStatus: String,
    @SerializedName("geometry")
    val geometry: @RawValue Geometry,
    @SerializedName("icon")
    val icon: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("opening_hours")
    val openingHours: @RawValue OpeningHours,
    @SerializedName("photos")
    val photos: @RawValue List<Photo>,
    @SerializedName("place_id")
    val placeId: String,
    @SerializedName("plus_code")
    val plusCode: @RawValue PlusCode,
    @SerializedName("price_level")
    val priceLevel: Int,
    @SerializedName("rating")
    val rating: Double,
    @SerializedName("reference")
    val reference: String,
    @SerializedName("scope")
    val scope: String,
    @SerializedName("types")
    val types: List<String>,
    @SerializedName("user_ratings_total")
    val userRatingsTotal: Int,
    @SerializedName("vicinity")
    val vicinity: String
) : Parcelable
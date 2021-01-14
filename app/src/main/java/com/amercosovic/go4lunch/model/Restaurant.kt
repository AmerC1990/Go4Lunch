package com.amercosovic.go4lunch.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Restaurant(
    @SerializedName("business_status")
    var businessStatus: String?,
    @SerializedName("geometry")
    var geometry: @RawValue Geometry,
    @SerializedName("icon")
    var icon: String?,
    @SerializedName("name")
    var name: String?,
    @SerializedName("opening_hours")
    var openingHours: @RawValue OpeningHours?,
    @SerializedName("photos")
    var photos: @RawValue List<Photo?>?,
    @SerializedName("place_id")
    var placeId: String?,
    @SerializedName("plus_code")
    var plusCode: @RawValue PlusCode?,
    @SerializedName("price_level")
    var priceLevel: Int?,
    @SerializedName("rating")
    var rating: Double?,
    @SerializedName("reference")
    var reference: String?,
    @SerializedName("scope")
    var scope: String?,
    @SerializedName("types")
    var types: List<String?>?,
    @SerializedName("user_ratings_total")
    var userRatingsTotal: Int?,
    @SerializedName("vicinity")
    var vicinity: String?
) : Parcelable


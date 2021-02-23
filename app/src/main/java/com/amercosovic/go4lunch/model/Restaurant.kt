package com.amercosovic.go4lunch.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Restaurant(
    @SerializedName("business_status")
    var businessStatus: String? = "",
    @SerializedName("geometry")
    var geometry: @RawValue Geometry = Geometry(
        Location(44.4, 44.4),
        Viewport(Northeast(44.4, 44.4), Southwest(44.4, 44.4))
    ),
    @SerializedName("icon")
    var icon: String? = "",
    @SerializedName("name")
    var name: String = "",
    @SerializedName("opening_hours")
    var openingHours: @RawValue OpeningHours? = OpeningHours(openNow = false),
    @SerializedName("photos")
    var photos: @RawValue List<Photo?>? = listOf(Photo(1, listOf(""), "", 1)),
    @SerializedName("place_id")
    var placeId: String? = "",
    @SerializedName("plus_code")
    var plusCode: @RawValue PlusCode? = PlusCode("", ""),
    @SerializedName("price_level")
    var priceLevel: Int? = 1,
    @SerializedName("rating")
    var rating: Double? = 1.0,
    @SerializedName("reference")
    var reference: String? = "",
    @SerializedName("scope")
    var scope: String? = "",
    @SerializedName("types")
    var types: List<String?>? = listOf(""),
    @SerializedName("user_ratings_total")
    var userRatingsTotal: Int? = 1,
    @SerializedName("vicinity")
    var vicinity: String? = ""
) : Parcelable


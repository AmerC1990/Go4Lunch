package com.amercosovic.go4lunch.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Photo(
    @SerializedName("height")
    var height: Int?,
    @SerializedName("html_attributions")
    var htmlAttributions: List<String?>?,
    @SerializedName("photo_reference")
    var photoReference: String?,
    @SerializedName("width")
    var width: Int?
) : Parcelable
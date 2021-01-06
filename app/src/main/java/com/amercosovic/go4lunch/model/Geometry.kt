package com.amercosovic.go4lunch.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Geometry(
    @SerializedName("location")
    val location: @RawValue Location,
    @SerializedName("viewport")
    val viewport: @RawValue Viewport
) : Parcelable
package com.amercosovic.go4lunch.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OpeningHours(
    @SerializedName("open_now")
    var openNow: Boolean?
) : Parcelable
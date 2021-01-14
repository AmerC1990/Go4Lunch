package com.amercosovic.go4lunch.model

import android.os.Parcelable
import com.amercosovic.go4lunch.model.Northeast
import com.amercosovic.go4lunch.model.Southwest
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Viewport(
    @SerializedName("northeast")
    val northeast: @RawValue Northeast?,
    @SerializedName("southwest")
    val southwest: @RawValue Southwest?
) : Parcelable
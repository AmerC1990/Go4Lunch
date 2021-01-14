package com.amercosovic.go4lunch.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PlusCode(
    @SerializedName("compound_code")
    var compoundCode: String?,
    @SerializedName("global_code")
    var globalCode: String?
) : Parcelable
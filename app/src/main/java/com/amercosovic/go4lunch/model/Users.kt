package com.amercosovic.go4lunch.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Users(
    var userImage: String? = null,
    var userName: String? = null,
    var userRestaurant: String? = null,
    var userRestaurantData: String? = null
) : Parcelable
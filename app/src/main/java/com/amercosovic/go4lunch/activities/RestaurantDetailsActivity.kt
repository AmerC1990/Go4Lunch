package com.amercosovic.go4lunch.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import com.amercosovic.go4lunch.R
import com.amercosovic.mapfragmentwithmvvmldemo.utility.Constants
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_restaurant_details.*

class RestaurantDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_details)

        val restaurantDataFromList: Parcelable? =
            intent.extras?.getParcelable("restaurantDataFromList")
        val restaurantDataFromMap: String? = intent.extras?.getString("restaurantDataFromMap")

        if (restaurantDataFromList == null) {
            if (restaurantDataFromMap != null) {
                insertRestaurantDetails(restaurantDataFromMap)
            }
        } else {
            insertRestaurantDetails(restaurantDataFromList.toString())
        }

    }

    private fun insertRestaurantDetails(data: String) {
        val rating =
            data.substringAfter("rating").replace("=", "").substringBefore(",").toDouble() / 1.66

        when {
            rating.toString().contains("1.") -> {
                restaurantDetailRating1.visibility = View.VISIBLE
            }
            rating.toString().contains("2.") -> {
                restaurantDetailRating1.visibility = View.VISIBLE
                restaurantDetailRating2.visibility = View.VISIBLE
            }
            rating.toString().contains("3.") -> {
                restaurantDetailRating1.visibility = View.VISIBLE
                restaurantDetailRating2.visibility = View.VISIBLE
                restaurantDetailRating3.visibility = View.VISIBLE
            }
        }

        Glide.with(imageOfRestaurant)
            .load(
                "https://maps.googleapis.com/maps/api/place/photo?maxwidth=200&photoreference=" +
                        data.toString().substringAfter("photoReference")
                            .replace(",", "").replace("]", "").replace("[", "").replace("=", "")
                            .substringBefore(" ") + "&key=" + Constants.GOOGLE_API_KEY
            )
            .centerCrop()
            .placeholder(R.drawable.defaultrestaurantimage)
            .error(R.drawable.defaultrestaurantimage)
            .into(imageOfRestaurant)

        val restaurantName = data.substringAfter("name=").replace("=", "").replace(",", "")
            .replace("=", "").replace(",", "").substringBefore("opening")

        nameOfRestaurantTextview.text =
            restaurantName.substring(0, Math.min(restaurantName.length, 24))

        val addressOfRestaurant = data.substringAfter("vicinity=")
            .substringBefore(",")
        addressOfRestaurantTextview.text = addressOfRestaurant.substring(
            0, Math.min
                (addressOfRestaurant.length, 33)
        )
    }


}
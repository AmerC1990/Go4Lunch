package com.amercosovic.go4lunch.adapters

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.activities.RestaurantDetailsActivity
import com.amercosovic.go4lunch.model.Restaurant
import com.amercosovic.mapfragmentwithmvvmldemo.utility.Constants
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil.computeDistanceBetween
import kotlinx.android.synthetic.main.fragment_restaurantlist_row.view.*

class RestaurantListAdapter(private val latitude: String, private val longitude: String) :
    RecyclerView.Adapter<RestaurantListAdapter.ViewHolder>() {

    var items: List<Restaurant> = ArrayList()

    fun setListData(data: List<Restaurant>) {
        this.items = data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_restaurantlist_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val restaurantData = items[position]

        val restaurantName = restaurantData.name

        holder.restaurantName.text =
            restaurantName?.substring(0, Math.min(restaurantName.length, 21))
                ?: "Restaurant Name"

        val restaurantAddress = restaurantData.vicinity?.toString()?.substringBefore(",")
            ?.replace("West", "W.")?.replace("East", "E.")?.replace("North", "N.")
            ?.replace("South", "S.")?.replace("Avenue", "")

        holder.restaurantAddress.text =
            restaurantAddress?.substring(0, Math.min(restaurantAddress.length, 18))
                ?: "Restaurant Address"

        if (restaurantData.openingHours?.openNow != null) {
            if (restaurantData.openingHours?.openNow.toString() == "true") {
                holder.openUntil.text = "Open Now"
                holder.openUntil.setTextColor(Color.GREEN)
            } else {
                holder.openUntil.text = "Closed"
                holder.openUntil.setTextColor(Color.RED)
            }

        }

        val rating = restaurantData.rating?.div(1.66)

        if (rating != null) {
            when {
                rating.toString().contains("1.") -> {
                    holder.rating1Star.visibility = View.VISIBLE
                }
                rating.toString().contains("2.") -> {
                    holder.rating1Star.visibility = View.VISIBLE
                    holder.rating2Star.visibility = View.VISIBLE
                }
                rating.toString().contains("3.") -> {
                    holder.rating1Star.visibility = View.VISIBLE
                    holder.rating2Star.visibility = View.VISIBLE
                    holder.rating3Star.visibility = View.VISIBLE
                }
            }
        }

        val myLocation = LatLng(latitude.toDouble(), longitude.toDouble())

        val restaurantLocation = LatLng(
            restaurantData.geometry.location.lat, restaurantData.geometry.location.lat
        )
        val distance =
            (computeDistanceBetween(myLocation, restaurantLocation)).toString().subSequence(0, 3)
                .toString() + "m"

        holder.distanceFromUser.text = if (distance != null) distance else "distance unknown"

        if (restaurantData.photos != null) {
            Glide.with(holder.imageOfRestaurant)
                .load(
                    "https://maps.googleapis.com/maps/api/place/photo?maxwidth=200&photoreference=" +
                            restaurantData.photos.toString().substringAfter("photoReference")
                                .replace(",", "").replace("]", "").replace("[", "").replace("=", "")
                                .substringBefore(" ") + "&key="
                            + Constants.GOOGLE_API_KEY
                )
                .centerCrop()
                .placeholder(R.drawable.defaultrestaurantimage)
                .error(R.drawable.defaultrestaurantimage)
                .into(holder.imageOfRestaurant)
        } else if (restaurantData.photos == null) {
            Glide.with(holder.imageOfRestaurant)
                .load(R.drawable.defaultrestaurantimage)
                .centerCrop()
                .placeholder(R.drawable.defaultrestaurantimage)
                .error(R.drawable.defaultrestaurantimage)
                .into(holder.imageOfRestaurant)
        }

        Log.d(
            "distancerest:", holder.distanceFromUser.text.toString() + "-" +
                    holder.restaurantName.text.toString()
        )

        Log.d("between", holder.distanceFromUser.text.toString())

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RestaurantDetailsActivity::class.java)
            intent.putExtra("restaurantDataFromList", restaurantData)
            holder.itemView.context.startActivity(intent)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val restaurantName: TextView = itemView.nameOfrestaurantTextview
        val restaurantAddress: TextView = itemView.addressOfRestaurantInList
        val openUntil: TextView = itemView.openUntilTextview
        var distanceFromUser: TextView = itemView.distanceFromUserToRestaurant
        val imageOfRestaurant: ImageView = itemView.restaurantImage
        val rating1Star: ImageView = itemView.restaurantRatingIcon1
        val rating2Star: ImageView = itemView.restaurantRatingIcon2
        val rating3Star: ImageView = itemView.restaurantRatingIcon3


    }
}
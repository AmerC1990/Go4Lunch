package com.amercosovic.go4lunch.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.model.Result
import com.amercosovic.mapfragmentwithmvvmldemo.utility.Constants
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil.computeDistanceBetween
import kotlinx.android.synthetic.main.fragment_restaurantlist_row.view.*

class RestaurantListAdapter(private val latitude: String, private val longitude: String) :
    RecyclerView.Adapter<RestaurantListAdapter.ViewHolder>() {

    var items: List<Result> = ArrayList()

    fun setListData(data: List<Result>) {
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


        val restaurantName = restaurantData.name.toString().substringBefore(" ") + " " +
                restaurantData.name.substringAfter(" ").substringBefore(" ")
        if (restaurantName.substringBefore(" ") == restaurantName.substringAfter(" ")) {
            holder.restaurantName.text = restaurantName.substringBefore(" ")
        } else {
            holder.restaurantName.text = restaurantName
        }
        holder.typeAndAddress.text =
            restaurantData.types.toString().substringBefore(",").removePrefix("=").removePrefix("]")
                .removePrefix("[").replace("_", " ")
                .capitalize() + " - " + restaurantData.vicinity.toString().substringBefore(",")
                .replace("West", "W.").replace("East", "E.").replace("North", "N.")
                .replace("South", "S.").replace("Avenue", "")
        if (restaurantData.openingHours.openNow.toString() == "true") {
            holder.openUntil.text = "Open Now"
            holder.openUntil.setTextColor(Color.GREEN)
        } else {
            holder.openUntil.text = "Closed"
            holder.openUntil.setTextColor(Color.RED)
        }

        val rating = restaurantData.rating / 1.66

        if (rating.toString().contains("1.")) {
            holder.rating1Star.visibility = View.VISIBLE
        } else if (rating.toString().contains("2.")) {
            holder.rating1Star.visibility = View.VISIBLE
            holder.rating2Star.visibility = View.VISIBLE
        } else if (rating.toString().contains("3.")) {
            holder.rating1Star.visibility = View.VISIBLE
            holder.rating2Star.visibility = View.VISIBLE
            holder.rating3Star.visibility = View.VISIBLE
        }

        val myLocation = LatLng(latitude.toDouble(), longitude.toDouble())
        val restaurantLocation = LatLng(
            restaurantData.geometry.location.lat, restaurantData.geometry.location.lat
        )
        val distance =
            (computeDistanceBetween(myLocation, restaurantLocation)).toString().subSequence(0, 3)
        holder.distanceFromUser.text = distance.toString() + "m"
        Glide.with(holder.imageOfRestaurant)
            .load(
                "https://maps.googleapis.com/maps/api/place/photo?maxwidth=200&photoreference=" +
                        restaurantData.photos.toString().substringAfter("photoReference")
                            .replace(",", "").replace("]", "").replace("[", "").replace("=", "")
                            .substringBefore(" ") + "&key="
                        + Constants.GOOGLE_API_KEY
            )
            .placeholder(R.drawable.defaultrestaurantimage)
            .error(R.drawable.defaultrestaurantimage)
            .into(holder.imageOfRestaurant)

        Log.d(
            "distancerest:", holder.distanceFromUser.text.toString() + "-" +
                    holder.restaurantName.text.toString()
        )

        Log.d("between", holder.distanceFromUser.text.toString())
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val restaurantName: TextView = itemView.nameOfrestaurantTextview
        val typeAndAddress: TextView = itemView.typeAndAddressOfRestaurant
        val openUntil: TextView = itemView.openUntilTextview
        var distanceFromUser: TextView = itemView.distanceFromUserToRestaurant
        val imageOfRestaurant: ImageView = itemView.restaurantImage
        val rating1Star: ImageView = itemView.restaurantRatingIcon1
        val rating2Star: ImageView = itemView.restaurantRatingIcon2
        val rating3Star: ImageView = itemView.restaurantRatingIcon3


    }
}
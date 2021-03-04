package com.amercosovic.go4lunch.adapters

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.activities.RestaurantDetailsActivity
import com.amercosovic.go4lunch.model.Restaurant
import com.amercosovic.go4lunch.utility.Constants
import com.amercosovic.go4lunch.utility.Translate.translate
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.SphericalUtil.computeDistanceBetween
import kotlinx.android.synthetic.main.fragment_restaurantlist_row.view.*


class RestaurantListAdapter(
    private val latitude: String,
    private val longitude: String
) :
    RecyclerView.Adapter<RestaurantListAdapter.ViewHolder>() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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

    // bind data
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val restaurantData = items[position]

        val restaurantName = restaurantData.name + " "

        holder.restaurantName.text =
            restaurantName.substring(0, Math.min(restaurantName.length, 21))

        val restaurantAddress = restaurantData.vicinity?.toString()?.substringBefore(",")
            ?.replace("West", "W.")?.replace("East", "E.")?.replace("North", "N.")
            ?.replace("South", "S.")?.replace("Avenue", "")

        holder.restaurantAddress.text =
            restaurantAddress?.substring(0, Math.min(restaurantAddress.length, 18))
                ?: R.string.restaurant_address.toString()

        if (restaurantData.openingHours?.openNow != null) {
            if (restaurantData.openingHours?.openNow.toString() == "true") {
                holder.openUntil.text = translate(english = "Open Now", spanish = "Abierta Ahora")
                holder.openUntil.setTextColor(Color.GREEN)
            } else {
                holder.openUntil.text = translate(english = "Closed", spanish = "Cerrada")
                holder.openUntil.setTextColor(Color.RED)
            }

        }

        val rating = restaurantData.rating?.div(1.66)

        if (rating != null) {
            holder.ratingStar.rating = rating.toFloat()
        }

        val myLocation = LatLng(latitude.toDouble(), longitude.toDouble())


        val restaurantLocation = LatLng(
            restaurantData.geometry.location.lat, restaurantData.geometry.location.lng
        )


        val distance = computeDistanceBetween(myLocation, restaurantLocation).toString()
        holder.distanceFromUser.text = distance.toString().substringBefore(".") + "m"

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

        val userRef = firestore.collection(restaurantName.removeSuffix(" "))
        Log.d("restaurantNameAdapter", "--$restaurantName--")
        userRef.get().addOnSuccessListener { snapshot ->
            userRef.document("count").get().addOnSuccessListener { documentSnapshot ->
                if (!documentSnapshot["count"].toString()
                        .contains("null") && documentSnapshot["count"].toString() != "0"
                ) {
                    holder.numberOfColleagues.text =
                        "(" + documentSnapshot["count"].toString() + ")"
                    holder.numberOfColleagues.visibility = View.VISIBLE
                    holder.colleagueIcon.visibility = View.VISIBLE
                } else {
                    holder.numberOfColleagues.visibility = View.INVISIBLE
                    holder.colleagueIcon.visibility = View.INVISIBLE
                    holder.numberOfColleagues.text = ""
                }
            }
        }

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
        val ratingStar: RatingBar = itemView.restaurantRatingFromList
        val numberOfColleagues: TextView = itemView.numberOfColleaguesTextview
        val colleagueIcon: ImageView = itemView.defaultColleagueIcon
    }

}
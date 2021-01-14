package com.amercosovic.go4lunch.activities

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.adapters.SingleRestaurantUserAdapter
import com.amercosovic.go4lunch.model.Users
import com.amercosovic.mapfragmentwithmvvmldemo.utility.Constants
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_restaurant_details.*

class RestaurantDetailsActivity : AppCompatActivity() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collectionReference: CollectionReference = db.collection("users")

    var adapter: SingleRestaurantUserAdapter? = null

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

    private fun insertRestaurantDetails(data: String?) {
        if (!data.toString().substringAfter("rating=").substringBefore(",").contains("null")) {
            val rating =
                data?.substringAfter("rating")?.replace("=", "")?.substringBefore(",")?.toDouble()
                    ?.div(1.66)

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
        } else {
            restaurantDetailRating1.visibility = View.VISIBLE
            restaurantDetailRating2.visibility = View.VISIBLE
        }

        if (!data.toString().substringAfter("photoReference").replace(",", "").replace("]", "")
                .replace("[", "").replace("=", "").substringBefore(" ").isNullOrEmpty()
        ) {
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
        } else {
            Glide.with(imageOfRestaurant)
                .load(R.drawable.defaultrestaurantimage)
                .centerCrop()
                .placeholder(R.drawable.defaultrestaurantimage)
                .error(R.drawable.defaultrestaurantimage)
                .into(imageOfRestaurant)
        }


        val restaurantName = data?.substringAfter("name=")?.replace("=", "")?.replace(",", "")
            ?.replace("=", "")?.replace(",", "")?.substringBefore("opening")

        if (restaurantName != null) {
            maintainChosenRestaurantCheckmark(restaurantName)
            setUpRecyclerView(collectionReference, restaurantName)
            plusIcon.setOnClickListener {
                addUserToFireStore(
                    userRestaurant = restaurantName,
                    userName = FirebaseAuth.getInstance().currentUser?.displayName.toString(),
                    userImage = FirebaseAuth.getInstance().currentUser?.photoUrl.toString()
                )
            }
        }

        nameOfRestaurantTextview.text =
            restaurantName?.substring(0, Math.min(restaurantName.length, 21)) ?: "Restaurant"

        val addressOfRestaurant = data?.substringAfter("vicinity=")
            ?.substringBefore(",")

        addressOfRestaurantTextview.text = addressOfRestaurant?.substring(
            0, Math.min(
                addressOfRestaurant.length, 33
            )
        ) ?: "Address"
    }

    private fun setUpRecyclerView(
        collectionReference: CollectionReference,
        restaurantName: String
    ) {
        val query: Query = collectionReference.whereEqualTo("userRestaurant", restaurantName)

        val firestoreRecyclerOptions: FirestoreRecyclerOptions<Users> =
            FirestoreRecyclerOptions.Builder<Users>()
                .setQuery(query, Users::class.java)
                .build()

        val decorator = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        adapter = SingleRestaurantUserAdapter(firestoreRecyclerOptions)
        singleRestaurantRecyclerView.layoutManager = LinearLayoutManager(this)
        singleRestaurantRecyclerView.adapter = adapter
        singleRestaurantRecyclerView.addItemDecoration(decorator)
        adapter?.notifyDataSetChanged()
    }

    private fun addUserToFireStore(userName: String, userImage: String, userRestaurant: String) {
        val user = hashMapOf(
            "userName" to userName,
            "userImage" to userImage,
            "userRestaurant" to userRestaurant
        )

        db.collection("users").document(userName)
            .set(user as Map<String, Any>).addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Successful", Toast.LENGTH_LONG).show()
                plusIcon.setImageResource(R.drawable.restaurant_chosen_check)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to add", Toast.LENGTH_LONG).show()
                Log.e("Error", exception.message)
            }
    }

    private fun maintainChosenRestaurantCheckmark(restaurantName: String) {
        val reference = db.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.displayName.toString())
        reference.get().addOnSuccessListener { document ->
            if (document != null) {
                if (document.data.toString().contains(restaurantName)) {
                    plusIcon.setImageResource(R.drawable.restaurant_chosen_check)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter?.stopListening()
    }

}
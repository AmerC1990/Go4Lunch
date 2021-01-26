package com.amercosovic.go4lunch.activities


import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_restaurant_details.*


class RestaurantDetailsActivity : AppCompatActivity() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collectionReference: CollectionReference = firestore.collection("users")
    var adapter: SingleRestaurantUserAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_details)

        val restaurantDataFromList: Parcelable? =
            intent.extras?.getParcelable("restaurantDataFromList")
        val restaurantDataFromMap: String? = intent.extras?.getString("restaurantDataFromMap")
        val restaurantDataFromWorkmates: String? = intent.extras?.getString("userRestaurantData")
        val restaurantDataFromNavDrawer: String? =
            intent.extras?.getString("restaurantDataFromNavDrawerClick")

        if (restaurantDataFromList != null) {
            insertRestaurantDetails(restaurantDataFromList.toString())
        }
        if (restaurantDataFromMap != null) {
            insertRestaurantDetails(restaurantDataFromMap)
        }
        if (restaurantDataFromWorkmates != null) {
            insertRestaurantDetails(restaurantDataFromWorkmates)
        }
        if (restaurantDataFromNavDrawer != null) {
            insertRestaurantDetails(restaurantDataFromNavDrawer)
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
                if (plusIcon.drawable.constantState == ContextCompat.getDrawable(
                        this,
                        R.drawable.whiteplusicon
                    )?.constantState
                ) {
                    addUserToFireStore(
                        userRestaurant = restaurantName,
                        userName = FirebaseAuth.getInstance().currentUser?.displayName.toString(),
                        userImage = FirebaseAuth.getInstance().currentUser?.photoUrl.toString(),
                        userRestaurantData = data
                    )
                } else if (plusIcon.drawable.constantState == ContextCompat.getDrawable(
                        this,
                        R.drawable.restaurant_chosen_check
                    )?.constantState
                ) {
                    updateUserRestaurantFieldToUndecided(
                        userName = FirebaseAuth.getInstance().currentUser?.displayName.toString(),
                        userRestaurant = restaurantName
                    )
                }
            }
            val sharedPrefs: SharedPreferences =
                this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
            val likedRestaurant = sharedPrefs.getString(restaurantName, null)
            if (!likedRestaurant.isNullOrEmpty()) {
                likeIcon.setColorFilter(this.resources.getColor(R.color.colorPrimaryDark))
                likeRestaurantTextview.setTextColor(Color.parseColor("#24D689"))
                likeRestaurantTextview.text = "LIKED"
            }
            likeRestaurantTextview.setOnClickListener {
                val myRestaurant = sharedPrefs.getString(restaurantName, null)
                if (!myRestaurant.isNullOrEmpty()) {
                    unLikeRestaurantRemoveFromSharedPrefs(
                        restaurantName = restaurantName,
                        sharedPrefs = sharedPrefs
                    )
                } else if (myRestaurant.isNullOrEmpty()) {
                    likeRestaurantAddToSharedPrefs(
                        restaurantName = restaurantName,
                        sharedPrefs = sharedPrefs
                    )
                }
            }
            likeIcon.setOnClickListener {
                val myRestaurant = sharedPrefs.getString(restaurantName, null)
                if (!myRestaurant.isNullOrEmpty()) {
                    unLikeRestaurantRemoveFromSharedPrefs(
                        restaurantName = restaurantName,
                        sharedPrefs = sharedPrefs
                    )
                } else if (myRestaurant.isNullOrEmpty()) {
                    likeRestaurantAddToSharedPrefs(
                        restaurantName = restaurantName,
                        sharedPrefs = sharedPrefs
                    )
                }
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

    private fun addUserToFireStore(
        userName: String,
        userImage: String,
        userRestaurant: String,
        userRestaurantData: String
    ) {
        val user = hashMapOf(
            "userName" to userName,
            "userImage" to userImage,
            "userRestaurant" to userRestaurant,
            "userRestaurantData" to userRestaurantData
        )
        val addUserCount = hashMapOf(
            "count" to FieldValue.increment(+1)
        )
        val subtractUserCount = hashMapOf(
            "count" to FieldValue.increment(-1)
        )
        val startUserCount = hashMapOf(
            "count" to 1
        )

        val currentRestaurantRef = firestore.collection("users").document(userName)
        currentRestaurantRef.get().addOnSuccessListener { currentRestaurant ->
            if (currentRestaurant["userRestaurant"] == "undecided") {
                firestore.collection("users").document(userName)
                    .set(user as Map<String, Any>).addOnSuccessListener { documentReference ->
                        Toast.makeText(this, "Successful", Toast.LENGTH_LONG).show()
                        val currentRestaurantCollectionDocument =
                            firestore.collection(userRestaurant).document("count")
                        currentRestaurantCollectionDocument.get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                currentRestaurantCollectionDocument.update(
                                    "count",
                                    FieldValue.increment(+1)
                                )
                            } else {
                                currentRestaurantCollectionDocument.set(startUserCount)
                            }
                        }
                        plusIcon.setImageResource(R.drawable.restaurant_chosen_check)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to add", Toast.LENGTH_LONG).show()
                        Log.e("Error", exception.message)
                    }
            } else if (currentRestaurant["userRestaurant"] != "undecided") {
                firestore.collection("users").document(userName)
                    .set(user as Map<String, Any>).addOnSuccessListener { documentReference ->
                        Toast.makeText(this, "Successful", Toast.LENGTH_LONG).show()
                        val currentRestaurantCollectionDocument =
                            firestore.collection(currentRestaurant["userRestaurant"].toString())
                                .document("count")
                        currentRestaurantCollectionDocument.get().addOnSuccessListener { document ->
                            currentRestaurantCollectionDocument.update(subtractUserCount as Map<String, Any>)
                            val newRestaurantRef =
                                firestore.collection(userRestaurant).document("count")
                            newRestaurantRef.get().addOnSuccessListener { documentReference2 ->
                                if (!documentReference2.exists()) {
                                    firestore.collection(userRestaurant).document("count")
                                        .set(startUserCount)
                                } else {
                                    firestore.collection(userRestaurant).document("count").update(
                                        addUserCount as Map<String, Any>
                                    )
                                }
                            }
                        }
                        plusIcon.setImageResource(R.drawable.restaurant_chosen_check)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to add", Toast.LENGTH_LONG).show()
                        Log.e("Error", exception.message)
                    }
            }
        }
    }


    private fun updateUserRestaurantFieldToUndecided(userName: String, userRestaurant: String) {
        val userRef = firestore.collection("users").document(userName)

        val updates = hashMapOf(
            "userRestaurantData" to FieldValue.delete(),
            "userRestaurant" to "undecided"
        )
        val userCountSubtract = hashMapOf(
            "count" to FieldValue.increment(-1)
        )

        userRef.update(updates).addOnCompleteListener {
            plusIcon.setImageResource(R.drawable.whiteplusicon)
        }
        val currentRestaurantRef = firestore.collection(userRestaurant).document("count")
        currentRestaurantRef.get().addOnSuccessListener { document ->
            if (document["count"].toString() != "0") {
                currentRestaurantRef.update(userCountSubtract as Map<String, Any>)
            }
        }
    }

    private fun maintainChosenRestaurantCheckmark(restaurantName: String) {
        val reference = firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.displayName.toString())
        reference.get().addOnSuccessListener { document ->
            if (document != null) {
                if (document.data.toString().contains(restaurantName)) {
                    plusIcon.setImageResource(R.drawable.restaurant_chosen_check)
                }
            }
        }
    }

    private fun unLikeRestaurantRemoveFromSharedPrefs(
        restaurantName: String?,
        sharedPrefs: SharedPreferences
    ) {
        val editor = sharedPrefs.edit()
        editor.apply {
            remove(restaurantName)
        }.apply()
        likeIcon.setColorFilter(this.resources.getColor(R.color.myBlack))
        likeRestaurantTextview.setTextColor(Color.parseColor("#070606"))
        likeRestaurantTextview.text = "LIKE"
    }

    private fun likeRestaurantAddToSharedPrefs(
        restaurantName: String?,
        sharedPrefs: SharedPreferences
    ) {
        val myNewLikedRestaurant = restaurantName
        val editor = sharedPrefs.edit()
        editor.apply {
            putString(restaurantName, myNewLikedRestaurant)
        }.apply()
        likeIcon.setColorFilter(this.resources.getColor(R.color.colorPrimaryDark))
        likeRestaurantTextview.setTextColor(Color.parseColor("#24D689"))
        likeRestaurantTextview.text = "LIKED"
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
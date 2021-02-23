package com.amercosovic.go4lunch.activities


import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.amercosovic.go4lunch.PlaceDetailsState
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.adapters.SingleRestaurantUserAdapter
import com.amercosovic.go4lunch.model.Restaurant
import com.amercosovic.go4lunch.model.Users
import com.amercosovic.go4lunch.receiver.AlarmReceiver
import com.amercosovic.go4lunch.utility.Translate.translate
import com.amercosovic.go4lunch.viewmodels.RestaurantsViewModel
import com.amercosovic.mapfragmentwithmvvmldemo.utility.Constants
import com.bumptech.glide.Glide
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_restaurant_details.*
import kotlinx.android.synthetic.main.fragment_restaurantlist.*
import java.time.Duration
import java.time.LocalTime
import java.util.*
import java.util.concurrent.TimeUnit


class RestaurantDetailsActivity : AppCompatActivity() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collectionReference: CollectionReference = firestore.collection("users")
    var adapter: SingleRestaurantUserAdapter? = null
    private var viewModel = RestaurantsViewModel()
    private lateinit var alarmManager: AlarmManager
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    private val channelId = "lunch"
    private val description = "notification"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_details)
        // attach livedata observers
        attachObservers()
        // create notification channel
        createNotificationChannel()
        //change font size of "call", "website" and "like if device is in spanish
        changeFontSizeIfSpanish()

        // set status bar to transparent
        setTransparentStatusBar()

        // receive data from previous fragment and call insertRestaurantDetails
        receiveDataAndCheckIfNullOrEmpty()
    }

    // populate ui with data
    private fun insertRestaurantDetails(data: Restaurant?) {
        val mapper = jacksonObjectMapper()
        val restaurantDataForFirestore = mapper.writeValueAsString(data)

        data?.placeId?.let {
            viewModel.fetchWebsiteAndPhoneNumberData(
                it
            )
        }

        if (!data?.rating.toString().isNullOrEmpty()) {
            val rating = data?.rating?.div(1.66)

            if (rating != null) {
                restaurantRating.rating = rating.toFloat()
            }
        }

        if (!data?.photos.isNullOrEmpty()
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

        val restaurantName = data?.name

        val addressOfRestaurant = data?.vicinity

        addressOfRestaurantTextview.text = addressOfRestaurant?.substring(
            0, Math.min(
                addressOfRestaurant.length, 33
            )
        ) ?: R.string.address.toString()

        if (restaurantName != null) {
            getCurrentUser()?.let { maintainChosenRestaurantCheckmark(restaurantName, it) }
            setUpRecyclerView(collectionReference, restaurantName)
            plusIcon.setOnClickListener {
                if (plusIcon.drawable.constantState == ContextCompat.getDrawable(
                        this,
                        R.drawable.whiteplusicon
                    )?.constantState
                ) {
                    getCurrentUser()?.let { it1 ->
                        addUserToFireStore(
                            userRestaurant = restaurantName.removeSuffix(" "),
                            userName = it1,
                            userImage = FirebaseAuth.getInstance().currentUser?.photoUrl.toString(),
                            userRestaurantData = restaurantDataForFirestore.toString(),
                            restaurantAddress = addressOfRestaurantTextview.text.toString()
                        )
                    }
                } else if (plusIcon.drawable.constantState == ContextCompat.getDrawable(
                        this,
                        R.drawable.restaurant_chosen_check
                    )?.constantState
                ) {
                    getCurrentUser()?.let { it1 ->
                        updateUserRestaurantFieldToUndecided(
                            userName = it1,
                            userRestaurant = restaurantName
                        )
                    }
                }
            }
            val sharedPrefs: SharedPreferences =
                this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
            val likedRestaurant = sharedPrefs.getString(restaurantName, null)
            if (!likedRestaurant.isNullOrEmpty()) {
                likeIcon.setColorFilter(this.resources.getColor(R.color.colorPrimaryDark))
                likeRestaurantTextview.setTextColor(Color.parseColor("#24D689"))
                likeRestaurantTextview.text = translate(spanish = "GUSTO", english = "LIKED")
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
    }

    // set up firestore recyclerview
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

    // add user restaurant info to firestore
    private fun addUserToFireStore(
        userName: String,
        userImage: String,
        userRestaurant: String,
        userRestaurantData: String,
        restaurantAddress: String
    ) {
        val sharedPreferences = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
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
                        Toast.makeText(
                            this,
                            translate(
                                english = "You're having lunch at $userRestaurant!",
                                spanish = "Estás almorzando en $userRestaurant!"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
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
                        editor.apply {
                            putString("restaurantAddress", restaurantAddress)
                        }.apply()
                        setAlarm()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            translate(english = "Failed to add", spanish = "No se pudo agregar"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } else if (currentRestaurant["userRestaurant"] != "undecided") {
                firestore.collection("users").document(userName)
                    .set(user as Map<String, Any>).addOnSuccessListener { documentReference ->
                        editor.apply {
                            putString("restaurantAddress", restaurantAddress)
                        }.apply()
                        Toast.makeText(
                            this,
                            translate(
                                english = "You're having lunch at $userRestaurant!",
                                spanish = "Estás almorzando en $userRestaurant!"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
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
                        setAlarm()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            translate(english = "Failed to add", spanish = "No se pudo agregar"),
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("Error", exception.message)
                    }
            }
        }
    }

    // update unselected restaurant user to firestore
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
            val sharedPreferences = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            plusIcon.setImageResource(R.drawable.whiteplusicon)
            editor.apply {
                remove("restaurantAddress")
            }.apply()
            cancelAlarm()
        }
        val currentRestaurantRef = firestore.collection(userRestaurant).document("count")
        currentRestaurantRef.get().addOnSuccessListener { document ->
            if (document["count"].toString() != "0") {
                currentRestaurantRef.update(userCountSubtract as Map<String, Any>)
            }
        }
    }

    // maintain chosen checkmark when user revisits activity
    private fun maintainChosenRestaurantCheckmark(restaurantName: String, currentUser: String) {
        val reference = firestore.collection("users").document(currentUser)
        reference.get().addOnSuccessListener { document ->
            if (document != null) {
                if (document.data.toString().contains(restaurantName)) {
                    plusIcon.setImageResource(R.drawable.restaurant_chosen_check)
                }
            }
        }
    }

    // unlike restaurant
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
        likeRestaurantTextview.text = translate(spanish = "ME GUSTA", english = "LIKE")
    }

    // like restaurants
    private fun likeRestaurantAddToSharedPrefs(
        restaurantName: String?,
        sharedPrefs: SharedPreferences
    ) {
        val editor = sharedPrefs.edit()
        editor.apply {
            putString(restaurantName, restaurantName)
        }.apply()
        likeIcon.setColorFilter(this.resources.getColor(R.color.colorPrimaryDark))
        likeRestaurantTextview.setTextColor(Color.parseColor("#24D689"))
        likeRestaurantTextview.text = translate(spanish = "GUSTO", english = "LIKED")
    }

    // set status bar to transparent
    fun Activity.setTransparentStatusBar() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    // attach livedata observers, set on click listeners for call and visit website when api call is successful
    private fun attachObservers() {
        viewModel.state2.observe(this, Observer { state2 ->
            when (state2) {
                is PlaceDetailsState.Success -> {
                    callIcon.setOnClickListener {
                        callRestaurant(state2 = state2)
                    }
                    callRestaurantTextview.setOnClickListener {
                        callRestaurant(state2 = state2)
                    }
                    visitWebsiteIcon.setOnClickListener {
                        openRestaurantWebsite(state2.placeDetailsResponse.result.website.toString())
                    }
                    websiteTextview.setOnClickListener {
                        openRestaurantWebsite(state2.placeDetailsResponse.result.website.toString())
                    }
                }
                is Error -> {
                    restaurantListProgressBar.visibility = View.GONE
                }
            }
        })
    }

    // call restaurant explicit intent
    private fun callRestaurant(state2: PlaceDetailsState.Success) {
        val dialIntent = Intent(Intent.ACTION_DIAL)
        dialIntent.data = Uri.parse(
            "tel:" + state2.placeDetailsResponse.result.formattedPhoneNumber.toString()
                .replace("(", "").replace(")", "")
                .replace(" ", "").replace("-", "")
        )
        startActivity(dialIntent)
    }

    // set alarm for notification
    private fun setAlarm() {
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntent2 =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        // 15 seconds = 15000 milliseconds
        val timeUntilNoon = Duration.between(LocalTime.now(), LocalTime.NOON).seconds
        val timeUntilNoonInMillis = TimeUnit.SECONDS.toMillis(timeUntilNoon.toLong())
        val myAlarm = AlarmManager.AlarmClockInfo(
            System.currentTimeMillis() + 15000,
            pendingIntent2
        )
        alarmManager.setAlarmClock(myAlarm, pendingIntent)
    }

    // cancel alarm
    private fun cancelAlarm() {
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // create my notification channel for notification in broadcast receiver
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(
                channelId, description, NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.BLACK
            notificationChannel.enableVibration(false)

            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    // open restaurant intent
    private fun openRestaurantWebsite(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    // get current user
    private fun getCurrentUser(): String? {
        var currentUser: String = ""
        if (!FirebaseAuth.getInstance().currentUser?.displayName.toString().isNullOrEmpty() &&
            !FirebaseAuth.getInstance().currentUser?.displayName.toString().contains("null")
        ) {
            currentUser = FirebaseAuth.getInstance().currentUser?.displayName.toString()
            return currentUser
        } else {
            val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
            val currentUserFromSharedPrefs = sharedPrefs.getString(
                FirebaseAuth.getInstance().currentUser?.email.toString(),
                null
            )
            if (currentUserFromSharedPrefs != null) {
                currentUser = currentUserFromSharedPrefs.toString()
                return currentUser
            }
        }
        return currentUser
    }

    // get data from passed intent and pass to populate ui
    private fun receiveDataAndCheckIfNullOrEmpty() {
        val restaurantDataFromList: Restaurant? =
            intent.extras?.getParcelable("restaurantDataFromList") as Restaurant?
        val restaurantDataFromMap: Restaurant? =
            intent.extras?.getParcelable("restaurantDataFromMap") as Restaurant?
        val restaurantDataFromWorkmates: Restaurant? =
            intent.extras?.getParcelable("userRestaurantData") as Restaurant?
        val restaurantDataFromNavDrawer: Restaurant? =
            intent.extras?.getParcelable("restaurantDataFromNavDrawerClick") as Restaurant?
        val restaurantDataFromNotification: Restaurant? =
            intent.extras?.getParcelable("restaurantDataFromNotification") as Restaurant?

        if (restaurantDataFromList != null) {
            insertRestaurantDetails(restaurantDataFromList)
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
        if (restaurantDataFromNotification != null) {
            insertRestaurantDetails(restaurantDataFromNotification)
        }
    }

    // change font size of call, website and like textviews if translated in spanish
    private fun changeFontSizeIfSpanish() {
        val language = Locale.getDefault().displayLanguage

        if (language.toString() == "español") {
            likeRestaurantTextview.textSize = 12F
            callRestaurantTextview.textSize = 11F
            websiteTextview.textSize = 12F
        }
    }

    // start listening for changes with firestore adapter
    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    // stop listening for changes with firestore adapter
    override fun onDestroy() {
        super.onDestroy()
        adapter?.stopListening()
    }

}
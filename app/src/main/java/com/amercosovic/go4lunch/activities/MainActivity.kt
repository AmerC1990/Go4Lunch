package com.amercosovic.go4lunch.activities


import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.fragments.*
import com.amercosovic.go4lunch.receiver.AlarmReceiver
import com.amercosovic.go4lunch.utility.RestaurantFromFirestore
import com.amercosovic.go4lunch.utility.Translate.translate
import com.bumptech.glide.Glide
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_restaurant_details.*
import kotlinx.android.synthetic.main.fragment_restaurantlist.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class MainActivity : AppCompatActivity() {


    var googleSignInClient: GoogleSignInClient? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private val settingsFragment = SettingsFragment()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var alarmManager: AlarmManager
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    private val channelId = "lunch"
    private val description = "notification"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // hide action bar
        this.supportActionBar?.hide()
        // set up search view
        setUpSearchView()
        // create notification channel for alarm receiver
        createNotificationChannel()

        val mapFragment = MapFragment()

        makeCurrentFragment(mapFragment)
        setupNavDrawer()
        setUpBottomNavClicks()

        // get current user info and display to textviews
        usernameTextview.text = getCurrentUser()
        userEmailTextview.text = getCurrentUserEmail()

        // load user profile picture
        Glide.with(currentUserImageView)
            .load(FirebaseAuth.getInstance().currentUser?.photoUrl)
            .centerCrop()
            .circleCrop()
            .placeholder(R.drawable.defaultprofilepicture)
            .error(R.drawable.defaultprofilepicture)
            .into(currentUserImageView)
    }

    // handle clicks of nav drawer items - open corresponding fragment or activity
    fun handleNavDrawerItemClicks(view: View) {
        when (view.id) {
            R.id.yourLunchButton -> {
                getCurrentUser()?.let { passDataOpenRestaurantActivity(it) }
            }
            R.id.settingsButton -> {
                makeCurrentFragment(settingsFragment)
                toolbarTitle.text =
                    translate(english = "             Settings", spanish = "      Configuraciones")
            }
            R.id.logoutButton -> {
                val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
                val editor = sharedPrefs.edit()
                if (isAlarmOn()) {
                    editor.apply {
                        putString("alarmWasOnAtLogout", "alarmWasOnAtLogout")
                    }.apply()
                }
                lifecycleScope.launch(IO) {
                    logout()
                    withContext(Main) {
                        alarmOnWhenLogout()
                        cancelAlarm()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        overridePendingTransition(R.anim.slide_out_down, R.anim.slide_in_down)
                        startActivity(intent)
                    }
                }
            }
        }
        mainActivityDrawerLayout?.let {
            it.closeDrawer(Gravity.LEFT)
        }
    }

    // replace current fragment
    private fun makeCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer, fragment)
            commit()
        }
    }

    // logout
    private fun logout() {
        val googleSignIn = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this@MainActivity, googleSignIn)
        FirebaseAuth.getInstance().signOut()
        LoginManager.getInstance().logOut()
        googleSignInClient?.signOut()
        val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.apply {
            remove(FirebaseAuth.getInstance().currentUser?.email.toString())
        }.apply()
    }

    // set up nav drawer
    private fun setupNavDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this, mainActivityDrawerLayout, toolbar, R.string.open,
            R.string.close
        )
        drawerToggle?.let {
            it.syncState()
        }
    }

    // set up bottom navigation item clicks
    private fun setUpBottomNavClicks() {
        val mapFragment = MapFragment()
        val listViewFragment = RestaurantListFragment()
        val workmatesFragment = WorkmatesFragment()
        bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.ic_mapview -> {
                    makeCurrentFragment(mapFragment)
                    toolbarTitle.text =
                        translate(english = "       I'm Hungry", spanish = "     Tengo hambre")
                }
                R.id.ic_listview -> {
                    makeCurrentFragment(listViewFragment)
                    toolbarTitle.text =
                        translate(english = "       I'm Hungry", spanish = "     Tengo hambre")
                }
                R.id.ic_workmates -> {
                    makeCurrentFragment(workmatesFragment)
                    toolbarTitle.text = translate(
                        english = " Available workmates",
                        spanish = " Trabajo disponibles"
                    )
                }
            }
            true
        }
    }

    // set up search view
    private fun setUpSearchView() {
        searchView.layoutParams = androidx.appcompat.widget.Toolbar.LayoutParams(Gravity.RIGHT)
        searchView.maxWidth = Int.MAX_VALUE
        searchView.setOnSearchClickListener {
            if (searchView.hasFocus()) {
                toolbarTitle.visibility = View.GONE
            }
        }
        searchView.setOnCloseListener {
            toolbarTitle.visibility = View.VISIBLE
            false
        }
    }

    // open restaurant activity and pass data of restaurant
    private fun passDataOpenRestaurantActivity(user: String) {
        val restaurantReference = firestore.collection("users")
            .document(user)
        restaurantReference.get().addOnSuccessListener { document ->
            if (!document["userRestaurant"].toString().contains("undecided")) {
                val data = document["userRestaurantData"].toString()
                val intent = Intent(this, RestaurantDetailsActivity::class.java)
                lifecycleScope.launch(Default) {
                    val restaurant = RestaurantFromFirestore.getRestaurant(data)
                    withContext(Main) {
                        intent.putExtra("restaurantDataFromNavDrawerClick", restaurant)
                        startActivity(intent)
                    }
                }
            } else if (document["userRestaurant"].toString().contains("undecided")) {
                Toast.makeText(
                    this,
                    translate(
                        english = "You haven't decided on a restaurant yet!",
                        spanish = "Aún no te has decidido por un restaurante!"
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // get current user info
    private fun getCurrentUser(): String? {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email.toString()
        val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val currentUserName = sharedPrefs.getString(currentUserEmail, null)
        if (!FirebaseAuth.getInstance().currentUser?.displayName.isNullOrEmpty() &&
            !FirebaseAuth.getInstance().currentUser?.displayName.toString().contains("null")
        ) {
            return FirebaseAuth.getInstance().currentUser?.displayName
        } else if (currentUserName != null) {
            return currentUserName.toString()
        }
        return ""
    }

    // reset alarm after login
    private fun alarmOnWhenLogout() {
        val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        if (isAlarmOn()) {
            editor.apply {
                putString("alarmWasOnAtLogout", "alarmWasOnAtLogout")
            }.apply()
        }

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

    // get current user from email and password auth
    private fun getCurrentUserEmail(): String? {
        if (!FirebaseAuth.getInstance().currentUser?.email.toString().isNullOrEmpty()) {
            return FirebaseAuth.getInstance().currentUser?.email
        }
        return ""
    }


    // create my notification channel for notification in alarm receiver
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

    // check if alarm is on
    private fun isAlarmOn(): Boolean {
        val intent = Intent(this, AlarmReceiver::class.java)
        if (PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_NO_CREATE
            ) != null
        ) {
            return true
        }
        return false
    }


}


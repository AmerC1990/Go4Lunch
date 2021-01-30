package com.amercosovic.go4lunch.activities


import android.content.Intent
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
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {


    var googleSignInClient: GoogleSignInClient? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private val settingsFragment = SettingsFragment()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.supportActionBar?.hide()
        setUpSearchView()

        val mapFragment = MapFragment()

        makeCurrentFragment(mapFragment)
        setupNavDrawer()
        setUpBottomNavClicks()

        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email.toString()
        val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val currentUserName = sharedPrefs.getString(currentUserEmail, null)

        if (!FirebaseAuth.getInstance().currentUser?.displayName.isNullOrEmpty() &&
            !FirebaseAuth.getInstance().currentUser?.displayName.toString().contains("null")
        ) {
            usernameTextview.text = FirebaseAuth.getInstance().currentUser?.displayName
        } else if (currentUserName != null) {
            usernameTextview.text = currentUserName.toString()
        }

        if (!FirebaseAuth.getInstance().currentUser?.email.toString().isNullOrEmpty()) {
            userEmailTextview.text = FirebaseAuth.getInstance().currentUser?.email
        } else {
            userEmailTextview.text = ""
        }

        Glide.with(currentUserImageView)
            .load(FirebaseAuth.getInstance().currentUser?.photoUrl)
            .centerCrop()
            .circleCrop()
            .placeholder(R.drawable.defaultprofilepicture)
            .error(R.drawable.defaultprofilepicture)
            .into(currentUserImageView)
    }

    fun update(view: View) {
        when (view.id) {
            R.id.yourLunchButton -> {
                val restaurantReference = firestore.collection("users")
                    .document(FirebaseAuth.getInstance().currentUser?.displayName.toString())
                restaurantReference.get().addOnSuccessListener { document ->
                    if (!document["userRestaurant"].toString().contains("undecided")) {
                        val data = document["userRestaurantData"].toString()
                        val intent = Intent(this, RestaurantDetailsActivity::class.java)
                        intent.putExtra("restaurantDataFromNavDrawerClick", data)
                        startActivity(intent)
                    } else if (document["userRestaurant"].toString().contains("undecided")) {
                        Toast.makeText(
                            this,
                            "You haven't decided on a restaurant yet!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            }
            R.id.settingsButton -> makeCurrentFragment(settingsFragment)
            R.id.logoutButton -> {
                lifecycleScope.launch(IO) {
                    logout()
                    withContext(Main) {
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

    private fun makeCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer, fragment)
            commit()
        }
    }

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

    private fun setupNavDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this, mainActivityDrawerLayout, toolbar, R.string.open,
            R.string.close
        )
        drawerToggle?.let {
            it.syncState()
        }
    }

    private fun setUpBottomNavClicks() {
        val mapFragment = MapFragment()
        val listViewFragment = RestaurantListFragment()
        val workmatesFragment = WorkmatesFragment()
        bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.ic_mapview -> makeCurrentFragment(mapFragment)
                R.id.ic_listview -> makeCurrentFragment(listViewFragment)
                R.id.ic_workmates -> makeCurrentFragment(workmatesFragment)
            }
            true
        }
    }

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
}


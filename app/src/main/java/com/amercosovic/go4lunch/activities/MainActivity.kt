package com.amercosovic.go4lunch.activities


import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.fragments.*
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    var googleSignInClient: GoogleSignInClient? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private val yourLunchFragment = YourLunchFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.supportActionBar?.hide()
        searchView.setLayoutParams(androidx.appcompat.widget.Toolbar.LayoutParams(Gravity.RIGHT))
        searchView.maxWidth = Int.MAX_VALUE
        val mapFragment = MapFragment()
        val listViewFragment = RestaurantListFragment()
        val workmatesFragment = WorkmatesFragment()

            makeCurrentFragment(mapFragment)

            bottomNavigation.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.ic_mapview -> makeCurrentFragment(mapFragment)
                    R.id.ic_listview -> makeCurrentFragment(listViewFragment)
                    R.id.ic_workmates -> makeCurrentFragment(workmatesFragment)
                }
                true
            }
            drawerToggle = ActionBarDrawerToggle(
                this, mainActivityDrawerLayout, toolbar, R.string.open,
                R.string.close
            )
            drawerToggle?.let {
                it.syncState()
            }
    }

    fun update(view: View) { 
        when (view.id) {
            R.id.yourLunchButton -> makeCurrentFragment(yourLunchFragment)
            R.id.settingsButton -> makeCurrentFragment(settingsFragment)
            R.id.logoutButton -> {
                CoroutineScope(IO).launch {
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
        var googleSignIn = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this@MainActivity, googleSignIn)
        FirebaseAuth.getInstance().signOut()
        LoginManager.getInstance().logOut()
        googleSignInClient?.signOut()
    }


}


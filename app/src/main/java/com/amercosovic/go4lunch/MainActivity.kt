package com.amercosovic.go4lunch


import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.amercosovic.go4lunch.fragments.*
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    var googleSignInClient: GoogleSignInClient? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private val yourLunchFragment = YourLunchFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main)

            this.supportActionBar?.hide()
            fl_wrapper.layoutParams = LinearLayout.LayoutParams(720, 1225)
            val mapFragment = MapFragment()
            val listViewFragment = ListViewFragment()
            val workmatesFragment = WorkmatesFragment()
            val loginFragment = LoginFragment()

            makeCurrentFragment(loginFragment)
            drawer_layout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

            bottom_navigation.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.ic_mapview -> makeCurrentFragment(mapFragment)
                    R.id.ic_listview -> makeCurrentFragment(listViewFragment)
                    R.id.ic_workmates -> makeCurrentFragment(workmatesFragment)
                }
                true
            }
            mDrawerToggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.open,
                R.string.close)
            mDrawerToggle!!.syncState()
    }

    fun update(view: View) { 
        when (view.id) {
            R.id.btn_your_lunch -> makeCurrentFragment(yourLunchFragment)
            R.id.btn_settings -> makeCurrentFragment(settingsFragment)
            R.id.btn_logout -> {
                CoroutineScope(IO).launch {
                    var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    googleSignInClient = GoogleSignIn.getClient(this@MainActivity, gso)
                    FirebaseAuth.getInstance().signOut()
                    LoginManager.getInstance().logOut()
                    googleSignInClient?.signOut()
                    withContext(Main) {
                        supportFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_out_down, R.anim.slide_in_down)
                            .replace(R.id.fl_wrapper, LoginFragment())
                            .addToBackStack(null)
                            .commit()
                }
            }
        }
    }
        drawer_layout!!.closeDrawer(Gravity.LEFT)
    }


    private fun makeCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fl_wrapper, fragment)
            commit()
        }
    }


}


package com.amercosovic.go4lunch.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.amercosovic.go4lunch.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder.encode
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginFragment : Fragment() {

    var googleSignInClient: GoogleSignInClient? = null
    var RC_SIGN_IN = 1000
    private var callbackManager = CallbackManager.Factory.create()
    private val LOCATION_PERMISSION_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val layoutInflater = inflater.inflate(R.layout.fragment_login, container, false)
        val navDrawer: DrawerLayout = activity!!.drawer_layout
        navDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val bottomNavBar: BottomNavigationView = activity!!.bottom_navigation
        bottomNavBar.visibility = View.GONE
        val flWrapper: FrameLayout = activity!!.fl_wrapper
        flWrapper.layoutParams =
        LinearLayout.LayoutParams(720, LinearLayout.LayoutParams.MATCH_PARENT)
        val actionBar: android.app.ActionBar? = activity!!.actionBar
        actionBar?.hide()
        activity!!.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        return layoutInflater
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getLocationPermission()
        val context = activity as AppCompatActivity
        val animation = AnimationUtils.loadAnimation(context, R.anim.scale_up)

        google_login_button_fr.setOnClickListener {
            google_login_button_fr.startAnimation(animation)
            CoroutineScope(IO).launch {
                googleLogin()
            }

        }

        facebook_login_button_fr.setOnClickListener {
            facebook_login_button_fr.startAnimation(animation)
            CoroutineScope(IO).launch {
                facebookLogin()
            }
        }
    }

    private fun googleLogin() {
        CoroutineScope(IO).launch {
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("827127043040-c3ai70jbjcb2ff7l28c2a78b5or44t4c.apps.googleusercontent.com")
            .requestEmail()
            .build()
        val context = activity as AppCompatActivity
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        var signInIntent = googleSignInClient?.signInIntent
        withContext(Main) {
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }
    }

    private suspend fun facebookLogin() {
        LoginManager.getInstance().loginBehavior = LoginBehavior.WEB_ONLY
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("public_profile", "email"))
        LoginManager.getInstance().registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                CoroutineScope(IO).launch {
                    firebaseAuthFacebook(result)
                }
            }
            override fun onCancel() {
            }
            override fun onError(error: FacebookException?) {
            }
        })
    }

    private suspend fun firebaseAuthFacebook(result: LoginResult?) {
        val credential = FacebookAuthProvider.getCredential(result?.accessToken?.token!!)
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                CoroutineScope(IO).launch {
                    withContext(Main) {
                        val context = activity as AppCompatActivity
                        context.supportFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
                            .replace(R.id.fl_wrapper, MapFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
            else {
                Log.d("fberror", "signInWithCredential:failure", task.exception)
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?) {
        CoroutineScope(IO).launch {
            val credential = GoogleAuthProvider.getCredential(acct?.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    CoroutineScope(IO).launch {
                        withContext(Main) {
                            val context = activity as AppCompatActivity
                            context.supportFragmentManager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out,
                                 R.anim.slide_out_down, R.anim.slide_in_down)
                                .replace(R.id.fl_wrapper, MapFragment())
                                .addToBackStack("fragmentLogin")
                                .commit()
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                CoroutineScope(IO).launch {
                    getGoogleAccountAfterResult(data)
                }
            }
        }
    }

    private suspend fun getGoogleAccountAfterResult(data: Intent?) {
        CoroutineScope(IO).launch {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        }
    }

    private fun getLocationPermission() {
        val context = activity as AppCompatActivity
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

}


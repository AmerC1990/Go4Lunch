package com.amercosovic.go4lunch.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.amercosovic.go4lunch.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    var googleSignInClient: GoogleSignInClient? = null
    var SIGN_IN_REQUESTCODE = 1000
    private var callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        this.supportActionBar?.hide()
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val sharedPrefs = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.clear()
        editor.apply()
        val animation = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        googleLoginButton.setOnClickListener {
            googleLoginButton.startAnimation(animation)
            lifecycleScope.launch(IO) {
                googleLogin()
            }
        }

        facebookLoginButton.setOnClickListener {
            facebookLoginButton.startAnimation(animation)
            lifecycleScope.launch(IO) {
                facebookLogin()
            }
        }
    }

    private suspend fun googleLogin() {
        lifecycleScope.launch(IO) {
            val googleSignIn = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("827127043040-c3ai70jbjcb2ff7l28c2a78b5or44t4c.apps.googleusercontent.com")
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this@LoginActivity, googleSignIn)
            val signInIntent = googleSignInClient?.signInIntent
            withContext(Main) {
                startActivityForResult(signInIntent, SIGN_IN_REQUESTCODE)
            }
        }
    }

    private suspend fun facebookLogin() {
        val loginManager = LoginManager.getInstance()
        loginManager.apply {
            loginBehavior = LoginBehavior.WEB_ONLY
            logInWithReadPermissions(this@LoginActivity, listOf("public_profile", "email"))
            registerCallback(callbackManager, object :
                FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult?) {
                    lifecycleScope.launch(IO) {
                        firebaseAuthFacebook(result)
                    }
                }

                override fun onCancel() {
                }

                override fun onError(error: FacebookException?) {
                }
            })
        }
    }

    private suspend fun firebaseAuthFacebook(result: LoginResult?) {
        lifecycleScope.launch(IO) {
            val credential =
                result?.accessToken?.token?.let { FacebookAuthProvider.getCredential(it) }
            withContext(Main) {
                if (credential != null) {
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                overridePendingTransition(
                                    R.anim.slide_out_down,
                                    R.anim.slide_in_down
                                )
                                startActivity(intent)
                            } else {
                                Log.d("fbError", "signInWithCredential:failure", task.exception)
                            }
                        }
                }
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?) {
        lifecycleScope.launch(IO) {
            val credential = GoogleAuthProvider.getCredential(acct?.idToken, null)
            withContext(Main) {
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            overridePendingTransition(R.anim.slide_out_down, R.anim.slide_in_down)
                            startActivity(intent)
                        }
                    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_REQUESTCODE) {
            if (resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch(IO) {
                    getGoogleAccountAfterResult(data)
                }
            }
        }
    }

    private suspend fun getGoogleAccountAfterResult(data: Intent?) {
        lifecycleScope.launch(IO) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        }
    }
}

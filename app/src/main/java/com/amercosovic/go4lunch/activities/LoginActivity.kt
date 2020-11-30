package com.amercosovic.go4lunch.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    var googleSignInClient: GoogleSignInClient? = null
    var RC_SIGN_IN = 1000
    private var callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        this.supportActionBar?.hide()
        this.window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

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

    private fun googleLogin() {
        lifecycleScope.launch(IO) {
            var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("827127043040-c3ai70jbjcb2ff7l28c2a78b5or44t4c.apps.googleusercontent.com")
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this@LoginActivity, gso)
            var signInIntent = googleSignInClient?.signInIntent
            withContext(Main) {
                startActivityForResult(signInIntent, RC_SIGN_IN)
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
            val credential = FacebookAuthProvider.getCredential(result?.accessToken?.token!!)
            withContext(Main) {
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            overridePendingTransition(R.anim.slide_out_down, R.anim.slide_in_down)
                            startActivity(intent)
                        } else {
                            Log.d("fbError", "signInWithCredential:failure", task.exception)
                    }
                }
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?) {
        lifecycleScope.launch(IO) {
            val credential = GoogleAuthProvider.getCredential(acct?.idToken, null)
            withContext(Main) {
            FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { task ->
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
        if (requestCode == RC_SIGN_IN) {
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

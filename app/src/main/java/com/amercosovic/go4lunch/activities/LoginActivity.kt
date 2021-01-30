package com.amercosovic.go4lunch.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_restaurant_details.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    var googleSignInClient: GoogleSignInClient? = null
    var SIGN_IN_REQUESTCODE = 1000
    private var callbackManager = CallbackManager.Factory.create()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        this.supportActionBar?.hide()
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        clearInputs()

        val animation = AnimationUtils.loadAnimation(this, R.anim.scale_up)


        registerButton.setOnClickListener {
            registerButton.startAnimation(animation)
            val userName = editTextUsername.text.toString()
            val email = editTextEmailAddress.text.toString()
            val password = editTextPassword.text.toString()

            when {
                (!userName.isNullOrEmpty() && !email.isNullOrEmpty() && !password.isNullOrEmpty()) -> {
                    registerUser(email = email, userName = userName, password = password)
                }
            }
            when {
                (editTextUsername.visibility == View.VISIBLE && userName.isNullOrEmpty() ||
                        editTextUsername.visibility == View.VISIBLE && email.isNullOrEmpty() ||
                        editTextUsername.visibility == View.VISIBLE && password.isNullOrEmpty()) -> {
                    Toast.makeText(
                        this,
                        "Please complete missing fields to create account",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            when {
                (editTextUsername.visibility == View.GONE) -> {
                    editTextUsername.visibility = View.VISIBLE
                }
            }
        }

        loginButton.setOnClickListener {
            loginButton.startAnimation(animation)
            val email = editTextEmailAddress.text.toString()
            val password = editTextPassword.text.toString()

            if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
                signIn(email = email, password = password)
            } else {
                Toast.makeText(
                    this,
                    "Please complete missing fields to create account",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

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
                                addFirestoreDocIfDoesntExist()
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
                            addFirestoreDocIfDoesntExist()
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

    private fun addFirestoreDocIfDoesntExist() {
        val userName: String = FirebaseAuth.getInstance().currentUser?.displayName.toString()
        val docRef = firestore.collection("users").document(userName)
        docRef.get().addOnSuccessListener { document ->
            if (document?.data.isNullOrEmpty()) {
                addUserToFireStore(userName)
            }
        }
    }

    private fun addUserFromEmailAndPasswordLogin(userName: String) {
        val docRef = firestore.collection("users").document(userName)
        docRef.get().addOnSuccessListener { document ->
            if (document?.data.isNullOrEmpty()) {
                addUserToFireStore(userName)
            }
        }
    }

    private fun addUserToFireStore(userName: String) {
        val user = hashMapOf(
            "userName" to userName,
            "userImage" to FirebaseAuth.getInstance().currentUser?.photoUrl.toString(),
            "userRestaurant" to "undecided"
        )

        firestore.collection("users").document(userName)
            .set(user as Map<String, Any>).addOnSuccessListener { documentReference ->
            }
            .addOnFailureListener { exception ->
                Log.e("Error", exception.message)
            }
    }

    private fun registerUser(userName: String, email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            when {
                it.isSuccessful -> {
                    val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
                    val editor = sharedPrefs.edit()
                    editor.apply {
                        putString(email, userName)
                    }.apply()
                    Toast.makeText(this, "User Registration success", Toast.LENGTH_LONG).show()
                    clearInputs()
                    editTextUsername.visibility = View.GONE
                    Log.d(
                        "theCurrentUserEmail",
                        FirebaseAuth.getInstance().currentUser?.email.toString()
                    )

                }
                else -> {
                    Toast.makeText(this, "User Registration failed", Toast.LENGTH_LONG).show()
                }
            }

        }
    }

    private fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            when {
                it.isSuccessful -> {
                    Toast.makeText(this, "User Login success", Toast.LENGTH_LONG).show()
                    val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
                    val userName = sharedPrefs.getString(email, null)
                    if (userName != null) {
                        addUserFromEmailAndPasswordLogin(userName)
                        val editor = sharedPrefs.edit()
                        editor.apply {
                            putString(email, userName)
                        }.apply()
                    }
                    Log.d(
                        "theCurrentUserEmail",
                        FirebaseAuth.getInstance().currentUser?.email.toString()
                    )
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    overridePendingTransition(
                        R.anim.slide_out_down,
                        R.anim.slide_in_down
                    )
                    startActivity(intent)
                }
                else -> {
                    Toast.makeText(this, "User Login failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun clearInputs() {
        editTextUsername.text.clear()
        editTextPassword.text.clear()
    }
}

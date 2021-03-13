package com.amercosovic.go4lunch.activities

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.receiver.AlarmReceiver
import com.amercosovic.go4lunch.utility.Translate.translate
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
import java.time.Duration
import java.time.LocalTime
import java.util.*
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    var googleSignInClient: GoogleSignInClient? = null
    var SIGN_IN_REQUESTCODE = 1000
    private var callbackManager = CallbackManager.Factory.create()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var alarmManager: AlarmManager
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    private val channelId = "lunch"
    private val description = "notification"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // hide action bar
        this.supportActionBar?.hide()
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        // create notification channel for alarm receiver
        createNotificationChannel()
        // logout, clear inputs of sign in edit texts
        logout()
        // clear username and password edit texts
        clearInputs()

        // initialize animation object
        val animation = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        // create new user account
        registerButton.setOnClickListener {
            registerButton.startAnimation(animation)
            handleEditTextClicksRegister()
        }

        //login user
        loginButton.setOnClickListener {
            loginButton.startAnimation(animation)
            handleEditTextClicksLogin()
        }

        // login with google
        googleLoginButton.setOnClickListener {
            googleLoginButton.startAnimation(animation)
            lifecycleScope.launch(IO) {
                googleLogin()
            }
        }

        // login with facebook
        facebookLoginButton.setOnClickListener {
            facebookLoginButton.startAnimation(animation)
            lifecycleScope.launch(IO) {
                facebookLogin()
            }
        }
    }

    // google login
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

    //facebook login
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

    // authentication facebook login with firebase
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
                                getCurrentUser()?.let { resetAlarmAfterLogin(it) }
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

    // authenticate google login with firebase
    private suspend fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?) {
        lifecycleScope.launch(IO) {
            val credential = GoogleAuthProvider.getCredential(acct?.idToken, null)
            withContext(Main) {
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            addFirestoreDocIfDoesntExist()
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            getCurrentUser()?.let { resetAlarmAfterLogin(it) }
                            overridePendingTransition(R.anim.slide_out_down, R.anim.slide_in_down)
                            startActivity(intent)
                        }
                    }
            }
        }
    }

    // get result after login
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

    // get google account after login
    private suspend fun getGoogleAccountAfterResult(data: Intent?) {
        lifecycleScope.launch(IO) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        }
    }

    // check if user exists ,add user to firestore from google or facebook login
    private fun addFirestoreDocIfDoesntExist() {
        val userName: String = FirebaseAuth.getInstance().currentUser?.displayName.toString()
        val docRef = firestore.collection("users").document(userName)
        docRef.get().addOnSuccessListener { document ->
            if (document?.data.isNullOrEmpty()) {
                addUserToFireStore(userName)
            }
        }
    }

    // check if user exists ,add user to firestore from email and password login
    private fun addUserFromEmailAndPasswordLogin(userName: String) {
        val docRef = firestore.collection("users").document(userName)
        docRef.get().addOnSuccessListener { document ->
            if (document?.data.isNullOrEmpty()) {
                addUserToFireStore(userName)
            }
        }
    }

    // add user to firestore
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

    // register user
    private fun registerUser(userName: String, email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            when {
                it.isSuccessful -> {
                    val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
                    val editor = sharedPrefs.edit()
                    editor.apply {
                        putString(email, userName)
                    }.apply()
                    Toast.makeText(
                        this,
                        translate(
                            english = R.string.User_registration_successful.toString(),
                            spanish = R.string.User_registration_successful_spanish.toString()
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                    clearInputs()
                    editTextUsername.visibility = View.GONE
                    Log.d(
                        "theCurrentUserEmail",
                        FirebaseAuth.getInstance().currentUser?.email.toString()
                    )
                }
                else -> {
                    Toast.makeText(
                        this, translate(
                            english = R.string.User_Registration_failed.toString(),
                            spanish = R.string.Error_de_registro_de_usuario.toString()
                        ), Toast.LENGTH_LONG
                    ).show()
                }
            }

        }
    }

    // sign in with email and password
    private fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            when {
                it.isSuccessful -> {
                    Toast.makeText(
                        this,
                        translate(
                            english = R.string.User_Login_success.toString(),
                            spanish = R.string.User_Login_success_spanish.toString()
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                    val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
                    val userName = sharedPrefs.getString(email, null)
                    if (userName != null) {
                        addUserFromEmailAndPasswordLogin(userName)
                        val editor = sharedPrefs.edit()
                        editor.apply {
                            putString(email, userName)
                        }.apply()
                    }
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    getCurrentUser()?.let { resetAlarmAfterLogin(it) }
                    overridePendingTransition(
                        R.anim.slide_out_down,
                        R.anim.slide_in_down
                    )
                    startActivity(intent)
                }
                else -> {
                    Toast.makeText(
                        this,
                        translate(
                            english = R.string.User_Login_failed.toString(),
                            spanish = R.string.User_Login_failed_spanish.toString()
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // clear username and password edit text inputs
    private fun clearInputs() {
        editTextUsername.text.clear()
        editTextPassword.text.clear()
    }

    // logout
    private fun logout() {
        val googleSignIn = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this@LoginActivity, googleSignIn)
        FirebaseAuth.getInstance().signOut()
        LoginManager.getInstance().logOut()
        googleSignInClient?.signOut()
        val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.apply {
            remove(FirebaseAuth.getInstance().currentUser?.email.toString())
        }.apply()
    }

    // reset alarm after login
    private fun resetAlarmAfterLogin(user: String) {
        val sharedPrefs = this.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val wasAlarmOn = sharedPrefs.getString("alarmWasOnAtLogout", null)
        if (wasAlarmOn != null) {
            if (user.toString() != "") {
                val restaurantReference = firestore.collection("users")
                    .document(user)
                restaurantReference.get().addOnSuccessListener { document ->
                    if (!document["userRestaurant"].toString().contains("undecided")) {
                        resetAlarm()
                        editor.apply {
                            remove("alarmWasOnAtLogout")
                        }.apply()
                    }
                }
            }
        }
    }

    // get current user info
    private fun getCurrentUser(): String? {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email.toString()
        val sharedPrefs = this.getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
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

    // reset alarm
    private fun resetAlarm() {
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
            System.currentTimeMillis() + timeUntilNoonInMillis,
            pendingIntent2
        )
        alarmManager.setAlarmClock(myAlarm, pendingIntent)
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

    // handle edit text clicks when registering
    private fun handleEditTextClicksRegister() {
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
                    translate(
                        english = R.string.missing_fields_reminder.toString(),
                        spanish = R.string.missing_fields_reminder_spanish.toString()
                    ),
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

    // handle edit text clicks when logging in
    private fun handleEditTextClicksLogin() {
        val email = editTextEmailAddress.text.toString()
        val password = editTextPassword.text.toString()

        if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
            signIn(email = email, password = password)
        } else {
            Toast.makeText(
                this,
                translate(
                    english = R.string.missing_fields_reminder.toString(),
                    spanish = R.string.missing_fields_reminder_spanish.toString()
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

}



package com.amercosovic.go4lunch.receiver

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.activities.RestaurantDetailsActivity
import com.amercosovic.go4lunch.model.Restaurant
import com.amercosovic.go4lunch.utility.RestaurantFromFirestore
import com.amercosovic.go4lunch.utility.Translate.translate
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    private lateinit var alarmManager: AlarmManager

    override fun onReceive(context: Context, intent: Intent?) {
        // get current user
        // get data from firestore database and display notification
        // reset alarm
        getCurrentUser(context = context)?.let { getFirestoreDataDisplayNotification(it, context) }

    }

    // reset alarm
    private fun resetAlarm(context: Context) {
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntent2 =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        // 24 hours = 86400000 milliseconds
        // 15 seconds = 15000 milliseconds
        val myAlarm = AlarmManager.AlarmClockInfo(
            System.currentTimeMillis() + 15000,
            pendingIntent2
        )
        alarmManager.setAlarmClock(myAlarm, pendingIntent)
    }

    // get firestore data and display the notification
    private fun getFirestoreDataDisplayNotification(currentUser: String, context: Context) {
        val sharedPrefs =
            context.getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
        val firestore = FirebaseFirestore.getInstance()
        val reference = firestore.collection("users").document(currentUser)
        reference.get().addOnSuccessListener { document ->
            if (!document["userRestaurant"].toString().isNullOrEmpty()) {
                val nameOfRestaurant = document["userRestaurant"]
                val addressOfRestaurant = sharedPrefs?.getString("restaurantAddress", null)
                val allUsers = firestore.collection("users")
                val usersAtCurrentRestaurant: MutableList<String> = mutableListOf()
                allUsers.whereEqualTo("userRestaurant", nameOfRestaurant)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            if (document["userRestaurant"] == nameOfRestaurant) {
                                val user = document.id
                                usersAtCurrentRestaurant.add(user)
                            }
                        }
                        val data = document["userRestaurantData"].toString()
                        var allWorkmatesJoining = usersAtCurrentRestaurant.toString()
                            .replace("[", "").replace("]", "").replace(", $currentUser", "")
                            .replace(currentUser, "").removePrefix(",")
                        if (!allWorkmatesJoining.toString().isNullOrEmpty()) {
                            allWorkmatesJoining = ",  with: $allWorkmatesJoining"
                        }
                        val notificationIntent =
                            Intent(context, RestaurantDetailsActivity::class.java)
                        val mapper = jacksonObjectMapper()
                        CoroutineScope(Default).launch {
                            val restaurant = RestaurantFromFirestore.getRestaurant(data)
                            withContext(Main) {
                                notificationIntent.putExtra(
                                    "restaurantDataFromNotification",
                                    restaurant
                                )
                                val contentIntent = PendingIntent.getActivity(
                                    context, 0,
                                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
                                )
                                val builder = NotificationCompat.Builder(context, "lunch")
                                    .setSmallIcon(R.drawable.myicon)
                                    .setContentTitle(
                                        translate(
                                            english = "Get ready for Lunch!",
                                            spanish = "Prepárate para el almuerzo!"
                                        )
                                    )
                                    .setStyle(
                                        NotificationCompat.BigTextStyle()
                                            .bigText(
                                                "${
                                                    translate(
                                                        english = "Today, you're eating lunch at",
                                                        spanish = "Hoy vas a almorzar en"
                                                    )
                                                } $nameOfRestaurant - $addressOfRestaurant" +
                                                        "${
                                                            translate(
                                                                english = allWorkmatesJoining,
                                                                spanish = allWorkmatesJoining.toString()
                                                                    .replace("with", "con")
                                                            )
                                                        } "
                                            )
                                    )
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setColor(Color.parseColor("#71E3B1"))

                                builder.setContentIntent(contentIntent)
                                builder.setDefaults(Notification.DEFAULT_SOUND)
                                builder.setAutoCancel(true)
                                val notificationManager =
                                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                notificationManager.notify(200, builder.build())
                                resetAlarm(context)
                            }
                        }
                    }

            }
        }
    }

    // get current user
    private fun getCurrentUser(context: Context): String? {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email.toString()
        val sharedPrefs =
            context.getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
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
}
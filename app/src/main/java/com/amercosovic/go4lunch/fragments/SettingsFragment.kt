package com.amercosovic.go4lunch.fragments

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.receiver.AlarmReceiver
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_settings.*
import java.time.Duration
import java.time.LocalTime
import java.util.*
import java.util.concurrent.TimeUnit


class SettingsFragment : Fragment() {

    private lateinit var alarmManager: AlarmManager
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    private val channelId = "lunch"
    private val description = "notification"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onStart() {
        super.onStart()
        createNotificationChannel()
        // set up search view
        val searchView = activity?.searchView
        searchView?.visibility = View.GONE
        // display toggle button accordingly - depending on if alarm is on or off
        if (isAlarmOn()) {
            configureNotificationSwitch.isChecked = true
        } else if (!isAlarmOn()) {
            configureNotificationSwitch.isChecked = false
        }
        // turn alarm on/off on toggle button / switch click
        configureNotificationSwitch.setOnClickListener {
            if (configureNotificationSwitch.isChecked) {
                // turn on alarm
                setAlarm()
                Toast.makeText(
                    context, translate(
                        english = "Notifications have been turned on!",
                        spanish = "Se han activado las notificaciones!"
                    ), Toast.LENGTH_LONG
                ).show()
            } else if (!configureNotificationSwitch.isChecked) {
                // cancel alarm
                cancelAlarm()
                Toast.makeText(
                    context, translate(
                        english = "Notifications have been turned off!",
                        spanish = "Se han desactivado las notificaciones!"
                    ), Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // check if alarm is currently on
    private fun isAlarmOn(): Boolean {
        val intent = Intent(context, AlarmReceiver::class.java)
        if (PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_NO_CREATE
            ) != null
        ) {
            return true
        }
        return false
    }

    // create my notification channel for notification in broadcast receiver
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(
                channelId, description, NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.BLACK
            notificationChannel.enableVibration(false)

            notificationManager =
                activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    // set alarm
    private fun setAlarm() {
        alarmManager = activity?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntent2 =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        // 15 seconds = 15000 milliseconds
        val timeUntilNoon = Duration.between(LocalTime.now(), LocalTime.NOON).seconds
        val timeUntilNoonInMillis = TimeUnit.SECONDS.toMillis(timeUntilNoon.toLong())
        val myAlarm = AlarmManager.AlarmClockInfo(
            System.currentTimeMillis() + timeUntilNoonInMillis,
            pendingIntent2
        )
        alarmManager.setAlarmClock(myAlarm, pendingIntent)
    }

    // cancel alarm
    private fun cancelAlarm() {
        alarmManager = activity?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // translate
    private fun translate(spanish: String, english: String): String {
        val language = Locale.getDefault().displayLanguage

        return if (language.toString() == "español") {
            return spanish
        } else {
            return english
        }
    }


}
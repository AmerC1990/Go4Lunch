package com.amercosovic.go4lunch.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.amercosovic.go4lunch.R
import com.amercosovic.go4lunch.utility.Translate
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

open class BaseFragment : Fragment() {

    // check permission and get location
    fun checkPermissionAndGetLocation(): Task<Location>? {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.create()
            locationRequest.interval = 60000
            locationRequest.fastestInterval = 5000
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            return LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation
        }
        return null
    }

    // check is location on
    fun isLocationOn() {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage(R.string.location_disabled_message.toString())
                .setCancelable(false)
                .setPositiveButton(
                    Translate.translate(
                        spanish = R.string.Si.toString(),
                        english = R.string.Yes.toString()
                    )
                ) { dialog, id ->
                    startActivityForResult(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 11
                    )
                }
            val alert: AlertDialog = builder.create()
            alert.show()
        }
    }
}
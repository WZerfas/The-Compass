package com.cs407.the_compass.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

/**
 * A utility class for fetching the current location.
 **/
class CurrentLocation(
    private val activity: Activity,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    private var locationCallback: LocationCallback? = null

    interface LocationResultCallback {
        fun onLocationRetrieved(latitude: Double, longitude: Double)
        fun onError(message: String)
    }

    /**
     * Checks for location permissions and attempts to fetch the location.
     **/
    fun checkPermissionsAndFetchLocation(
        permissionLauncher: ActivityResultLauncher<String>,
        callback: LocationResultCallback
    ) {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocation(callback)
        } else {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (shouldShowRationale) {
                // Show rationale to the user
                callback.onError("Location permission is needed to fetch your current location.")
            } else {
                // Request user permission
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * Fetches the last known location or requests a new location update.
     **/
    fun fetchLocation(callback: LocationResultCallback) {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            callback.onError("Location permission not granted.")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                activity.runOnUiThread {
                    callback.onLocationRetrieved(location.latitude, location.longitude)
                }
            } else {
                requestNewLocation(callback)
            }
        }.addOnFailureListener { exception ->
            callback.onError("Failed to get location: ${exception.message}")
        }
    }

    /**
     * Requests a new location update if the last known location is unavailable.
     **/
    private fun requestNewLocation(callback: LocationResultCallback) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                stopLocationUpdates()
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.locations.first()
                    activity.runOnUiThread {
                        callback.onLocationRetrieved(location.latitude, location.longitude)
                    }
                } else {
                    callback.onError("Unable to retrieve location.")
                }
            }
        }

        try {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                callback.onError("Location permission not granted.")
                return
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                activity.mainLooper
            )
        } catch (e: SecurityException) {
            callback.onError("Location permission not granted.")
        } catch (e: Exception) {
            callback.onError("An unexpected error occurred: ${e.message}")
        }
    }

    /**
     * Stops location updates to prevent memory leaks.
     **/
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }
}

package com.cs407.the_compass.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority

class CurrentLocation(
    private val activity: Activity,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    private lateinit var locationCallback: LocationCallback

    interface LocationResultCallback {
        fun onLocationRetrieved(latitude: Double, longitude: Double)
        fun onError(message: String)
    }

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
            if (!shouldShowRationale) {
                // Permission denied with 'Don't Ask Again'
                callback.onError("Location permission denied.\nPlease enable it in app settings.")
            } else {
                // Request user permission
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }


    fun fetchLocation(callback: LocationResultCallback) {
        if (ActivityCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            callback.onError("Location permission not granted")
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null){
                callback.onLocationRetrieved(location.latitude,location.longitude)
            }else{
                requestNewLocation(callback)
            }
        }.addOnFailureListener { exception ->
            callback.onError("Failed to get location: ${exception.message}")
        }
    }

    private fun requestNewLocation(callback: LocationResultCallback) {
        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = 10000 // 10 seconds
            fastestInterval = 5000
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                if (locationResult.locations.isNotEmpty()){
                    val location = locationResult.locations.first()
                    callback.onLocationRetrieved(location.latitude,location.longitude)
                }else{
                    callback.onError("Location data not available")
                }
            }
        }

        try {
            if (ActivityCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                callback.onError("Location permission not granted")
                return
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,activity.mainLooper)
        } catch (e: SecurityException) {
            callback.onError("Permission denied or location not available")
        }
    }
}

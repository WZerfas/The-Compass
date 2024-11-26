package com.cs407.the_compass

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class NavigationActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var arrowView: ImageView
    private lateinit var distanceTextView: TextView
    private var destinationLat = 0.0
    private var destinationLon = 0.0
    private var currentAzimuth = 0f
    private var bearingToDestination = 0f
    private lateinit var sensorManager: SensorManager
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    private var previousAngle = 0f

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val btnHome = findViewById<ImageView>(R.id.btnHome)
        val btnSetting = findViewById<ImageView>(R.id.btnSetting)
        val btnSearch = findViewById<ImageView>(R.id.btnSearch)

        arrowView = findViewById(R.id.imageView)
        distanceTextView = findViewById(R.id.distanceTextView)

        destinationLat = intent.getDoubleExtra("latitude", 0.0)
        destinationLon = intent.getDoubleExtra("longitude", 0.0)

        if (destinationLat != 0.0 && destinationLon != 0.0) {
            initializeSensors()
            initializeLocationUpdates()
        } else {
            Toast.makeText(this, "Invalid destination coordinates.", Toast.LENGTH_SHORT).show()
        }

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnSetting.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        btnSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI
        )
    }

    private fun initializeLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
            return
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(currentLocation: Location) {
                val destinationLocation = Location("Destination")
                destinationLocation.latitude = destinationLat
                destinationLocation.longitude = destinationLon

                val distance = currentLocation.distanceTo(destinationLocation) // in meters
                bearingToDestination = currentLocation.bearingTo(destinationLocation) // in degrees

                // Update UI
                distanceTextView.text = "Distance: ${distance.toInt()} m"
                updateArrow()
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // Request location updates
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            1f,   // Minimum distance in meters
            locationListener
        )
    }

    private fun updateArrow() {
        val targetAngle = (bearingToDestination - currentAzimuth + 360) % 360

        var angleDifference = targetAngle - previousAngle
        angleDifference = ((angleDifference + 540) % 360) - 180 // We normalize to -180 - 180

        val newAngle = previousAngle + angleDifference

        // Logging
        Log.d("NavigationActivity", "currentAzimuth: $currentAzimuth")
        Log.d("NavigationActivity", "bearingToDestination: $bearingToDestination")
        Log.d("NavigationActivity", "targetAngle: $targetAngle")
        Log.d("NavigationActivity", "angleDifference: $angleDifference")
        Log.d("NavigationActivity", "newAngle: $newAngle")

        arrowView.rotation = newAngle
        previousAngle = newAngle % 360
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> gravity = event.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.clone()
        }

        if (gravity != null && geomagnetic != null) {
            val R = FloatArray(9)
            val I = FloatArray(9)
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                val azimuthInRadians = orientation[0]
                val newAzimuth = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                val normalizedAzimuth = (newAzimuth + 360) % 360

                // Smoothing factor
                val smoothFactor = 0.1f
                currentAzimuth = currentAzimuth + smoothFactor * ((normalizedAzimuth - currentAzimuth + 540) % 360 -180)
                currentAzimuth = (currentAzimuth + 360) % 360
                updateArrow()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission was granted, initialize location updates
                initializeLocationUpdates()
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Permission denied. Unable to get location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initializeSensors()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initializeLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        if (::locationManager.isInitialized && ::locationListener.isInitialized) {
            locationManager.removeUpdates(locationListener)
        }
    }
}

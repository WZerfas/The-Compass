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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class NavigationActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var arrowView: ImageView
    private lateinit var distanceTextView: TextView
    private var destinationLat = Double.NaN
    private var destinationLon = Double.NaN
    private var currentAzimuth = 0f
    private var bearingToDestination = 0f
    private lateinit var sensorManager: SensorManager
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    private var previousAngle = 0f
    private val arrivalBuffer = 10 // in meters

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private var haveSensorData = false
    private var haveLocationData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val btnHome = findViewById<ImageView>(R.id.btnHome)
        val btnSetting = findViewById<ImageView>(R.id.btnSetting)
        val btnSearch = findViewById<ImageView>(R.id.btnSearch)
        val btnEnd = findViewById<ImageView>(R.id.btnEnd)

        arrowView = findViewById(R.id.imageView)
        distanceTextView = findViewById(R.id.distanceTextView)

        // Load destination from SharedPreferences
        loadDestination()

        processIntentData(intent)

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

        btnEnd.setOnClickListener {
            endNavigation()
        }
    }

    private fun processIntentData(intent: Intent) {
        val newLat = intent.getDoubleExtra("latitude", Double.NaN)
        val newLon = intent.getDoubleExtra("longitude", Double.NaN)
        Log.d("NavigationActivity","Received coordinates: latitude=$newLat, longitude=$newLon")

        if (!newLat.isNaN() && !newLon.isNaN()) {
            // Update destination coordinates
            destinationLat = newLat
            destinationLon = newLon
            // Save to SharedPreferences
            saveDestination(destinationLat, destinationLon)

            //Reset data and sensors
            haveSensorData = false
            haveLocationData = false

            //Restart sensors and location updates
            initializeSensors()
            initializeLocationUpdates()
        } else if (destinationLat.isNaN() || destinationLon.isNaN()) {
            // No coordinates provided and no stored destination
            Toast.makeText(this, "No destination selected. Please search for a destination.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDestination(lat: Double, lon: Double) {
        val prefs = getSharedPreferences("navigation_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putFloat("dest_lat", lat.toFloat())
        editor.putFloat("dest_lon", lon.toFloat())
        editor.apply()
    }

    private fun loadDestination() {
        val prefs = getSharedPreferences("navigation_prefs", Context.MODE_PRIVATE)
        destinationLat = prefs.getFloat("dest_lat", Float.NaN).toDouble()
        destinationLon = prefs.getFloat("dest_lon", Float.NaN).toDouble()
    }

    private fun clearDestination() {
        val prefs = getSharedPreferences("navigation_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove("dest_lat")
        editor.remove("dest_lon")
        editor.apply()
        destinationLat = Double.NaN
        destinationLon = Double.NaN
    }

    private fun endNavigation() {
        clearDestination()
        Toast.makeText(this, "Navigation ended.", Toast.LENGTH_SHORT).show()
        distanceTextView.text = "Distance: -- m"
        arrowView.rotation = 0f

        haveSensorData = false
        haveLocationData = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntentData(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!destinationLat.isNaN()&& !destinationLon.isNaN()){
            initializeSensors()
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

    private fun initializeSensors() {
        if (!::sensorManager.isInitialized) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
        sensorManager.unregisterListener(this)
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
        if (!::locationManager.isInitialized) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(currentLocation: Location) {
                if (!destinationLat.isNaN() && !destinationLon.isNaN()) {
                    val destinationLocation = Location("Destination")
                    destinationLocation.latitude = destinationLat
                    destinationLocation.longitude = destinationLon

                    val distance = currentLocation.distanceTo(destinationLocation) // in meters
                    bearingToDestination = currentLocation.bearingTo(destinationLocation) // in degrees

                    haveLocationData = true

                    // Update UI
                    distanceTextView.text = "Distance: ${distance.toInt()} m"

                    if (haveSensorData && haveLocationData) {
                        updateArrow()
                    }

                    // Check if the user has arrived
                    if (distance <= arrivalBuffer){
                        arrivedAtDestination()
                    }
                } else {
                    distanceTextView.text = "Distance: -- m"
                    arrowView.rotation = 0f
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            1f,   // Minimum distance in meters
            locationListener
        )

        // Get last known location
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation != null) {
            locationListener.onLocationChanged(lastKnownLocation)
        }
    }

    private fun updateArrow() {
        val targetAngle = (bearingToDestination - currentAzimuth + 360) % 360

        var angleDifference = targetAngle - previousAngle
        angleDifference = ((angleDifference + 540) % 360) - 180 // Normalize to -180 to 180

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

    private fun arrivedAtDestination(){
        endNavigation()

        AlertDialog.Builder(this)
            .setTitle("Arrived at Destination")
            .setMessage("You have arrived at your destination")
            .setPositiveButton("OK"){dialog,_ ->
                dialog.dismiss()
            }
            .show()
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

                haveSensorData = true
                if (haveSensorData && haveLocationData) {
                    updateArrow()
                }
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
}

package com.cs407.the_compass

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class NavigationActivity : AppCompatActivity() {
    private lateinit var arrowView: ImageView
    private lateinit var distanceTextView: TextView
    private lateinit var destTextView: TextView
    private lateinit var currentLocationText: TextView
    private lateinit var navigationUpdateReceiver: BroadcastReceiver

    private var destinationLat = Double.NaN
    private var destinationLon = Double.NaN
    private var destinationName: String? = null

    private var currentAzimuth = 0f
    private var bearingToDestination = 0f
    private var arrowAngle = 0f
    private val arrivalBuffer = 10 // in meters
    private var hasValidLocation = false

    // Flag to ensure we only check arrival after receiving valid updates
    private var hasReceivedInitialUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val btnHome = findViewById<ImageView>(R.id.btnHome)
        val btnSetting = findViewById<ImageView>(R.id.btnSetting)
        val btnSearch = findViewById<ImageView>(R.id.btnSearch)
        val btnEnd = findViewById<ImageView>(R.id.btnEnd)

        arrowView = findViewById(R.id.compassView)
        distanceTextView = findViewById(R.id.distanceTextView)
        destTextView = findViewById(R.id.destTextView)
        currentLocationText = findViewById(R.id.currentLocationText)

        // Load destination from SharedPreferences
        loadDestination()
        btnEnd.visibility = if (isNavigationActive()) View.VISIBLE else View.GONE

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

        navigationUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.cs407.the_compass.UPDATE_NAVIGATION") {
                    val distance = intent.getFloatExtra("distance", 0f)
                    val direction = intent.getStringExtra("direction") ?: "--"
                    val destinationNameExtra = intent.getStringExtra("destinationName") ?: "Unknown Location"
                    bearingToDestination = intent.getFloatExtra("bearingToDestination", 0f)
                    currentAzimuth = intent.getFloatExtra("currentAzimuth", 0f)

                    val currentLat = intent.getDoubleExtra("currentLat", Double.NaN)
                    val currentLon = intent.getDoubleExtra("currentLon", Double.NaN)

                    distanceTextView.text = "Distance: ${distance.toInt()} m"
                    destTextView.text = "Navigating to: $destinationNameExtra"

                    // Convert to DMS if valid
                    if (!currentLat.isNaN() && !currentLon.isNaN()) {
                        val latDMS = convertToDMS(currentLat, true)
                        val lonDMS = convertToDMS(currentLon, false)
                        currentLocationText.text = "Current Location: $latDMS, $lonDMS"
                        hasValidLocation = true
                    } else {
                        if (!hasValidLocation) {
                            currentLocationText.text = "Fetching location..."
                        }
                    }

                    // Compute arrow angle and rotation smoothing factor
                    arrowAngle = (bearingToDestination - currentAzimuth + 360) % 360
                    rotateArrowSmoothly(arrowAngle)

                    if (distance > 0) {
                        hasReceivedInitialUpdate = true
                    }

                    if (hasReceivedInitialUpdate && distance <= arrivalBuffer) {
                        arrivedAtDestination()
                    }
                }
            }
        }
    }

    // Conversion function
    private fun convertToDMS(coordinate: Double, isLatitude: Boolean): String {
        val absolute = Math.abs(coordinate)
        val degrees = absolute.toInt()
        val minutesFull = (absolute - degrees) * 60
        val minutes = minutesFull.toInt()
        val seconds = ((minutesFull - minutes) * 60).toInt()

        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }

        return String.format("%d°%02d'%02d\"%s", degrees, minutes, seconds, direction)
    }

    private fun rotateArrowSmoothly(angle: Float) {

        val currentRotation = arrowView.rotation % 360
        val delta = ((angle - currentRotation + 540) % 360) - 180


        val smoothingFactor = 0.1f
        val newAngle = currentRotation + delta * smoothingFactor

        arrowView.animate().rotation(newAngle).setDuration(100).start()
    }

    private fun processIntentData(intent: Intent) {
        val newLat = intent.getDoubleExtra("latitude", Double.NaN)
        val newLon = intent.getDoubleExtra("longitude", Double.NaN)
        val name = intent.getStringExtra("destination_name")

        Log.d("NavigationActivity", "Received coordinates: latitude=$newLat, longitude=$newLon, name=$name")

        if (!newLat.isNaN() && !newLon.isNaN()) {
            // Update coordinates and activate navigation
            destinationLat = newLat
            destinationLon = newLon
            destinationName = name
            saveDestination(destinationLat, destinationLon, destinationName)

            // Check permissions before starting navigation
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startNavigationProcess(destinationName)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    2001
                )
            }
        } else {
            // No valid destination
            Log.d("NavigationActivity", "No valid destination provided. Navigation not active.")
        }
    }

    private fun startNavigationProcess(name: String?) {
        if (!destinationLat.isNaN() && !destinationLon.isNaN()) {
            Log.d("NavigationActivity", "Starting navigation with coords: $destinationLat, $destinationLon")
            setNavigationActive(true)
        } else {
            Log.e("NavigationActivity", "Cannot start navigation due to invalid coordinates.")
        }
    }

    private fun saveDestination(lat: Double, lon: Double, name: String?) {
        val prefs = getSharedPreferences("navigation_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putFloat("dest_lat", lat.toFloat())
        editor.putFloat("dest_lon", lon.toFloat())
        name?.let {
            editor.putString("destination_name", it)
        }
        editor.apply()
    }

    private fun loadDestination() {
        val prefs = getSharedPreferences("navigation_prefs", Context.MODE_PRIVATE)
        destinationLat = prefs.getFloat("dest_lat", Float.NaN).toDouble()
        destinationLon = prefs.getFloat("dest_lon", Float.NaN).toDouble()
        destinationName = prefs.getString("destination_name", null)
    }

    private fun clearDestination() {
        val prefs = getSharedPreferences("navigation_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove("dest_lat")
        editor.remove("dest_lon")
        editor.remove("destination_name")
        editor.apply()
        destinationLat = Double.NaN
        destinationLon = Double.NaN
        destinationName = null
    }

    private fun endNavigation() {
        clearDestination()
        setNavigationActive(false)
        Toast.makeText(this, "Navigation ended.", Toast.LENGTH_SHORT).show()
        distanceTextView.text = "Distance: -- m"
        destTextView.text = "Navigation Ended."
        currentLocationText.text = "Fetching location..."
        arrowView.rotation = 0f
        hasReceivedInitialUpdate = false
    }

    private fun setNavigationActive(active: Boolean) {
        val prefs = getSharedPreferences("navigation_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("navigation_active", active)
        editor.apply()

        val btnEnd = findViewById<ImageView>(R.id.btnEnd)
        btnEnd.visibility = if (active) View.VISIBLE else View.GONE

        if (active) {
            // Start the navigation service
            startNavigationService()
        } else {
            // Stop the navigation service
            stopNavigationService()
        }
    }

    private fun isNavigationActive(): Boolean {
        val prefs = getSharedPreferences("navigation_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("navigation_active", false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntentData(intent)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            navigationUpdateReceiver,
            IntentFilter("com.cs407.the_compass.UPDATE_NAVIGATION")
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(navigationUpdateReceiver)
    }

    private fun arrivedAtDestination() {
        endNavigation()

        AlertDialog.Builder(this)
            .setTitle("Arrived at Destination")
            .setMessage("You have arrived at your destination")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun startNavigationService() {
        val prefs = getSharedPreferences("navigation_prefs", Context.MODE_PRIVATE)
        val destinationName = prefs.getString("destination_name", null)

        val intent = Intent(this, NavigationService::class.java).apply {
            putExtra("destination_lat", destinationLat)
            putExtra("destination_lon", destinationLon)
            destinationName?.let {
                putExtra("destination_name", it)
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            Toast.makeText(this, "Location permission not granted. Cannot start navigation service.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopNavigationService() {
        val intent = Intent(this, NavigationService::class.java)
        stopService(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNavigationProcess(destinationName)
            } else {
                Toast.makeText(this, "Location permission denied. Cannot start navigation.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

package com.cs407.the_compass

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log // For logging
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.cs407.the_compass.util.CompassManager
import com.cs407.the_compass.util.CurrentLocation
import com.cs407.the_compass.util.ElevationManager
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {
    private lateinit var compassManager: CompassManager
    private lateinit var currentLocation: CurrentLocation
    private lateinit var elevationManager: ElevationManager
    private lateinit var altitudeTextView: TextView
    //private lateinit var pressureTextView: TextView
    private val fusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (!shouldShowRationale) {
                showPermissionDeniedDialog()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Location permission is required for this app to function. Please enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val compassImage = findViewById<ImageView>(R.id.compassNeedle)
        val degreeTextView = findViewById<TextView>(R.id.degreeView)
        val btnMap = findViewById<ImageView>(R.id.btnMap)
        val btnSetting = findViewById<ImageView>(R.id.btnSetting)
        val btnLog = findViewById<ImageView>(R.id.btnLog)

        altitudeTextView = findViewById(R.id.altitudeText)
        //pressureTextView = findViewById(R.id.pressureText)

        elevationManager = ElevationManager(this) { elevation, pressure ->
            if (elevation != null && pressure != null) {
                altitudeTextView.text = "Altitude: ${elevation.toInt()} m"
                //pressureTextView.text = "Pressure: ${pressure.toInt()} hPa"
            } else {
                altitudeTextView.text = "Altitude unavailable"
                //pressureTextView.text = "Pressure unavailable"
            }
        }

        compassManager = CompassManager(this){ degree, direction ->
            degreeTextView.text = "${degree.toInt()}º $direction"
            compassImage.rotation = -degree
        }
        currentLocation = CurrentLocation(this, fusedLocationProviderClient)

        btnMap.setOnClickListener {
            // Navigate directly to NavigationActivity
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }

        btnSetting.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        btnLog.setOnClickListener{
            val sharedPreferenceSet = getSharedPreferences("StoredPreferences", MODE_PRIVATE)
            val isLocationLogEnabled = sharedPreferenceSet.getBoolean("locationLogEnabled", false)
            if (isLocationLogEnabled == false){
                Toast.makeText(this, "Log function not enabled", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "Enter the log (PlaceHolder)", Toast.LENGTH_SHORT).show()
            }
        }

        // Check location permission and fetch location
        currentLocation.checkPermissionsAndFetchLocation(permissionLauncher, object: CurrentLocation.LocationResultCallback {
            override fun onLocationRetrieved(latitude: Double, longitude: Double) {
                updateLocationUI(latitude, longitude)
            }
            override fun onError(message: String) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        })

        // Initialize the database access
        val databaseAccess = DatabaseAccess.getInstance(this)

        // Open the database and log tables
        databaseAccess.open()
        databaseAccess.logTables()
        databaseAccess.close()
        // Fetch the address from the database with id = 1
        fetchAddressFromDatabase()
        //databaseAccess.close()
    }

    override fun onResume() {
        super.onResume()
        compassManager.start()
        elevationManager.startListening()
        updateNavigationIcon()
    }

    override fun onPause() {
        super.onPause()
        compassManager.stop()
        elevationManager.stopListening()
    }

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
        return String.format("%d°%02d'%02d\" %s ", degrees, minutes, seconds, direction)
    }

    private fun updateLocationUI(latitude: Double, longitude: Double) {
        val locationTextView = findViewById<TextView>(R.id.degreeText)
        val latitudeDMS = convertToDMS(latitude, true)
        val longitudeDMS = convertToDMS(longitude, false)
        locationTextView.text = "$latitudeDMS $longitudeDMS"
    }

    private fun updateNavigationIcon(){
        val prefs = getSharedPreferences("navigation_prefs",Context.MODE_PRIVATE)
        val navigatioActive = prefs.getBoolean("navigation_active",false)
        val btnMap = findViewById<ImageView>(R.id.btnMap)
        if (navigatioActive){
            btnMap.setImageResource(R.drawable.map_icon_active)
        }else{
            btnMap.setImageResource(R.drawable.map_icon)
        }
    }

    // TODO fetch the item with id = 1 in the database
    private fun fetchAddressFromDatabase() {
        val dbAccess = DatabaseAccess.getInstance(this)
        dbAccess.open()

        // Query the address for id = 1
        val address = dbAccess.getAddress(1)

        // Log the address
        Log.d("DatabaseLog", "Address for ID 1: $address")

        dbAccess.close()
    }

    private fun getCurrentLocation() {
        currentLocation.fetchLocation(object: CurrentLocation.LocationResultCallback {
            override fun onLocationRetrieved(latitude: Double, longitude: Double) {
                updateLocationUI(latitude, longitude)
            }

            override fun onError(message: String) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}

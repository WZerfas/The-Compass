package com.cs407.the_compass

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.cs407.the_compass.util.CompassManager
import com.cs407.the_compass.util.CurrentLocation
import com.cs407.the_compass.util.ElevationManager
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity(),SensorEventListener {
    private lateinit var compassManager: CompassManager
    private lateinit var currentLocation: CurrentLocation
    private lateinit var elevationManager: ElevationManager
    private lateinit var altitudeTextView: TextView
    private lateinit var pressureTextView: TextView
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

        altitudeTextView = findViewById(R.id.altitudeText)
        pressureTextView = findViewById(R.id.pressureText)

        elevationManager = ElevationManager(this){elevation, pressure ->
            if (elevation != null && pressure != null){
                altitudeTextView.text = "Altitude: ${elevation.toInt()} m"
                pressureTextView.text = "Pressure: ${pressure.toInt()} hPa"
            } else{
                altitudeTextView.text = "Altitude unavailable"
                pressureTextView.text = "Pressure unavailable"
            }
        }

        compassManager = CompassManager(this, compassImage, degreeTextView)
        currentLocation = CurrentLocation(this,fusedLocationProviderClient)

        btnMap.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }
        btnSetting.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        // Check location permission and fetch location
        currentLocation.checkPermissionsAndFetchLocation(permissionLauncher, object:CurrentLocation.LocationResultCallback{
            override fun onLocationRetrieved(latitude:Double,longitude:Double){
                updateLocationUI(latitude,longitude)
            }
            override fun onError(message:String){
                Toast.makeText(this@MainActivity,message,Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        compassManager.start()
        elevationManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        compassManager.stop()
        elevationManager.stopListening()
    }

    override fun onSensorChanged(event: SensorEvent?){
        if (event?.sensor?.type == Sensor.TYPE_ORIENTATION){
            val degree = Math.round(event.values[0])
            findViewById<ImageView>(R.id.compassNeedle).rotation = -degree.toFloat()
            findViewById<TextView>(R.id.degreeView).text = "Heading: ${degree}º"
        }
    }

    private fun convertToDMS(coordinate:Double,isLatitude:Boolean):String{
        val absolute = Math.abs(coordinate)
        val degrees = absolute.toInt()
        val minutesFull = (absolute-degrees) * 60
        val minutes = minutesFull.toInt()
        val seconds = ((minutesFull-minutes) * 60).toInt()

        val direction = if (isLatitude){
            if (coordinate >= 0) "N" else "S"
        } else{
            if (coordinate >= 0) "E" else "W"
        }
        return String.format("%d°%02d'%02d\" %s ", degrees, minutes, seconds, direction)
    }

    private fun updateLocationUI(latitude:Double,longitude:Double){
        val locationTextView = findViewById<TextView>(R.id.degreeText)
        val latitudeDMS = convertToDMS(latitude,true)
        val longitudeDMS = convertToDMS(longitude,false)
        locationTextView.text = "$latitudeDMS $longitudeDMS"
    }

    private fun getCurrentLocation(){
        currentLocation.fetchLocation(object:CurrentLocation.LocationResultCallback{
            override fun onLocationRetrieved(latitude: Double, longitude: Double) {
                updateLocationUI(latitude,longitude)
            }

            override fun onError(message: String) {
                Toast.makeText(this@MainActivity,message,Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onAccuracyChanged(sensor: Sensor?,accuracy:Int) {

    }

}

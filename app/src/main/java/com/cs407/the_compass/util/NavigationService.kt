package com.cs407.the_compass

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.*
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cs407.the_compass.util.NotificationUtils
import com.google.android.gms.location.*

class NavigationService : Service(), SensorEventListener {

    // Location-related fields
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var destinationLat: Double = Double.NaN
    private var destinationLon: Double = Double.NaN
    private var currentDistance: Float = 0f
    private var bearingToDestination: Float = 0f

    // Direction-related fields
    private var currentDirection = "--"

    // Sensor-related fields for compass updates
    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var currentAzimuth = 0f
    private var destinationName: String? = null


    override fun onCreate() {
        super.onCreate()
        NotificationUtils.createNotificationChannel(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize SensorManager for rotation updates
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        // Location callback updates distance and bearing
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val locations = result.locations
                if (locations.isNotEmpty()) {
                    val location = locations.last()
                    updateLocationInfo(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        destinationLat = intent?.getDoubleExtra("destination_lat", Double.NaN) ?: Double.NaN
        destinationLon = intent?.getDoubleExtra("destination_lon", Double.NaN) ?: Double.NaN
        destinationName = intent?.getStringExtra("destination_name")

        if (destinationLat.isNaN() || destinationLon.isNaN()) {
            Log.e("NavigationService", "Invalid destination coordinates.")
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceWithNotification()
        startLocationUpdates()
        startSensorUpdates()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopSensorUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification(distance = "--", direction = "--")
        startForeground(1, notification)
    }

    private fun updateNotification() {
        val notification = buildNotification(
            distance = "${currentDistance.toInt()} m",
            direction = currentDirection
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(1, notification)
    }

    private fun buildNotification(distance: String, direction: String): Notification {
        val notificationIntent = Intent(this, NavigationActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID)
            .setContentTitle("Navigation Active")
            .setContentText("Distance: $distance, Direction: $direction")
            .setSmallIcon(R.drawable.navigation_icon) // Ensure this icon exists
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("NavigationService", "Location permission not granted.")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun updateLocationInfo(location: Location) {
        val dest = Location("").apply {
            latitude = destinationLat
            longitude = destinationLon
        }
        currentDistance = location.distanceTo(dest)
        bearingToDestination = location.bearingTo(dest)

        updateDirectionFromAzimuth(currentAzimuth)
        updateNotification()
        broadcastUpdates(location.latitude, location.longitude) // Pass current location
    }

    private fun broadcastUpdates(currentLat: Double, currentLon: Double) {
        val intent = Intent("com.cs407.the_compass.UPDATE_NAVIGATION")
        intent.putExtra("distance", currentDistance)
        intent.putExtra("direction", currentDirection)
        intent.putExtra("bearingToDestination", bearingToDestination)
        intent.putExtra("currentAzimuth", currentAzimuth)
        intent.putExtra("currentLat", currentLat)    // Add current location
        intent.putExtra("currentLon", currentLon)
        if (!destinationName.isNullOrBlank()) {
            intent.putExtra("destinationName", destinationName)
        } else {
            intent.putExtra("destinationName", "$destinationLat, $destinationLon")
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }



    private fun updateDirectionFromAzimuth(azimuth: Float) {
        // Convert azimuth to direction
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val rawIndex = ((azimuth + 22.5f) / 45f).toInt()
        val idx = ((rawIndex % 8) + 8) % 8
        currentDirection = directions[idx]
    }

    private fun broadcastUpdates() {
        // Send broadcast to NavigationActivity
        val intent = Intent("com.cs407.the_compass.UPDATE_NAVIGATION")
        intent.putExtra("distance", currentDistance)
        intent.putExtra("direction", currentDirection)
        intent.putExtra("bearingToDestination", bearingToDestination)
        intent.putExtra("currentAzimuth", currentAzimuth)
        // Include the name if available
        if (!destinationName.isNullOrBlank()) {
            intent.putExtra("destinationName", destinationName)
        } else {
            // If no name is provided, we can send coordinates as a fallback
            intent.putExtra("destinationName", "${destinationLat}, ${destinationLon}")
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun startSensorUpdates() {
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthInDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val normalizedAzimuth = (azimuthInDegrees + 360) % 360

            // Smooth the azimuth if desired
            val smoothFactor = 0.1f
            currentAzimuth = currentAzimuth + smoothFactor * (
                    ((normalizedAzimuth - currentAzimuth + 540) % 360) - 180
                    )
            currentAzimuth = (currentAzimuth + 360) % 360

            // Update direction even if location not changed
            updateDirectionFromAzimuth(currentAzimuth)
            updateNotification()
            broadcastUpdates()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}

package com.cs407.the_compass.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class CompassManager(
    private val context: Context,
    private val compassView: ImageView,
    private val degreeTextView: TextView
) : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var currentDegree = 0f
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    fun start() {
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
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
                val degree = Math.toDegrees(orientation[0].toDouble()).toFloat()

                // Normalize and smooth the degree
                val normalizedDegree = (degree % 360 + 360) % 360

                // Calculate the shortest path for rotation, instead of go around
                var delta = normalizedDegree - currentDegree
                if (delta > 180) delta -= 360
                if (delta < -180) delta += 360
                currentDegree += delta * 0.1f // Smoothing factor for animation
                currentDegree = (currentDegree % 360 + 360) % 360

                // Update UI
                val direction = getDirection(currentDegree)
                degreeTextView.text = "${currentDegree.toInt()}º $direction"
                compassView.rotation = -currentDegree
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD && accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Toast.makeText(context, "Magnetic field sensor accuracy is low.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDirection(degree: Float): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((degree + 22.5) / 45).toInt() % 8
        return directions[index]
    }
}
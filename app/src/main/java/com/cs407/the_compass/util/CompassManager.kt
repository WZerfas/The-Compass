package com.cs407.the_compass.util

import android.content.Context
import android.hardware.*
import android.os.Handler
import android.os.Looper
import android.util.Log

class CompassManager(
    private val context: Context,
    private val callback: (degree: Float, direction: String) -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var currentDegree = 0f
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    private val handler = Handler(Looper.getMainLooper())

    fun start() {
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        } ?: run {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            magnetometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        try {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    val orientation = FloatArray(3)
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val degree = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    updateCompass(degree)
                }
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
                    updateCompass(degree)
                }
            }
        } catch (e: Exception) {
            Log.e("CompassManager", "Error processing sensor data", e)
        }
    }

    private fun updateCompass(rawDegree: Float) {
        val normalizedDegree = (rawDegree + 360) % 360
        val delta = ((normalizedDegree - currentDegree + 540) % 360) - 180
        currentDegree += delta * 0.1f // Smoothing factor
        currentDegree = (currentDegree + 360) % 360 // We normalize to 0-360

        val direction = getDirection(currentDegree)
        handler.post {
            callback(currentDegree, direction)
        }
    }

    private fun getDirection(degree: Float): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((degree + 22.5f) / 45f).toInt() % 8
        return directions[index]
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle sensor accuracy changes if necessary
    }
}

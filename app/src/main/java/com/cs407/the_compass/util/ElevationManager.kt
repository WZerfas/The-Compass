package com.cs407.the_compass.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Manages elevation calculations based on barometric pressure readings.
 **/
class ElevationManager(
    private val context: Context,
    private val callback: (elevation: Float?, pressure: Float?) -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager
    private val pressureSensor: Sensor?
    private var seaLevelPressure = 1013.25f

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (pressureSensor == null) {
            callback(null, null) // Notify that the pressure sensor is unavailable
        }
    }

    /**
     * Starts listening to pressure sensor updates.
     */
    fun startListening() {
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Stops listening to pressure sensor updates.
     */
    fun stopListening() {
        pressureSensor?.let {
            sensorManager.unregisterListener(this)
        }
    }

    /**
     * Sets the sea-level pressure for altitude calculation.
     **/
    fun setSeaLevelPressure(pressure: Float) {
        seaLevelPressure = pressure
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            val altitude = SensorManager.getAltitude(seaLevelPressure, pressure)
            Log.d("ElevationManager", "Pressure: $pressure hPa, Altitude: $altitude m")
            // Ensure callback is called on the main thread
            Handler(Looper.getMainLooper()).post {
                callback(altitude, pressure)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("ElevationManager", "Sensor accuracy changed: $accuracy")
    }
}

package com.cs407.the_compass.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ElevationManager (
    private val context: Context,
    private val callback: (elevation: Float?,pressure:Float?)->Unit

):SensorEventListener{
    private var sensorManager:SensorManager ?= null
    private var pressureSensor: Sensor?= null

    // Standard sea-level atmospheric pressure in hPa
    private val seaLevelPressure = 1013.25f

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null){
            callback(null,null) // Notify the pressure sensor is unavailable
        }
    }

    fun startListening(){
        pressureSensor?.let{
            sensorManager?.registerListener(this,it,SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening(){
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event:SensorEvent?) {
        if(event?.sensor?.type == Sensor.TYPE_PRESSURE){
            val pressure = event.values[0]
            val altitude = SensorManager.getAltitude(seaLevelPressure, pressure)
            callback(altitude,pressure)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}
package com.cs407.the_compass.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView

class CompassManager (
    private val context: Context, private val compassView: ImageView,
            private val degreeTextView: TextView
): SensorEventListener{
    private var sensorManager:SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var currentDegree = 0f
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    init{
        sensorManager=context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer=sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer=sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    fun start(){
        sensorManager?.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_GAME)
        sensorManager?.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop(){
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event:SensorEvent?){
        if(event==null) return
        when (event.sensor.type){
            Sensor.TYPE_ACCELEROMETER -> gravity = event.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.clone()
        }

        if (gravity != null && geomagnetic != null){
            val R = FloatArray(9)
            val I = FloatArray(9)
            if (SensorManager.getRotationMatrix(R,I,gravity,geomagnetic)){
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R,orientation)
                val degree = Math.toDegrees(orientation[0].toDouble()).toFloat()

                // Smooth the degree value
                val alpha = 0.1f //Change this between 0 and 1 for smoother rotation
                fun normalizeDegree(degree:Float):Float{
                    return (degree + 360)%360
                }

                fun getDirection(degree:Float):String{
                    return when{
                        degree >= 337.5 || degree < 22.5 -> "N"
                        degree >= 22.5 && degree < 67.5 -> "NE"
                        degree >= 67.5 && degree < 112.5 -> "E"
                        degree >= 112.5 && degree < 157.5 -> "SE"
                        degree >= 157.5 && degree < 202.5 -> "S"
                        degree >= 202.5 && degree < 247.5 -> "SW"
                        degree >= 247.5 && degree < 292.5 -> "W"
                        degree >= 292.5 && degree < 337.5 -> "NW"
                        else -> "N" //Should not reach here, bad thing happen.
                    }
                }

                val normalizedDegree = normalizeDegree(degree)
                currentDegree = alpha * normalizedDegree + (1-alpha) * currentDegree

                val direction = getDirection(normalizedDegree)
                degreeTextView.text = "${normalizedDegree.toInt()}º $direction"
                compassView.rotation = -currentDegree
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //Placeholder
    }
}
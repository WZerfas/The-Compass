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
                currentDegree = alpha * normalizeDegree(degree) + (1-alpha) * currentDegree

                degreeTextView.text = "${degree.toInt()} degrees"
                compassView.rotation = -currentDegree
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //Placeholder
    }
}
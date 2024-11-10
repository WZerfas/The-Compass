package com.cs407.the_compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    var sensor: Sensor? = null
    var sensorManager: SensorManager? = null
    lateinit var compassImage: ImageView
    lateinit var rotationTV: TextView

    // Track the rotation of the compass
    var currentDegree = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        compassImage = findViewById(R.id.compassNeedle)
        rotationTV = findViewById(R.id.degreeView)

        if (sensor == null) {
            rotationTV.text = "Orientation sensor not available"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val degree = event?.values?.get(0)?.let { Math.round(it) } ?: return
        rotationTV.text = "$degree degrees"
        val rotationAnimation = RotateAnimation(
            currentDegree,
            (-degree).toFloat(),
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )

        rotationAnimation.duration = 240
        rotationAnimation.fillAfter = true

        compassImage.startAnimation(rotationAnimation)
        currentDegree = (-degree).toFloat()
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not implemented
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }
}

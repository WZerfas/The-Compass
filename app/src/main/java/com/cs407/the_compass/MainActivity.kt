package com.cs407.the_compass

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cs407.the_compass.util.CompassManager

class MainActivity : AppCompatActivity() {
    private lateinit var compassManager: CompassManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val compassImage = findViewById<ImageView>(R.id.compassNeedle)
        val degreeText = findViewById<TextView>(R.id.degreeView)
        val btnMap = findViewById<ImageView>(R.id.btnMap)

        compassManager = CompassManager(this, compassImage, degreeText)

        btnMap.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        compassManager.start()
    }

    override fun onPause() {
        super.onPause()
        compassManager.stop()
    }
}

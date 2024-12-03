package com.cs407.the_compass

import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cs407.the_compass.util.CompassManager

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        val btnHome = findViewById<ImageView>(R.id.BtnReturn_Set)
        //val btnMap = findViewById<ImageView>(R.id.btnMap)
        val locationLogSwitch = findViewById<Switch>(R.id.locationLogSwitch)
        val logStatusText = findViewById<TextView>(R.id.LogstatusText)

        btnHome.setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
            finish()
        }

        logStatusText.text = if (locationLogSwitch.isChecked) "On" else "Off"
        // Add a listener to the Switch
        locationLogSwitch.setOnCheckedChangeListener { _, isChecked ->
            logStatusText.text = if (isChecked) "On" else "Off"
        }
        /**btnMap.setOnClickListener{
            val intent = Intent(this,NavigationActivity::class.java)
            startActivity(intent)
        } */
    }
}
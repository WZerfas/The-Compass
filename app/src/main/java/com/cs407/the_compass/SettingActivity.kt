package com.cs407.the_compass

import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
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

        val btnHome = findViewById<ImageView>(R.id.btnHome)
        val btnMap = findViewById<ImageView>(R.id.btnMap)

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnMap.setOnClickListener{
            val intent = Intent(this,NavigationActivity::class.java)
            startActivity(intent)
        }
    }
}
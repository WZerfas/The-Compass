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

class NavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val btnHome = findViewById<ImageView>(R.id.btnHome)
        val btnSetting = findViewById<ImageView>(R.id.btnSetting)
        val btnSearch = findViewById<ImageView>(R.id.btnSearch)

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnSetting.setOnClickListener{
            val intent = Intent(this,SettingActivity::class.java)
            startActivity(intent)
        }

        btnSearch.setOnClickListener{
            val intent = Intent(this,SearchActivity::class.java)
            startActivity(intent)
        }
    }
}
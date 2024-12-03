package com.cs407.the_compass

import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cs407.the_compass.util.CompassManager

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        val btnHome = findViewById<ImageView>(R.id.BtnReturn_Set)
        val locationLogSwitch = findViewById<Switch>(R.id.locationLogSwitch)
        val logStatusText = findViewById<TextView>(R.id.LogstatusText)
        val bookMarkFavorateText = findViewById<TextView>(R.id.bookMarkFavorateText)

        btnHome.setOnClickListener {
            finish()
        }

        logStatusText.text = if (locationLogSwitch.isChecked) "On" else "Off"
        locationLogSwitch.setOnCheckedChangeListener { _, isChecked ->
            logStatusText.text = if (isChecked) "On" else "Off"
        }

        bookMarkFavorateText.setOnClickListener {
            showInputDialog()
        }
    }

    private fun showInputDialog() {
        // Create an EditText to input location name
        val input = EditText(this)
        input.hint = "Enter location name"

        // Build an AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Favorite")
            .setMessage("Enter the location you want save as favorite")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val locationName = input.text.toString()
                if (locationName.isNotEmpty()) {
                    saveToSharedPreferences(locationName)
                    Toast.makeText(this, "Location saved: $locationName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Location cannot be empty!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun saveToSharedPreferences(locationName: String) {
        val sharedPreferences = getSharedPreferences("StoredFavorite", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Use a fixed key to save the favorite location
        editor.putString("favorite", locationName)
        editor.apply()
    }

}
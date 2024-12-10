package com.cs407.the_compass

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cs407.the_compass.util.NotificationUtils

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // Initialize notification channels
        NotificationUtils.createNotificationChannel(this)

        val btnHome = findViewById<ImageView>(R.id.BtnReturn_Set)
        val locationLogSwitch = findViewById<Switch>(R.id.locationLogSwitch)
        val receptionAlertSwitch = findViewById<Switch>(R.id.AlertSwitch2)
        val logStatusText = findViewById<TextView>(R.id.LogstatusText)
        val alertStatusText = findViewById<TextView>(R.id.alertStatText)
        val bookMarkFavorateText = findViewById<TextView>(R.id.bookMarkFavorateText)
        val clearLocationLogText = findViewById<TextView>(R.id.clearLocationLogText)

        clearLocationLogText.setOnClickListener {
            // Show confirmation dialog before clearing
            AlertDialog.Builder(this)
                .setTitle("Clear Search History")
                .setMessage("Are you sure you want to clear all search history?")
                .setPositiveButton("Clear") { _, _ ->
                    clearSearchHistory()
                    Toast.makeText(this, "LocationLog cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnHome.setOnClickListener {
            finish()
        }

        // Load saved states
        val sharedPreferences = getSharedPreferences("StoredPreferences", Context.MODE_PRIVATE)

        // Location log switch setup
        val isLocationLogEnabled = sharedPreferences.getBoolean("locationLogEnabled", false)
        locationLogSwitch.isChecked = isLocationLogEnabled
        logStatusText.text = if (isLocationLogEnabled) "On" else "Off"

        // Reception alert switch setup
        val isReceptionAlertEnabled = sharedPreferences.getBoolean("receptionAlertEnabled", false)
        receptionAlertSwitch.isChecked = isReceptionAlertEnabled
        alertStatusText.text = if (isReceptionAlertEnabled) "On" else "Off"

        // Switch listeners
        locationLogSwitch.setOnCheckedChangeListener { _, isChecked ->
            logStatusText.text = if (isChecked) "On" else "Off"
            saveSwitchState("locationLogEnabled", isChecked)
        }

        receptionAlertSwitch.setOnCheckedChangeListener { _, isChecked ->
            alertStatusText.text = if (isChecked) "On" else "Off"
            saveSwitchState("receptionAlertEnabled", isChecked)
        }

        bookMarkFavorateText.setOnClickListener {
            showInputDialog()
        }
    }


    private fun saveSwitchState(key: String, isChecked: Boolean) {
        val sharedPreferences = getSharedPreferences("StoredPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, isChecked)
        editor.apply()
    }

    private fun showInputDialog() {
        val alertDialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val input1 = EditText(this).apply {
            hint = "Enter location name"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        val input2 = EditText(this).apply {
            hint = "Enter coordinate name"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        alertDialogLayout.addView(input1)
        alertDialogLayout.addView(input2)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Favorite")
            .setMessage("Enter the location you want to save as favorite")
            .setView(alertDialogLayout)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val locationName = input1.text.toString().trim()
            val locationCoordinate = input2.text.toString().trim()

            when {
                locationName.isNotEmpty() && locationCoordinate.isNotEmpty() -> {
                    // Both fields filled
                    saveToSharedPreferences(locationName, locationCoordinate)
                    Toast.makeText(this, "Location saved with name and coordinates", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                locationName.isNotEmpty() -> {
                    // Only name filled, clear coordinate
                    saveToSharedPreferences(locationName, shouldClearCoordinate = true)
                    Toast.makeText(this, "Location saved with name: $locationName", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                locationCoordinate.isNotEmpty() -> {
                    // Only coordinate filled, clear name
                    saveToSharedPreferences(locationCoordinate = locationCoordinate, shouldClearName = true)
                    Toast.makeText(this, "Location saved with coordinates: $locationCoordinate", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                else -> {
                    // Neither field filled
                    Toast.makeText(this, "Please enter at least a location name or coordinates", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveToSharedPreferences(
        locationName: String? = null,
        locationCoordinate: String? = null,
        shouldClearName: Boolean = false,
        shouldClearCoordinate: Boolean = false
    ) {
        val sharedPreferences = getSharedPreferences("StoredPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Handle location name
        if (shouldClearName) {
            editor.remove("favoriteName")
        } else {
            locationName?.let { editor.putString("favoriteName", it) }
        }

        // Handle location coordinate
        if (shouldClearCoordinate) {
            editor.remove("favoriteCoordinate")
        } else {
            locationCoordinate?.let { editor.putString("favoriteCoordinate", it) }
        }

        editor.apply()
    }

    private fun clearSearchHistory() {
        val sharedPreferences = getSharedPreferences("StoredPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Clear all location log entries (1-5)
        for (i in 1..5) {
            editor.remove("locationLogLat$i")
            editor.remove("locationLogLon$i")
            editor.remove("locationLogName$i")
        }

        editor.apply()
    }
}
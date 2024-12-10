package com.cs407.the_compass

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
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
        val input = EditText(this)
        input.hint = "Enter location name"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Favorite")
            .setMessage("Enter the location you want save as favorite")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val locationName = input.text.toString()
                if (locationName.isNotEmpty()) {
                    saveFavoriteLocation(locationName)
                    Toast.makeText(this, "Location saved: $locationName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Location cannot be empty!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun saveFavoriteLocation(locationName: String) {
        val sharedPreferences = getSharedPreferences("StoredPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("favorite", locationName)
        editor.apply()
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1
        private const val REQUEST_PHONE_STATE_PERMISSION = 2
    }
}
package com.cs407.the_compass

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.cs407.the_compass.util.NotificationUtils

class SettingActivity : AppCompatActivity() {

    private lateinit var receptionAlertSwitch: Switch
    private lateinit var alertStatusText: TextView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, keep the switch on
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (!shouldShowRationale) {
                showNotificationPermissionDeniedDialog()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }

            // Since permission was not granted, revert the switch to off
            receptionAlertSwitch.isChecked = false
            alertStatusText.text = "Off"
            saveSwitchState("receptionAlertEnabled", false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // Initialize notification channels
        NotificationUtils.createNotificationChannel(this)

        val btnHome = findViewById<ImageView>(R.id.BtnReturn_Set)
        val locationLogSwitch = findViewById<Switch>(R.id.locationLogSwitch)
        receptionAlertSwitch = findViewById(R.id.AlertSwitch2)
        val logStatusText = findViewById<TextView>(R.id.LogstatusText)
        alertStatusText = findViewById(R.id.alertStatText)
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
            if (isChecked) {
                // If user turns on and Android 13+, ensure notification permission is granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        // Request permission
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Already granted
                        alertStatusText.text = "On"
                        saveSwitchState("receptionAlertEnabled", true)
                    }
                } else {
                    // For Android versions below 13, no permission required
                    alertStatusText.text = "On"
                    saveSwitchState("receptionAlertEnabled", true)
                }
            } else {
                // Switch turned off
                alertStatusText.text = "Off"
                saveSwitchState("receptionAlertEnabled", false)
            }
        }

        bookMarkFavorateText.setOnClickListener {
            showInputDialog()
        }
    }

    private fun showNotificationPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Notification Permission Required")
            .setMessage("This app requires notification permission to keep you updated on important events. Please enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
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

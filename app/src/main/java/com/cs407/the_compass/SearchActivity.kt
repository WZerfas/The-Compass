package com.cs407.the_compass

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class SearchActivity : AppCompatActivity() {
    private var searchingCoordinate = false
    private lateinit var searchEditText: EditText
    private lateinit var confirmButton: ImageView
    private lateinit var returnButton: ImageView
    private lateinit var searchModeSwitch: Switch
    private lateinit var modeTextView: TextView

    private lateinit var suggestionsListView: ListView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchEditText = findViewById(R.id.searchText)
        confirmButton = findViewById(R.id.btnConfirmSearch)
        returnButton = findViewById(R.id.btnReturn)
        searchModeSwitch = findViewById(R.id.searchModeSwitch)
        modeTextView = findViewById(R.id.modeTextView)
        suggestionsListView = findViewById(R.id.suggestionsList)

        searchModeSwitch.isChecked = searchingCoordinate
        updateMode()

        searchModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            searchingCoordinate = isChecked
            updateMode()
        }

        returnButton.setOnClickListener {
            finish()
        }

        confirmButton.setOnClickListener {
            val userInputText = searchEditText.text.toString()
            if (userInputText.isNotEmpty()) {
                if (searchingCoordinate) {
                    // Coordinate Mode
                    handleCoordinateInput(userInputText)
                } else {
                    // Address Mode
                    handleAddressInput(userInputText)
                }
            } else {
                Toast.makeText(this, "Please enter an address or coordinates!", Toast.LENGTH_SHORT).show()
            }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!searchingCoordinate) { // Only for Address Mode
                    val userInput = s.toString()
                    if (userInput.isNotEmpty()) {
                        displaySuggestions(userInput)
                    } else {
                        suggestionsListView.visibility = View.GONE
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


    }

    private fun displaySuggestions(query: String) {
        val databaseAccess = DatabaseAccess.getInstance(this)
        databaseAccess.open()
        val matchingLocations = databaseAccess.getLocationsByQuery(query) // Implement this in your database logic
        databaseAccess.close()

        if (matchingLocations.isEmpty()) {
            suggestionsListView.visibility = View.GONE
        } else {
            suggestionsListView.visibility = View.VISIBLE
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, matchingLocations.map { it.first })
            suggestionsListView.adapter = adapter

            suggestionsListView.setOnItemClickListener { _, _, position, _ ->
                val selectedLocation = matchingLocations[position]
                searchEditText.setText(selectedLocation.first) // Fill selected location
                suggestionsListView.visibility = View.GONE
            }
        }
    }


    private fun updateMode() {
        if (searchingCoordinate) {
            modeTextView.text = "Coordinate Mode"
            searchEditText.hint = "Enter coordinates (lat,lon)"
        } else {
            modeTextView.text = "Address Mode"
            searchEditText.hint = "Enter address"
        }
    }

    private fun handleCoordinateInput(inputText: String) {
        val coordinates = inputText.split(",").map { it.trim() }
        if (coordinates.size == 2) {
            try {
                val latitude = coordinates[0].toDouble()
                val longitude = coordinates[1].toDouble()

                Log.d("SearchActivity","Parsed coordinates: latitude=$latitude, longitude=$longitude")

                // Validate coordinates
                if (latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                    // Pass the coordinates to NavigationActivity
                    navigateToDestination(latitude, longitude)
                } else {
                    Toast.makeText(this, "Invalid coordinates!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(
                    this,
                    "Enter valid coordinates in lat,lon!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                this,
                "Enter coordinates in lat,lon!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleAddressInput(address: String) {
        val databaseAccess = DatabaseAccess.getInstance(this)
        databaseAccess.open()
        val locationDetails = databaseAccess.getLocationByName(address) // Query the database by name
        databaseAccess.close()

        if (locationDetails != null) {
            // Name exists in the database
            val (latitude, longitude) = locationDetails
            navigateToDestination(latitude, longitude)
        } else {
            // Fallback to Geocoder if name is not found in the database
            val geocoder = Geocoder(this, Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocationName(address, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val location = addresses[0]
                    val latitude = location.latitude
                    val longitude = location.longitude
                    navigateToDestination(latitude, longitude)
                } else {
                    Toast.makeText(this, "Cannot get address!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Geocoding failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun navigateToDestination(latitude: Double, longitude: Double) {
        Log.d("SearchActivity","Navigation to destination: latitude=$latitude, longitude=$longitude")
        val intent = Intent(this, NavigationActivity::class.java)
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}

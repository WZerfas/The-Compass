package com.cs407.the_compass

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale
import android.content.Context
import android.location.LocationManager

class SearchActivity : AppCompatActivity() {
    private var searchingCoordinate = false
    private lateinit var searchEditText: EditText
    private lateinit var confirmButton: ImageView
    private lateinit var returnButton: ImageView
    private lateinit var searchModeSwitch: Switch
    private lateinit var modeTextView: TextView
    private lateinit var favoriteButton: ImageView
    private lateinit var suggestionsListView: ListView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLatitude: Double? = null
    private var userLongitude: Double? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Initialize Views
        searchEditText = findViewById(R.id.searchText)
        confirmButton = findViewById(R.id.btnConfirmSearch)
        returnButton = findViewById(R.id.btnReturn)
        searchModeSwitch = findViewById(R.id.searchModeSwitch)
        modeTextView = findViewById(R.id.modeTextView)
        suggestionsListView = findViewById(R.id.suggestionsList)
        favoriteButton = findViewById(R.id.btnFavorite)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request Location Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getUserLocation()
        }

        searchModeSwitch.isChecked = searchingCoordinate
        updateMode()

        searchModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            searchingCoordinate = isChecked
            updateMode()
        }

        returnButton.setOnClickListener {
            finish()
        }

        favoriteButton.setOnClickListener {
            val sharedPreferenceFavor = getSharedPreferences("StoredPreferences", Context.MODE_PRIVATE)
            val favoriteLocation = sharedPreferenceFavor.getString("favorite", null)
            if (favoriteLocation == null) {
                Toast.makeText(this, "You haven't added a favorite location yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            searchEditText.setText(favoriteLocation)
        }

        confirmButton.setOnClickListener {
            val userInputText = searchEditText.text.toString()
            if (userInputText.isNotEmpty()) {
                if (searchingCoordinate) {
                    handleCoordinateInput(userInputText)
                } else {
                    handleAddressInput(userInputText)
                }
            } else {
                Toast.makeText(this, "Please enter an address or coordinates!", Toast.LENGTH_SHORT).show()
            }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!searchingCoordinate) {
                    val userInput = s.toString()
                    if (userInput.isNotEmpty() && userLatitude != null && userLongitude != null) {
                        displaySuggestions(userInput, userLatitude!!, userLongitude!!)
                    } else {
                        suggestionsListView.visibility = View.GONE
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun getUserLocation() {
        if (isLocationEnabled()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    Log.d("SearchActivity", "User location: $userLatitude, $userLongitude")
                } else {
                    Log.w("SearchActivity", "Unable to fetch user location")
                }
            }.addOnFailureListener { exception ->
                Log.e("SearchActivity", "Failed to get location: ${exception.message}")
            }
        } else {
            Log.e("SearchActivity", "Location services are disabled")
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun displaySuggestions(query: String, referenceLat: Double, referenceLon: Double) {
        val databaseAccess = DatabaseAccess.getInstance(this)
        databaseAccess.open()

        // Get matching locations with their distances
        val matchingLocations = databaseAccess.getLocationsByQuery(query, referenceLat, referenceLon)
        databaseAccess.close()

        if (matchingLocations.isEmpty()) {
            suggestionsListView.visibility = View.GONE
        } else {
            suggestionsListView.visibility = View.VISIBLE

        // Map the locations to display name and distance in a user-friendly way
        val adapterData = matchingLocations.map { location ->
            val name = location.first
            val (latitude, longitude, distance) = location.second
            "${name} - ${"%.2f".format(distance)} km away"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, adapterData)
            suggestionsListView.adapter = adapter

            suggestionsListView.setOnItemClickListener { _, _, position, _ ->
                val selectedLocation = matchingLocations[position]
                searchEditText.setText(selectedLocation.first)  // Fill selected location
                suggestionsListView.visibility = View.GONE

            // Correctly navigate to the selected destination
            navigateToDestination(
                selectedLocation.second.first,  // Latitude
                selectedLocation.second.second, // Longitude
                selectedLocation.first          // Destination Name
            )
        }
    }
}


    private fun updateMode() {
        if (searchingCoordinate) {
            modeTextView.text = "Coordinate Mode"
            searchEditText.hint = "Enter coordinates (lat,lon)"
        } else {
            modeTextView.text = "Search Mode"
            searchEditText.hint = "Searching for..."
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
                    // No destination name available in coordinate mode
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
            navigateToDestination(latitude, longitude, address)
        } else {
            // Fallback to Geocoder if name is not found in the database
            val geocoder = Geocoder(this, Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocationName(address, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val location = addresses[0]
                    val latitude = location.latitude
                    val longitude = location.longitude
                    navigateToDestination(latitude, longitude, address)
                } else {
                    Toast.makeText(this, "Cannot get address!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Geocoding failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDestination(latitude: Double, longitude: Double, destinationName: String? = null) {
        Log.d("SearchActivity","Navigation to destination: latitude=$latitude, longitude=$longitude, name=$destinationName")
        val intent = Intent(this, NavigationActivity::class.java).apply {
            putExtra("latitude", latitude)
            putExtra("longitude", longitude)
            destinationName?.let {
                putExtra("destination_name", it)
            }
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }
}

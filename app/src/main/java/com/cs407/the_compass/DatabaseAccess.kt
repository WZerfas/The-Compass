package com.cs407.the_compass

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseAccess private constructor(context: Context) {

    private val openHelper: SQLiteOpenHelper = DatabaseHelper(context)
    private var db: SQLiteDatabase? = null
    private val cursor: Cursor? = null

    companion object {
        @Volatile
        private var instance: DatabaseAccess? = null

        // To return the single instance of DatabaseAccess
        fun getInstance(context: Context): DatabaseAccess {
            return instance ?: synchronized(this) {
                instance ?: DatabaseAccess(context).also { instance = it }
            }
        }
    }

    // Opens the database
    fun open() {
        db = openHelper.writableDatabase
        Log.d("Database", "Database path: ${db?.path}")
    }

    // Closes the database connection
    fun close() {
        db?.close()
    }

    fun getLocationByName(name: String): Pair<Double, Double>? {
        val query = "SELECT latitude, longitude FROM locations WHERE name = ?"
        val cursor = db?.rawQuery(query, arrayOf(name))
        return cursor?.use {
            if (it.moveToFirst()) {
                val latitude = it.getDouble(it.getColumnIndexOrThrow("latitude"))
                val longitude = it.getDouble(it.getColumnIndexOrThrow("longitude"))
                Pair(latitude, longitude)
            } else {
                null
            }
        }
    }


    fun getLocationsByQuery(query: String, referenceLat: Double, referenceLon: Double): List<Pair<String, Triple<Double, Double, Double>>> {
        val locations = mutableListOf<Pair<String, Triple<Double, Double, Double>>>()

        val sqlQuery = """
        SELECT name, latitude, longitude, 
        ((latitude - $referenceLat) * (latitude - $referenceLat) + 
        (longitude - $referenceLon) * (longitude - $referenceLon)) AS distance 
        FROM locations WHERE name LIKE ? ORDER BY distance ASC LIMIT 3 """.trimIndent()

        val cursor = db?.rawQuery(sqlQuery, arrayOf("%$query%"))
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val latitude = it.getDouble(it.getColumnIndexOrThrow("latitude"))
                val longitude = it.getDouble(it.getColumnIndexOrThrow("longitude"))

                // Calculate Haversine distance in kilometers
                val distance = haversine(referenceLat, referenceLon, latitude, longitude)

                // Store name, latitude, longitude, and distance in the list
                locations.add(Pair(name, Triple(latitude, longitude, distance)))
            }
        }
        return locations
    }


    /**
     * Formula used to calculate the distance between two points on the surface of a sphere,
     * given their latitude and longitude.
     */
    private val EARTH_RADIUS_KM = 6371.0

    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }

}

package com.cs407.the_compass

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
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

    // To open the database
    fun open() {
        db = openHelper.writableDatabase
        Log.d("Database", "Database path: ${db?.path}")
    }

    // Closing the database connection
    fun close() {
        db?.close()
    }

    // Query to get the address by passing id
    fun getAddress(id: Int): String {
        val result = StringBuilder()
        //TODO Change "Places" to (table in locations)
        val query = "SELECT name, longitude, latitude FROM locations WHERE id = ?"
        val c = db?.rawQuery(query, arrayOf(id.toString()))

        c?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val longitude = it.getDouble(it.getColumnIndexOrThrow("longitude"))
                val latitude = it.getDouble(it.getColumnIndexOrThrow("latitude"))

                result.append("Name: $name, Longitude: $longitude, Latitude: $latitude")
            }
        }

        return result.toString()
    }



    fun logTables() {
        if (db == null) {
            Log.e("DatabaseAccess", "Database is not open. Cannot log tables.")
            return
        }

        try {
            val cursor = db?.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null
            )
            cursor?.use {
                Log.d("DatabaseAccess", "Tables in the database:")
                while (it.moveToNext()) {
                    val tableName = it.getString(0)
                    Log.d("DatabaseAccess", "Table: $tableName")
                }
            }
        } catch (e: Exception) {
            Log.e("DatabaseAccess", "Error while logging tables: ${e.message}")
        }
    }

    fun getLocationByName(name: String): Pair<Double, Double>? {
        //TODO Change Places
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
        FROM locations 
        WHERE name LIKE ? 
        ORDER BY distance ASC 
        LIMIT 3
    """.trimIndent()

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
    fun getLocationsByQuery(query: String, referenceLat: Double, referenceLon: Double): List<Pair<String, Triple<Double, Double, Double>>> {
        val locations = mutableListOf<Pair<String, Triple<Double, Double, Double>>>()

        // TODO Change places
        val sqlQuery = """
        SELECT name, latitude, longitude, 
        ((latitude - $referenceLat) * (latitude - $referenceLat) + 
        (longitude - $referenceLon) * (longitude - $referenceLon)) AS distance 
        FROM Places 
        WHERE name LIKE ? 
        ORDER BY distance ASC 
        LIMIT 3
    """.trimIndent()

        val cursor = db?.rawQuery(sqlQuery, arrayOf("%$query%"))

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val latitude = it.getDouble(it.getColumnIndexOrThrow("latitude"))
                val longitude = it.getDouble(it.getColumnIndexOrThrow("longitude"))
                val distance = it.getDouble(it.getColumnIndexOrThrow("distance"))

                // Add destination name, latitude, longitude, and distance to the list
                locations.add(Pair(name, Triple(latitude, longitude, distance)))
            }
        }
        return locations
    }
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

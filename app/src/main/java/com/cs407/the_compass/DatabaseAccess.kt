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
        val query = "SELECT name, longitude, latitude, region FROM Places WHERE id = ?"
        val c = db?.rawQuery(query, arrayOf(id.toString()))

        c?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val longitude = it.getDouble(it.getColumnIndexOrThrow("longitude"))
                val latitude = it.getDouble(it.getColumnIndexOrThrow("latitude"))
                val region = it.getString(it.getColumnIndexOrThrow("region"))

                result.append("Name: $name, Longitude: $longitude, Latitude: $latitude, Region: $region")
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

    fun getClosestLocations(latitude: Double, longitude: Double, limit: Int = 3): List<Pair<String, Double>> {
        val results = mutableListOf<Pair<String, Double>>()
        val query = """
        SELECT name, 
               latitude, 
               longitude, 
               ( (latitude - ?) * (latitude - ?) + (longitude - ?) * (longitude - ?) ) AS distance 
        FROM Places
        ORDER BY distance ASC
        LIMIT ?
    """.trimIndent()

        val cursor = db?.rawQuery(query, arrayOf(latitude.toString(), latitude.toString(), longitude.toString(), longitude.toString(), limit.toString()))
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0)
                val placeLatitude = it.getDouble(1)
                val placeLongitude = it.getDouble(2)
                val distance = calculateDistance(latitude, longitude, placeLatitude, placeLongitude)
                results.add(name to distance)
            }
        }
        return results
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 3958.8 // Radius in miles
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }


}

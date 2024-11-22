package com.cs407.the_compass

import android.content.Context
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

class DatabaseHelper(context: Context) : SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "places.db"
        private const val DATABASE_VERSION = 1
    }
}

/**class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "places.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Read and execute SQL file
        val inputStream = context.assets.open("places.db")
        val reader = BufferedReader(InputStreamReader(inputStream))

        db.beginTransaction()
        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.trim().isNotEmpty()) {
                        db.execSQL(line)
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades if needed
        db.execSQL("DROP TABLE IF EXISTS Places")
        onCreate(db)
    }

    fun getPlaceById(id: Int): String? {
        val db = readableDatabase
        val cursor = db.query(
            "Places", // Table name
            arrayOf("name", "longitude", "latitude", "region"), // Columns
            "id = ?", // WHERE clause
            arrayOf(id.toString()), // WHERE arguments
            null, null, null // Group by, having, order by
        )
        cursor.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val longitude = it.getDouble(it.getColumnIndexOrThrow("longitude"))
                val latitude = it.getDouble(it.getColumnIndexOrThrow("latitude"))
                val region = it.getString(it.getColumnIndexOrThrow("region"))
                return "Place: $name, Longitude: $longitude, Latitude: $latitude, Region: $region"
            }
        }
        return null
    }

}
 */

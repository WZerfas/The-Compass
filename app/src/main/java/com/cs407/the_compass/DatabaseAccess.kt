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
        val query = "SELECT * FROM Places WHERE id = 1"
        val c = db?.rawQuery(query, arrayOf(id.toString()))

        c?.use {
            while (it.moveToNext()) {
                val address = it.getString(0)
                result.append(address)
            }
        }

        return result.toString()
    }
}

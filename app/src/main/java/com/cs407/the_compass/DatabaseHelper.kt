package com.cs407.the_compass

import android.content.Context
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

class DatabaseHelper(context: Context) : SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "locations.db" // Ensure this matches your file name
        private const val DATABASE_VERSION = 1
    }

    init {
        // Ensure old database file is removed
        context.deleteDatabase(DATABASE_NAME)
    }
}


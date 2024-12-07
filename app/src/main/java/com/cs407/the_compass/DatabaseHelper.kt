package com.cs407.the_compass

import android.content.Context
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

class DatabaseHelper(context: Context) : SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        //TODO Change to locations.db
        private const val DATABASE_NAME = "places.db"
        private const val DATABASE_VERSION = 1
    }
}


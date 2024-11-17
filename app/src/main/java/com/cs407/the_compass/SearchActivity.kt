package com.cs407.the_compass

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val returnBtn = findViewById<ImageView>(R.id.btnReturn)
        val doSearchBtn = findViewById<ImageView>(R.id.btnConfirmSearch)
        val searchBar = findViewById<EditText>(R.id.searchText)

        returnBtn.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }

        doSearchBtn.setOnClickListener {
            val userInputText = searchBar.text.toString()
            Toast.makeText(this, "Searching: "+userInputText, Toast.LENGTH_SHORT).show()
        }

    }
}
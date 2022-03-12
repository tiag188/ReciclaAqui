package com.example.esgapireciclaaqui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun createRoutes(view: View) {
        val intent = Intent(this, RoutesActivity::class.java)
        startActivity(intent)
    }
}
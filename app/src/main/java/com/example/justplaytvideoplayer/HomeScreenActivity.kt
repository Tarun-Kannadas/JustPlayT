package com.example.justplaytvideoplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class HomeScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_screen)

        findViewById<Button>(R.id.btn_local).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java) // your current video listing screen
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_network).setOnClickListener {
            val intent = Intent(this, NetworkVideosActivity::class.java)
            startActivity(intent)
        }
    }
}
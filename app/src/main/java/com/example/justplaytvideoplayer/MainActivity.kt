package com.example.justplaytvideoplayer

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.justplaytvideoplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding  // Correct binding for activity_main.xml

    private fun loadVideos() {
        val videoList = mutableListOf<Pair<String, Uri>>() // Stores (Title, Uri)

        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media._ID
        )

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleIndex)
                val id = cursor.getLong(idIndex)
                val contentUri = ContentUris.withAppendedId(collection, id)
                videoList.add(Pair(title, contentUri))
            }
        }

        if (videoList.isNotEmpty()) {
            binding.videoRecyclerView.apply {
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MainActivity)
                adapter = VideoAdapter(this@MainActivity, videoList)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadVideos() // Load the video list here

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

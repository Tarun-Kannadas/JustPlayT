package com.example.justplaytvideoplayer

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.justplaytvideoplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadLocalVideos()

        val backButton: ImageButton = findViewById(R.id.backbtn)
        backButton.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadLocalVideos() {
        val videoList = mutableListOf<LocalVideo>() // ✅ Now using LocalVideo model

        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media._ID
        )

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

                videoList.add(
                    LocalVideo(
                        title = title,
                        uri = contentUri
                    )
                )
            }
        }

        if (videoList.isNotEmpty()) {
            binding.videoRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = LocalVideoAdapter(this@MainActivity, videoList) // ✅ LocalVideoAdapter used
            }
        }
    }
}

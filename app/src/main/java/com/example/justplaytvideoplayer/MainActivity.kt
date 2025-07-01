package com.example.justplaytvideoplayer

import android.net.Uri
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    private val isPlaying: Boolean
        get() = this::player.isInitialized && player.isPlaying

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // Define subtitles
        val subtitle = MediaItem.SubtitleConfiguration.Builder(
            Uri.parse("https://example.com/subtitle.srt")
        )
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage("en")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        // Define media item with subtitle
        val mediaItem = MediaItem.Builder()
            .setUri("https://youtu.be/3FByxYBcQA4") // 🔁 Use a valid .mp4 URL, not YouTube!
            .setSubtitleConfigurations(listOf(subtitle))
            .build()

        // Set and prepare media
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        // Log player status
        if (isPlaying) {
            Log.d("PlayerStatus", "Video is playing")
        } else {
            Log.d("PlayerStatus", "Video is paused or not initialized")
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::player.isInitialized) {
            player.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::player.isInitialized) {
            player.play()
        }
    }

    override fun onStop() {
        super.onStop()
        if (this::player.isInitialized) {
            player.release()
        }
    }
}

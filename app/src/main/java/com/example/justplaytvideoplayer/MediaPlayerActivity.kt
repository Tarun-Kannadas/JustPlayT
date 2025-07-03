package com.example.justplaytvideoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.justplaytvideoplayer.databinding.ActivityMediaPlayerBinding
import java.util.concurrent.TimeUnit

class MediaPlayerActivity : AppCompatActivity() {

    lateinit var binding: ActivityMediaPlayerBinding
    lateinit var player: ExoPlayer
    private var isFullscreen = false
    lateinit var gestureDetector: GestureDetector
    private lateinit var handler: Handler
    private lateinit var playPauseButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val layoutParams = window.attributes
        layoutParams.screenBrightness = 0.5f // set an initial value between 0.01 to 1.0
        window.attributes = layoutParams

        binding = ActivityMediaPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Request permission to modify brightness
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        val url = intent.getStringExtra("url")
        if (url.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid video URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Getting the title
        val title = intent.getStringExtra("title") ?: "Untitled"
        binding.playerView.findViewById<TextView>(R.id.titlecard)?.text = title

        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        // Assigning the media item
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        handler = Handler(mainLooper)

        // finding all the corresponding UI components
        playPauseButton = binding.playerView.findViewById(R.id.btn_play_pause)
        val rewindButton = binding.playerView.findViewById<ImageButton>(R.id.btn_rewind)
        val forwardButton = binding.playerView.findViewById<ImageButton>(R.id.btn_forward)
        val prevButton = binding.playerView.findViewById<ImageButton>(R.id.btn_prev)
        val nextButton = binding.playerView.findViewById<ImageButton>(R.id.btn_next)
        val fullscreenButton = binding.playerView.findViewById<ImageButton>(R.id.btn_fullscreen)
        seekBar = binding.playerView.findViewById(R.id.seek_bar)
        currentTimeText = binding.playerView.findViewById(R.id.current_time)
        totalTimeText = binding.playerView.findViewById(R.id.total_duration)

        val backButton = binding.playerView.findViewById<ImageButton>(R.id.backbtn)
        backButton.setOnClickListener {

            player.pause()
            player.release()
            // Finish the activity and return
            finish()
        }

        // Play/Pause Logic
        playPauseButton.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
                playPauseButton.setImageResource(R.drawable.ic_play)
            } else {
                player.play()
                playPauseButton.setImageResource(R.drawable.ic_play)
            }
        }

        // Rewind logic
        rewindButton.setOnClickListener {
            val newPosition = (player.currentPosition - 10000).coerceAtLeast(0)
            player.seekTo(newPosition)
        }

        // Forward logic
        forwardButton.setOnClickListener {
            val newPosition = (player.currentPosition + 10000).coerceAtMost(player.duration)
            player.seekTo(newPosition)
        }

        // Prev logic
        prevButton.setOnClickListener {
            player.seekTo(0)
        }

        // Next logic
        nextButton.setOnClickListener {
            player.seekTo(player.duration)
        }

        // Fullscreen Logic
        fullscreenButton.setOnClickListener {
            isFullscreen = !isFullscreen
            requestedOrientation = if (isFullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }

        // Gesture Logic
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val screenWidth = binding.root.width
                val tapX = e.x

                if (tapX < screenWidth / 2) {
                    val newPosition = (player.currentPosition - 10000).coerceAtLeast(0)
                    player.seekTo(newPosition)
                    Toast.makeText(applicationContext, "Rewinded 10 secs", Toast.LENGTH_SHORT).show()
                } else {
                    val newPosition = (player.currentPosition + 10000).coerceAtMost(player.duration)
                    player.seekTo(newPosition)
                    Toast.makeText(applicationContext, "Forwarded 10 secs", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            @OptIn(UnstableApi::class)
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                binding.playerView.showController()
                if (player.isPlaying) {
                    player.pause()
                    playPauseButton.setImageResource(R.drawable.ic_play)
                } else {
                    player.play()
                    playPauseButton.setImageResource(R.drawable.ic_play)
                }
                return true
            }

            // Scroll Volume & Brightness Logic
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                val deltaY = e2.y - (e1?.y ?: 0f)
                val deltaX = e2.x - (e1?.x ?: 0f)

                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

                // Vertical swipe = Volume
                if (Math.abs(deltaY) > Math.abs(deltaX)) {
                    if (deltaY > 50) {
                        // Swipe down = Volume down
                        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    } else if (deltaY < -50) {
                        // Swipe up = Volume up
                        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    }
                }
                // Horizontal swipe = Brightness
                else {
                    if (deltaX > 50) {
                        // Swipe right = Increase brightness
                        changeScreenBrightness(0.05f)
                    } else if (deltaX < -50) {
                        // Swipe left = Decrease brightness
                        changeScreenBrightness(-0.05f)
                    }
                }

                return true
            }
        })

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(applicationContext, "Error in Playing the Media", Toast.LENGTH_SHORT).show()
                super.onPlayerError(error)
            }
        })

        seekBar.max = 1000
        updateSeekBar()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Brightness Adjustment function
    private fun changeScreenBrightness(delta: Float) {
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "Permission to change brightness denied", Toast.LENGTH_SHORT).show()
            return
        }

        val currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 125)
        val newBrightness = (currentBrightness + (delta * 255)).toInt().coerceIn(10, 255)

        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, newBrightness)
    }

    // Seekbar update function
    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val position = player.currentPosition
                val duration = player.duration
                if (duration > 0) {
                    val progress = ((position.toDouble() / duration) * 1000).toInt()
                    seekBar.progress = progress

                    currentTimeText.text = formatTime(position)
                    totalTimeText.text = formatTime(duration)
                }
                handler.postDelayed(this, 1000)
            }
        }, 0)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = player.duration
                    val newPosition = (duration * progress) / 1000
                    player.seekTo(newPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun formatTime(milliseconds: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onStart() {
        super.onStart()
        player.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

}
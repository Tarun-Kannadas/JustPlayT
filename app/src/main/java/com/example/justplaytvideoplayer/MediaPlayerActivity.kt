package com.example.justplaytvideoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.GestureDetector
import android.view.Menu
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintSet.Motion
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.mediarouter.app.MediaRouteActionProvider
import androidx.mediarouter.app.MediaRouteButton
import androidx.mediarouter.app.MediaRouteDialogFactory
import com.example.justplaytvideoplayer.databinding.ActivityMediaPlayerBinding
import com.google.android.gms.cast.framework.CastButtonFactory
import java.io.File
import java.util.concurrent.TimeUnit
//import com.google.android.gms.cast.framework.media.widget.CastControlButtonsProvider
import com.google.android.gms.cast.framework.CastContext
import kotlin.math.abs

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
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private var gestureDirection: String? = null
    private var isZooming = false
    private val SUBTITLE_PICKER_REQUEST_CODE = 1001
    private var initialSeekPosition: Long = 0L
    private var lastSeekUpdateTime = 0L
    private var scrubbing = false

    private lateinit var gestureTooltip: TextView

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val layoutParams = window.attributes
        layoutParams.screenBrightness = 0.5f // set an initial value between 0.01 to 1.0
        window.attributes = layoutParams

        binding = ActivityMediaPlayerBinding.inflate(layoutInflater)
        CastContext.getSharedInstance(this)
        setContentView(binding.root)

        val castButton: MediaRouteButton = findViewById(R.id.btn_cast)
        val castContext = CastContext.getSharedInstance(this)

        castButton.routeSelector = castContext.mergedSelector!!
        castButton.setDialogFactory(MediaRouteDialogFactory.getDefault())

        // Initialize tooltip
        gestureTooltip = findViewById(R.id.gestureTooltip)
        gestureTooltip.visibility = View.GONE

        // Setuping the player
        val trackSelector = DefaultTrackSelector(this)
        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
        binding.playerView.player = player
        binding.playerView.setControllerShowTimeoutMs(1000) // hides after 1 second

        // ✅ Request permission to modify brightness
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        // Assigning the media items as a list
        val isNetwork = intent.getBooleanExtra("isNetwork", false)

        if (isNetwork)
        {
            val videoList = intent.getSerializableExtra("networkVideos") as? ArrayList<Video>
            val index = intent.getIntExtra("currentIndex", 0)

            if (videoList.isNullOrEmpty()) {
                Toast.makeText(this, "No Videos found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val mediaItems = videoList.map { video ->
                val sourceUrl = video.sources.firstOrNull()

                if (sourceUrl.isNullOrBlank()) {
                    Toast.makeText(this, "Invalid video source for: ${video.title}", Toast.LENGTH_SHORT).show()
                    return@map null
                }

                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(Uri.parse(sourceUrl))
                    .setMediaId(video.title)

                // Explicit MIME type for HLS
                if (sourceUrl.endsWith(".m3u8", ignoreCase = true)) {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }

                // Subtitles
                if (!video.subtitle.isNullOrEmpty()) {
                    val subtitleConfigs = video.subtitle.mapNotNull { subtitleUrl ->
                        val uri = Uri.parse(subtitleUrl)
                        val mimeType = when {
                            subtitleUrl.endsWith(".vtt", ignoreCase = true) -> "text/vtt"
                            subtitleUrl.endsWith(".srt", ignoreCase = true) -> "application/x-subrip"
                            else -> null
                        }

                        mimeType?.let {
                            MediaItem.SubtitleConfiguration.Builder(uri)
                                .setMimeType(it)
                                .setLanguage("en")
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        }
                    }
                    mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
                }

                mediaItemBuilder.build()
            }.filterNotNull() // Remove any nulls from invalid sources

            if (mediaItems.isEmpty()) {
                Toast.makeText(this, "No valid media items found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            player.setMediaItems(mediaItems, index, 0L)
            binding.playerView.findViewById<TextView>(R.id.titlecard)?.text = videoList[index].title
            binding.playerView.setShowSubtitleButton(true)
        }
        else
        {
            val urlList = intent.getStringArrayListExtra("videoUris") ?: arrayListOf()
            val currentIndex = intent.getIntExtra("currentIndex",0)

            if (urlList.isEmpty()) {
                Toast.makeText(this, "No videos found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val supportedSubtitleExts = listOf("srt", "vtt", "ass", "ssa", "sub", "txt")

            val mediaItems = urlList.map { videoPath ->
                val videoUri = Uri.parse(videoPath)
                val videoFile = File(videoUri.path!!)
                val videoDir = videoFile.parentFile
                val baseName = videoFile.nameWithoutExtension

                // Find any subtitle file with matching name in the same folder
                val subtitleFile = supportedSubtitleExts
                    .map { ext -> File(videoDir, "$baseName.$ext") }
                    .firstOrNull { it.exists() }

                val builder = MediaItem.Builder()
                    .setUri(videoUri)
                    .setMediaId(baseName)

                subtitleFile?.let {
                    val mimeType = when (it.extension.lowercase()) {
                        "srt" -> "application/x-subrip"
                        "vtt" -> "text/vtt"
                        "ass", "ssa" -> "text/x-ssa"
                        "sub", "txt" -> "application/x-subrip" // fallback
                        else -> "application/x-subrip"
                    }

                    val subtitleUri = Uri.fromFile(it)
                    val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                        .setMimeType(mimeType)
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()

                    builder.setSubtitleConfigurations(listOf(subtitleConfig))
                }

                builder.build()
            }

            player.setMediaItems(mediaItems, currentIndex, 0L)
            binding.playerView.findViewById<TextView>(R.id.titlecard)?.text = Uri.parse(urlList[currentIndex]).lastPathSegment ?: "Untitled"
            binding.playerView.setShowSubtitleButton(true)
        }

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
            Toast.makeText(applicationContext, "Rewinded 10 secs", Toast.LENGTH_SHORT).show()
        }

        // Forward logic
        forwardButton.setOnClickListener {
            val newPosition = (player.currentPosition + 10000).coerceAtMost(player.duration)
            player.seekTo(newPosition)
            Toast.makeText(applicationContext, "Forwarded 10 secs", Toast.LENGTH_SHORT).show()
        }

        // Prev logic
        prevButton.setOnClickListener {
            if (player.hasPreviousMediaItem()) {
                player.seekToPrevious()
            } else {
                player.seekTo(0) // Restart current

            }
        }

        // Next logic
        nextButton.setOnClickListener {
            if (player.hasNextMediaItem()) {
                player.seekToNext()
            }
            else
            {
                Toast.makeText(this, "No next video", Toast.LENGTH_SHORT).show()
            }
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


            // Single Tap Logics for gestures
            @OptIn(UnstableApi::class)
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                binding.playerView.showController()

                // Get the touch location
                val x = e.rawX.toInt()
                val y = e.rawY.toInt()

                // Check if the touch was on a control button (like play/pause)
                val playPauseRect = IntArray(2).apply { playPauseButton.getLocationOnScreen(this) }
                val px = playPauseRect[0]
                val py = playPauseRect[1]
                val pw = playPauseButton.width
                val ph = playPauseButton.height

                val tappedPlayPause = (x in px..(px + pw) && y in py..(py + ph))

                if (!tappedPlayPause) {
                    if (player.isPlaying) {
                        player.pause()
                        playPauseButton.setImageResource(R.drawable.ic_play)
                    } else {
                        player.play()
                        playPauseButton.setImageResource(R.drawable.ic_play)
                    }
                }
                return true
            }

            // Scroll Volume & Video Seekbar Logic
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isZooming) return false

                val deltaY = e2.y - (e1?.y ?: 0f)
                val deltaX = e2.x - (e1?.x ?: 0f)
                val absDeltaX = abs(deltaX)
                val absDeltaY = abs(deltaY)

                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

                // Detect direction (horizontal for seek, vertical for volume)
                if (gestureDirection == null) {
                    gestureDirection = if (absDeltaY > absDeltaX) "vertical" else "horizontal"
                    if (gestureDirection == "horizontal") {
                        initialSeekPosition = player.currentPosition
                        scrubbing = true
                    }
                }

                if (gestureDirection == "vertical") {
                    // Volume Control
                    if (deltaY > 50) {
                        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    } else if (deltaY < -50) {
                        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    }
                } else if (gestureDirection == "horizontal") {
                    // Seek Logic
                    val screenWidth = binding.root.width
                    val percentMoved = deltaX / screenWidth
                    val seekOffset = (percentMoved * player.duration).toLong()
                    val newSeekPosition = (initialSeekPosition + seekOffset).coerceIn(0, player.duration)

                    // Show tooltip
                    val delta = newSeekPosition - initialSeekPosition
                    val sign = if (delta >= 0) "+" else "-"
                    val timeDiff = formatTime(abs(delta))

                    // Set and show tooltip
                    gestureTooltip.text = "$sign$timeDiff"
                    gestureTooltip.visibility = View.VISIBLE

                    // Auto-hide after 1 second
                    gestureTooltip.removeCallbacks(hideTooltipRunnable)
                    gestureTooltip.postDelayed(hideTooltipRunnable, 1000)

                    // Throttle seek updates
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSeekUpdateTime > 100) {
                        player.seekTo(newSeekPosition)
                        updateSeekBarVisuals(newSeekPosition)
                        lastSeekUpdateTime = currentTime
                    }
                }
                return true
            }
        })

        // Scale Gesture Detector
        scaleGestureDetector = ScaleGestureDetector(this, object: ScaleGestureDetector.SimpleOnScaleGestureListener(){

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isZooming = true
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(1.0f,3.0f)

                binding.playerView.apply {
                    pivotX = detector.focusX
                    pivotY = detector.focusY

                    animate()
                        .scaleX(scaleFactor)
                        .scaleY(scaleFactor)
                        .setDuration(200)
                        .start()
                }

                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isZooming = false
            }
        })

        seekBar.max = 1000
        updateSeekBar()

        player.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(applicationContext, "Error in Playing the Media", Toast.LENGTH_SHORT).show()
                super.onPlayerError(error)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                // Check if the video is from network or local
                if (isNetwork) {
                    val videoList = intent.getSerializableExtra("networkVideos") as? ArrayList<Video>
                    val currentIndex = player.currentMediaItemIndex
                    if (videoList != null && currentIndex < videoList.size) {
                        binding.playerView.findViewById<TextView>(R.id.titlecard)?.text = videoList[currentIndex].title
                    }
                } else {
                    val uriList = intent.getStringArrayListExtra("videoUris") ?: arrayListOf()
                    val currentIndex = player.currentMediaItemIndex
                    if (currentIndex < uriList.size) {
                        binding.playerView.findViewById<TextView>(R.id.titlecard)?.text =
                            Uri.parse(uriList[currentIndex]).lastPathSegment ?: "Untitled"
                    }
                }
            }
        })

        // 3 Dot Menu Mechanism
        val menuBtn = findViewById<ImageButton>(R.id.btn_menu)

        menuBtn.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.player_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_dark_mode -> {
                        toggleDarkMode()
                        true
                    }
                    R.id.action_subtitles -> {
                        if (!isNetwork) {
                            openSubtitleFilePicker() // local storage manual subtitle uploading
                        } else {
                            showSubtitleSelectionPopup() // subtitles for online library
                        }
                        true
                    }
                    R.id.action_audio_tracks -> {
                        showAudioTrackSelectionPopup()
                        true
                    }
                    R.id.action_playlist -> {
                        player.pause()
                        player.release()
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    //Handling the Result of subtitle file picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SUBTITLE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { subtitleUri ->
                addSubtitleToPlayer(subtitleUri)
            }
        }
    }

    //Audio track selection
    @OptIn(UnstableApi::class)
    private fun showAudioTrackSelectionPopup() {
        val trackSelector = player.trackSelector as? DefaultTrackSelector ?: return
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return

        var foundAudioTracks = false

        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            if (player.getRendererType(rendererIndex) == C.TRACK_TYPE_AUDIO) {
                val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)

                if (trackGroups.length > 0) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Select Audio Track")

                    val audioTracks = mutableListOf<String>()
                    val trackGroup = trackGroups[0]
                    for (i in 0 until trackGroup.length) {
                        val format = trackGroup.getFormat(i)
                        audioTracks.add(format.language ?: "Track ${i + 1}")
                    }

                    builder.setItems(audioTracks.toTypedArray()) { _, which ->
                        val parametersBuilder = trackSelector.parameters.buildUpon()
                        parametersBuilder.setRendererDisabled(rendererIndex, false)
                        parametersBuilder.setSelectionOverride(
                            rendererIndex,
                            trackGroups,
                            DefaultTrackSelector.SelectionOverride(0, which)
                        )
                        trackSelector.parameters = parametersBuilder.build()
                    }
                    builder.show()
                    foundAudioTracks = true
                    break
                }
            }
        }

        if (!foundAudioTracks) {
            Toast.makeText(this, "No audio tracks available", Toast.LENGTH_SHORT).show()
        }
    }


    // File name for manual uploading subtitle
    private fun getSubtitleMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".srt", true) -> "application/x-subrip"
            fileName.endsWith(".vtt", true) -> "text/vtt"
            else -> "text/plain"
        }
    }

    // update seekbar + time
    private fun updateSeekBarVisuals(position: Long) {
        val duration = player.duration

        // Check if duration is valid and positive
        if (duration != C.TIME_UNSET && duration > 0) {
            // Clamp position within valid range
            val safePosition = position.coerceIn(0, duration)

            val progress = ((safePosition.toDouble() / duration) * 1000).toInt()
            seekBar.progress = progress
            currentTimeText.text = formatTime(safePosition)
        } else {
            // Optional: Handle case when duration is unknown
            seekBar.progress = 0
            currentTimeText.text = formatTime(0L)
        }
    }

    // Tooltip Hide Runnable
    private val hideTooltipRunnable = Runnable {
        gestureTooltip.visibility = View.GONE
    }

    // Manual uploading of subtitle handler
    @OptIn(UnstableApi::class)
    private fun addSubtitleToPlayer(subtitleUri: Uri) {
        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(getSubtitleMimeType(subtitleUri.toString()))
            .setLanguage("en")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null) {
            val newMediaItem = currentMediaItem.buildUpon()
                .setSubtitleConfigurations(listOf(subtitleConfig))
                .build()

            val currentPosition = player.currentPosition

            player.setMediaItem(newMediaItem)
            player.prepare()
            player.seekTo(currentPosition)
            player.play()
        }
    }

    private fun openSubtitleFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/x-subrip", "text/vtt", "text/plain"))
        }
        startActivityForResult(intent, SUBTITLE_PICKER_REQUEST_CODE)
    }


    // Subtitles function
    @OptIn(UnstableApi::class)
    private fun showSubtitleSelectionPopup() {
        val trackSelector = player.trackSelector as? DefaultTrackSelector ?: return
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return

        var foundSubtitles = false

        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)

            if (player.getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT && trackGroups.length > 0) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Select Subtitle")

                val overrides = mutableListOf<Pair<Int, Int>>()
                val subtitles = mutableListOf<String>()
                for (groupIndex in 0 until trackGroups.length) {
                    val group = trackGroups[groupIndex]
                    for (trackIndex in 0 until group.length) {
                        val format = group.getFormat(trackIndex)
                        val rawLabel = format.label ?: format.language ?: "Subtitle ${subtitles.size + 1}"
                        val label = when {
                            format.label != null -> format.label
                            format.language.equals("und", true) -> "Default"
                            format.language != null -> format.language!!.uppercase()
                            else -> "Subtitle ${subtitles.size + 1}"
                        }
                        if (label != null) {
                            subtitles.add(label)
                        }
                        overrides.add(groupIndex to trackIndex)
                    }
                }
                subtitles.add("Disable Subtitles")

                builder.setItems(subtitles.toTypedArray()) { _, which ->
                    val parametersBuilder = trackSelector.parameters.buildUpon()
                    if (which < overrides.size) {
                        val (groupIndex, trackIndex) = overrides[which]
                        parametersBuilder.setRendererDisabled(rendererIndex, false)
                        parametersBuilder.setSelectionOverride(
                            rendererIndex,
                            trackGroups,
                            DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
                        )
                    } else {
                        parametersBuilder.setRendererDisabled(rendererIndex, true)
                    }
                    trackSelector.parameters = parametersBuilder.build()
                }
                builder.show()
                foundSubtitles = true
                break
            }
        }

        if (!foundSubtitles) {
            Toast.makeText(this, "No subtitles available", Toast.LENGTH_SHORT).show()
        }
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

    private fun toggleDarkMode() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        )
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
        ev?.let {

            // Pass the event to both gesture detectors
            scaleGestureDetector.onTouchEvent(it)   // pinch-zoom gestures
            gestureDetector.onTouchEvent(it)        // video seekbar gestures

            // Reset gesture lock on finger lift
            if (it.action == MotionEvent.ACTION_UP || it.action == MotionEvent.ACTION_CANCEL) {
                gestureDirection = null
                scrubbing = false
                gestureTooltip.visibility = View.GONE
            }
        }
        return super.dispatchTouchEvent(ev)
    }

}
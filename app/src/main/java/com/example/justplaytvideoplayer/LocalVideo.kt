package com.example.justplaytvideoplayer

import android.net.Uri

data class LocalVideo(
    val title: String,
    val uri: Uri? = null,
)
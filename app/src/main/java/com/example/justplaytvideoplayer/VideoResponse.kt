package com.example.justplaytvideoplayer

import com.google.gson.annotations.SerializedName

data class VideoResponse(
    @SerializedName("categories")
    val videos: List<Video>
)

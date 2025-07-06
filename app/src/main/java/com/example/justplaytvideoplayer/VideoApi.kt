package com.example.justplaytvideoplayer

import retrofit2.Call
import retrofit2.http.GET

interface VideoApi {
    @GET("aaba791e2314e690d9926ac5110302cf/raw") // Relative path from base URL
    fun getVideos(): Call<VideoResponse>
}
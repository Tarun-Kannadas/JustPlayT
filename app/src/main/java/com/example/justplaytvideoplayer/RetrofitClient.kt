package com.example.justplaytvideoplayer

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://gist.githubusercontent.com/Tarun-Kannadas/" // Make sure your URL ends with '/'

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val instance: VideoApi = retrofit.create(VideoApi::class.java)
}

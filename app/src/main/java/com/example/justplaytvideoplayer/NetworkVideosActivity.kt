package com.example.justplaytvideoplayer

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.justplaytvideoplayer.databinding.ActivityNetworkVideosBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NetworkVideosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNetworkVideosBinding
    private lateinit var adapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkVideosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.videoRecyclerView.layoutManager = LinearLayoutManager(this)
        fetchVideos()

        val backButton: ImageButton = findViewById(R.id.backbtn)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchVideos() {
        RetrofitClient.instance.getVideos().enqueue(object : Callback<VideoResponse> {
            override fun onResponse(call: Call<VideoResponse>, response: Response<VideoResponse>) {
                if (response.isSuccessful) {
                    val networkVideoList = response.body()?.videos ?: emptyList()

                    if (networkVideoList.isNotEmpty()) {
                        adapter = VideoAdapter(this@NetworkVideosActivity, networkVideoList)
                        binding.videoRecyclerView.adapter = adapter
                    } else {
                        Toast.makeText(this@NetworkVideosActivity, "No videos found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@NetworkVideosActivity, "Response failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<VideoResponse>, t: Throwable) {
                Toast.makeText(this@NetworkVideosActivity, "Failed to load videos", Toast.LENGTH_SHORT).show()
            }
        })
    }

}

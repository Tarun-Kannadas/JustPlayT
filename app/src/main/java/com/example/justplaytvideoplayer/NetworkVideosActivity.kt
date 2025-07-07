package com.example.justplaytvideoplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
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
    private lateinit var networkReceiver: BroadcastReceiver
    private lateinit var offlineWarning: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkVideosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        offlineWarning = findViewById(R.id.offlineWarning)

        networkReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                val isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

                if (!isWifiConnected) {
                    offlineWarning.text = "Now you're Offline. Kindly turn on Wi-Fi to stream online."
                    offlineWarning.visibility = View.VISIBLE
                } else {
                    offlineWarning.visibility = View.GONE
                    fetchVideos()
                }
            }
        }

        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        binding.videoRecyclerView.layoutManager = LinearLayoutManager(this)

        fetchVideos()

        val backButton: ImageButton = findViewById(R.id.backbtn)
        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkReceiver)
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
                t.printStackTrace()
            }
        })
    }

}

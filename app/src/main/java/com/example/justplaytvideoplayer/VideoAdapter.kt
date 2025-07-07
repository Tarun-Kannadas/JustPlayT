package com.example.justplaytvideoplayer

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.justplaytvideoplayer.databinding.ItemVideoBinding

class VideoAdapter(
    private val context: Context,
    private val videos: List<Video>
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.binding.videoTitle.text = video.title

        holder.binding.root.setOnClickListener {
            val intent = Intent(context, MediaPlayerActivity::class.java)
            intent.putExtra("isNetwork", true)
            intent.putExtra("currentIndex", position)
            intent.putExtra("networkVideos", ArrayList(videos)) // Make Video implement Serializable
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = videos.size
}

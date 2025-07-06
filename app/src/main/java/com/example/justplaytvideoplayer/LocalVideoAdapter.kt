package com.example.justplaytvideoplayer

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.justplaytvideoplayer.databinding.ItemVideoBinding

class LocalVideoAdapter(
    private val context: Context,
    private val videos: List<LocalVideo>

) : RecyclerView.Adapter<LocalVideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.binding.videoTitle.text = video.title


        holder.binding.root.setOnClickListener {
            val uriList = videos.map { it.uri.toString() }
            val intent = Intent(context, MediaPlayerActivity::class.java)
            intent.putExtra("isNetwork", false)
            intent.putStringArrayListExtra("videoUris", ArrayList(uriList))
            intent.putExtra("currentIndex", position)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = videos.size
}

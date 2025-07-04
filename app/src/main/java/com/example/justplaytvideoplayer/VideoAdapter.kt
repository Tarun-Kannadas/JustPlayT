package com.example.justplaytvideoplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.justplaytvideoplayer.databinding.ItemVideoBinding

class VideoAdapter(

    private val context: Context,

    private val videos: List<Pair<String, Uri>>
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun getItemCount(): Int = videos.size

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val (title, uri) = videos[position]
        holder.binding.videoTitle.text = title

        holder.binding.root.setOnClickListener {
            val intent = Intent(context, MediaPlayerActivity::class.java)

            // Prepare full list of URIs
            val uriStrings = ArrayList<String>()
            videos.forEach { (_, videoUri) ->
                uriStrings.add(videoUri.toString())
            }

            intent.putStringArrayListExtra("videoUris", uriStrings)
            intent.putExtra("currentIndex", position)
            context.startActivity(intent)
        }
    }
}


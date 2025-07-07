package com.example.justplaytvideoplayer

import java.io.Serializable

data class Video (
    val description: String,
    val sources: List<String>,
    val subtitle: List<String>? = null,
    val thumb: String,
    val title: String) : Serializable
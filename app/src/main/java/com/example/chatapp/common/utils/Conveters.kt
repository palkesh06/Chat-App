package com.example.chatapp.common.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun isVideoUrl(url: String): Boolean {
    // Check if the URL ends with a video file extension
    val videoExtensions = listOf("mp4", "mov", "avi", "mkv", "webm", "flv")
    return videoExtensions.any { url.endsWith(it, ignoreCase = true) }
}

suspend fun getVideoThumbnail(videoUrl: String): Bitmap? =  withContext(Dispatchers.IO) {
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoUrl, HashMap())
        val bitmap = retriever.getFrameAtTime(1_000_000) // Get frame at 1 second
        retriever.release()
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
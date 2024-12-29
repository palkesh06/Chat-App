package com.example.chatapp.common.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


// Helper function to format the timestamp
fun getFormattedTimestamp(timestamp: Timestamp): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance()

    // Convert Firebase Timestamp to Date
    time.time = timestamp.toDate()

    val diffInMillis = now.timeInMillis - time.timeInMillis
    val diffInMinutes = diffInMillis / (60 * 1000)
    val diffInHours = diffInMillis / (60 * 60 * 1000)
    val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)

    return when {
        diffInMinutes < 1 -> "Just now"
        diffInMinutes < 60 -> "$diffInMinutes min ago"
        diffInHours < 24 -> "$diffInHours hr ago"
        diffInDays < 7 -> "$diffInDays days ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(time.time)
    }
}
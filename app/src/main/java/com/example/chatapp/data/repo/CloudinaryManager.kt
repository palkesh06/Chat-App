package com.example.chatapp.data.repo

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.chatapp.common.utils.isVideoUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CloudinaryManager(
) {
    private val cloudinary = Cloudinary(
        ObjectUtils.asMap(
            "cloud_name", "dphlsu76m",
            "api_key", "587864365714954",
            "api_secret", "yOyL3Lu30p_gstiPXUT1uQz4LAk"
        )
    )

    suspend fun uploadMedia(file: File): String? {
        return withContext(Dispatchers.IO) {
            try {
                val uploadOptions = if (isVideoUrl(file.name)) {
                    ObjectUtils.asMap("resource_type", "video")
                } else {
                    ObjectUtils.emptyMap()
                }
                val uploadResult = cloudinary.uploader().upload(file, uploadOptions)
                uploadResult["secure_url"] as? String // Return the uploaded file URL

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
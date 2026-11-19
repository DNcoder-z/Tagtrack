package com.tagtrack.nfc.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageManager {

    fun saveUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val photosDir = File(context.filesDir, "item_photos")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val destFile = File(photosDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteImageFile(context: Context?, photoUri: String?) {
        if (context == null || photoUri.isNullOrBlank()) return
        try {
            val path = when {
                photoUri.startsWith("file://") -> Uri.parse(photoUri).path
                photoUri.startsWith("/") -> photoUri
                else -> null
            }
            if (path != null) {
                val file = File(path)
                val photosDir = File(context.filesDir, "item_photos")
                if (file.exists() && file.canonicalPath.startsWith(photosDir.canonicalPath)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

package com.utils.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persistent media storage in app-specific directories.
 *
 * Responsibilities:
 * - Persist user-selected images to stable local files
 * - Compress oversized images to max 1440px width
 * - Generate collision-free filenames
 * - Return Coil-compatible file URIs
 * - Clean up temporary cache
 *
 * No external storage permissions required on Android 13+
 * since we use app-specific files dir.
 */
@Singleton
class MediaStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MEDIA_DIR = "chat_media"
        private const val MAX_WIDTH = 1440
        private const val MAX_HEIGHT = 1440
        private const val JPEG_QUALITY = 85
        private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB safety limit
    }

    private val mediaDir: File by lazy {
        File(context.filesDir, MEDIA_DIR).also { it.mkdirs() }
    }

    /**
     * Persists a user-selected image from a content URI to app-specific storage.
     *
     * @param uri Content URI from image picker (content://)
     * @return StoredMedia with stable local file URI and metadata
     * @throws IllegalArgumentException if URI cannot be opened or decoded
     * @throws java.io.IOException if disk write fails
     */
    suspend fun saveImage(uri: Uri): StoredMedia = withContext(Dispatchers.IO) {
        val mimeType = resolveMimeType(uri)
        val extension = extensionForMime(mimeType)
        val safeFilename = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
        val outputFile = File(mediaDir, safeFilename)

        // Decode with downsampling to avoid OOM
        val (bitmap, originalWidth, originalHeight) = decodeSampled(uri)

        // Compress if needed
        val (finalBitmap, needsRecycle) = if (bitmap.width > MAX_WIDTH || bitmap.height > MAX_HEIGHT) {
            val ratio = minOf(
                MAX_WIDTH.toFloat() / bitmap.width,
                MAX_HEIGHT.toFloat() / bitmap.height
            )
            val newWidth = (bitmap.width * ratio).toInt()
            val newHeight = (bitmap.height * ratio).toInt()
            bitmap.scale(newWidth, newHeight) to true
        } else {
            bitmap to false
        }

        // Write to app-specific storage
        val compressFormat = when (mimeType) {
            "image/png" -> Bitmap.CompressFormat.PNG
            "image/webp" -> Bitmap.CompressFormat.WEBP_LOSSY
            else -> Bitmap.CompressFormat.JPEG
        }
        val quality = if (mimeType == "image/png") 100 else JPEG_QUALITY

        FileOutputStream(outputFile).use { output ->
            finalBitmap.compress(compressFormat, quality, output)
        }

        val resultWidth = finalBitmap.width
        val resultHeight = finalBitmap.height

        // Clean up bitmaps
        if (needsRecycle) {
            bitmap.recycle()
        }
        finalBitmap.recycle()

        StoredMedia(
            localUri = outputFile.absolutePath,
            mimeType = mimeType,
            fileSize = outputFile.length(),
            width = resultWidth,
            height = resultHeight
        )
    }

    /**
     * Returns the absolute path for a saved media file.
     * Used by LiteRT inference which needs a file path, not a URI.
     */
    fun getFilePath(localUri: String): String = localUri

    /**
     * Deletes a specific media file.
     */
    suspend fun deleteMedia(localUri: String) = withContext(Dispatchers.IO) {
        File(localUri).takeIf { it.exists() }?.delete()
    }

    /**
     * Cleans up temporary image cache files (from old ImageUtils usage).
     */
    suspend fun cleanTempCache() = withContext(Dispatchers.IO) {
        context.cacheDir.listFiles()
            ?.filter { it.name.startsWith("img_") && it.name.endsWith(".jpg") }
            ?.forEach { it.delete() }
    }

    /**
     * Deletes all media files for cleanup (e.g., when clearing app data).
     */
    suspend fun clearAllMedia() = withContext(Dispatchers.IO) {
        mediaDir.listFiles()?.forEach { it.delete() }
    }

    // ─── Private Helpers ───────────────────────────────────────────

    private fun resolveMimeType(uri: Uri): String {
        val fromResolver = context.contentResolver.getType(uri)
        if (fromResolver != null) return fromResolver

        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
    }

    private fun extensionForMime(mimeType: String): String = when (mimeType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }

    /**
     * Decodes a URI with BitmapFactory sample size to avoid OOM on very large images.
     * Returns the decoded bitmap and original dimensions.
     */
    private fun decodeSampled(uri: Uri): Triple<Bitmap, Int, Int> {
        // First pass: get dimensions only
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // Calculate sample size for initial decode
        val sampleSize = calculateSampleSize(originalWidth, originalHeight, MAX_WIDTH, MAX_HEIGHT)

        // Second pass: decode with sample size
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalArgumentException("Cannot open or decode URI: $uri")

        return Triple(bitmap, originalWidth, originalHeight)
    }

    private fun calculateSampleSize(
        width: Int, height: Int,
        reqWidth: Int, reqHeight: Int
    ): Int {
        var sampleSize = 1
        if (width > reqWidth || height > reqHeight) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / sampleSize >= reqWidth && halfHeight / sampleSize >= reqHeight) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }
}

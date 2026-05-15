package com.utils.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale
import java.io.File

object ImageUtils {
    fun Uri.toBitmap(context: Context): Bitmap {
        return context.contentResolver.openInputStream(this)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        } ?: throw IllegalArgumentException("Cannot open or decode URI: $this")
    }

    fun Uri.toCompressedBytes(
            context: Context,
            maxWidth: Int = 512,
            maxHeight: Int = 512,
            quality: Int = 80
    ): ByteArray {

        val inputStream = context.contentResolver.openInputStream(this)
                ?: throw IllegalArgumentException("Cannot read URI data: $this")

        val bitmap = BitmapFactory.decodeStream(inputStream)
                ?: throw IllegalArgumentException("Failed to decode bitmap")

        // Maintain aspect ratio
        val ratio = minOf(
                maxWidth.toFloat() / bitmap.width,
                maxHeight.toFloat() / bitmap.height
        )

        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()

        val resizedBitmap = bitmap.scale(width, height)

        val outputStream = ByteArrayOutputStream()

        resizedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                outputStream
        )

        bitmap.recycle()

        if (bitmap != resizedBitmap) {
            resizedBitmap.recycle()
        }

        return outputStream.toByteArray()
    }

    fun Uri.toCompressedFile(
            context: Context,
            quality: Int = 80
    ): File {

        val inputStream = context.contentResolver.openInputStream(this)
                ?: throw IllegalArgumentException("Cannot open URI")

        val bitmap = BitmapFactory.decodeStream(inputStream)
                ?: throw IllegalArgumentException("Cannot decode bitmap")

        val resized = bitmap.scale(512, 512)

        val file = File(
                context.cacheDir,
                "img_${System.currentTimeMillis()}.jpg"
        )

        file.outputStream().use { output ->
            resized.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    output
            )
        }

        bitmap.recycle()

        if (bitmap != resized) {
            resized.recycle()
        }

        return file
    }
}
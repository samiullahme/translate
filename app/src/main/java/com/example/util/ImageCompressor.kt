package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageCompressor {
    private const val MAX_EDGE_PX = 1600
    private const val JPEG_QUALITY = 70
    /** API Ninjas free tier limit is 200KB; stay under with margin. */
    private const val MAX_BYTES = 180 * 1024

    suspend fun compressImage(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val is2 = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(is2)
            is2.close()

            if (originalBitmap == null) return@withContext null

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            val rotatedBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0,
                originalBitmap.width, originalBitmap.height,
                matrix, true
            )

            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            val largestEdge = max(width, height)

            val scaledBitmap = if (largestEdge > MAX_EDGE_PX) {
                val scale = MAX_EDGE_PX.toFloat() / largestEdge
                val newWidth = (width * scale).toInt()
                val newHeight = (height * scale).toInt()
                Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true)
            } else {
                rotatedBitmap
            }

            val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            var quality = JPEG_QUALITY
            do {
                FileOutputStream(tempFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                if (tempFile.length() <= MAX_BYTES || quality <= 40) break
                quality -= 10
            } while (true)

            return@withContext tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

package com.airbnb.utils.cloudinary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Pre-upload image compression utility.
 *
 * Reduces image file size before sending to Cloudinary, which:
 *  - Cuts upload time and bandwidth usage.
 *  - Prevents failures on very large images (some networks/devices limit request body size).
 *  - Keeps Cloudinary storage costs lower.
 *
 * Usage:
 *   val compressedUri = ImageCompressor.compress(context, originalUri)
 *   val result = CloudinaryManager.uploadImage(context, compressedUri)
 */
object ImageCompressor {

    // Default constraints (tuned for listing photos)
    private const val DEFAULT_MAX_WIDTH  = 1280   // px
    private const val DEFAULT_MAX_HEIGHT = 1280   // px
    private const val DEFAULT_QUALITY   = 82      // JPEG 0-100

    /**
     * Compresses an image [Uri] and writes the result to a temp file in [Context.cacheDir].
     *
     * Runs on [Dispatchers.IO] — safe to call from any coroutine context.
     *
     * @param context   Android context for content resolver + cache dir access.
     * @param uri       Source image URI (content:// or file://).
     * @param maxWidth  Maximum output width in pixels.
     * @param maxHeight Maximum output height in pixels.
     * @param quality   JPEG compression quality (0–100).
     * @return          A file:// [Uri] pointing to the compressed temp file.
     * @throws IllegalArgumentException if the URI cannot be decoded.
     */
    suspend fun compress(
        context: Context,
        uri: Uri,
        maxWidth: Int  = DEFAULT_MAX_WIDTH,
        maxHeight: Int = DEFAULT_MAX_HEIGHT,
        quality: Int   = DEFAULT_QUALITY
    ): Uri = withContext(Dispatchers.IO) {

        // 1. Decode bounds first (no memory allocation)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val originalWidth  = options.outWidth
        val originalHeight = options.outHeight

        require(originalWidth > 0 && originalHeight > 0) {
            "Unable to decode image dimensions from URI: $uri"
        }

        // 2. Calculate sample size (power-of-two down-scaling to reduce memory)
        options.inSampleSize = calculateSampleSize(originalWidth, originalHeight, maxWidth, maxHeight)
        options.inJustDecodeBounds = false

        val sampledBitmap: Bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")

        // 3. Fine-scale to exact target dimensions while preserving aspect ratio
        val scaledBitmap = scaleBitmap(sampledBitmap, maxWidth, maxHeight)
        if (scaledBitmap !== sampledBitmap) sampledBitmap.recycle()

        // 4. Write compressed JPEG to cache
        val outFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
        }
        scaledBitmap.recycle()

        Uri.fromFile(outFile)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Calculates the largest power-of-two sample size such that both
     * dimensions remain at least [reqWidth] × [reqHeight].
     */
    private fun calculateSampleSize(
        width: Int, height: Int,
        reqWidth: Int, reqHeight: Int
    ): Int {
        var sampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth  = width  / 2
            while (halfHeight / sampleSize >= reqHeight &&
                   halfWidth  / sampleSize >= reqWidth) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    /**
     * Scales the [bitmap] so that neither dimension exceeds the given max
     * while preserving the original aspect ratio.
     * Returns the original bitmap unchanged if no scaling is needed.
     */
    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height

        if (w <= maxWidth && h <= maxHeight) return bitmap

        val scale = minOf(maxWidth.toFloat() / w, maxHeight.toFloat() / h)
        val newW   = (w * scale).toInt()
        val newH   = (h * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newW, newH, /* filter= */ true)
    }
}

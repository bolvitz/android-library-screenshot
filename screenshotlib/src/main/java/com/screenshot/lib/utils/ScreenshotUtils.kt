package com.screenshot.lib.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utility functions for screenshot operations
 * Provides common transformations and sharing capabilities
 */
object ScreenshotUtils {

    /**
     * Resize a bitmap to specified dimensions
     *
     * @param bitmap The bitmap to resize
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @param maintainAspectRatio Whether to maintain aspect ratio (default: true)
     * @return Resized bitmap (original will not be recycled)
     */
    fun resize(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int,
        maintainAspectRatio: Boolean = true
    ): Bitmap {
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) {
            return bitmap
        }

        val (newWidth, newHeight) = if (maintainAspectRatio) {
            calculateAspectRatioSize(bitmap.width, bitmap.height, maxWidth, maxHeight)
        } else {
            Pair(maxWidth, maxHeight)
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Calculate new dimensions maintaining aspect ratio
     */
    private fun calculateAspectRatioSize(
        currentWidth: Int,
        currentHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        val aspectRatio = currentWidth.toFloat() / currentHeight.toFloat()

        return if (currentWidth > currentHeight) {
            // Landscape
            val newWidth = minOf(currentWidth, maxWidth)
            val newHeight = (newWidth / aspectRatio).toInt()
            Pair(newWidth, newHeight)
        } else {
            // Portrait
            val newHeight = minOf(currentHeight, maxHeight)
            val newWidth = (newHeight * aspectRatio).toInt()
            Pair(newWidth, newHeight)
        }
    }

    /**
     * Crop a bitmap to specified rectangle
     *
     * @param bitmap The bitmap to crop
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     * @param width Width of crop area
     * @param height Height of crop area
     * @return Cropped bitmap (original will not be recycled)
     */
    fun crop(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        val safeX = x.coerceIn(0, bitmap.width - 1)
        val safeY = y.coerceIn(0, bitmap.height - 1)
        val safeWidth = width.coerceAtMost(bitmap.width - safeX)
        val safeHeight = height.coerceAtMost(bitmap.height - safeY)

        return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth, safeHeight)
    }

    /**
     * Rotate a bitmap by specified degrees
     *
     * @param bitmap The bitmap to rotate
     * @param degrees Rotation angle in degrees (90, 180, 270, etc.)
     * @return Rotated bitmap (original will not be recycled)
     */
    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360 == 0f) {
            return bitmap
        }

        val matrix = Matrix().apply {
            postRotate(degrees)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Add a watermark text to bitmap
     *
     * @param bitmap The bitmap to watermark
     * @param text Watermark text
     * @param textSize Text size in pixels (default: 24f)
     * @param alpha Text alpha 0-255 (default: 128 for 50% transparency)
     * @param x X position (default: 10 from left)
     * @param y Y position (default: 10 from bottom)
     * @return New bitmap with watermark (original will not be recycled)
     */
    fun addWatermark(
        bitmap: Bitmap,
        text: String,
        textSize: Float = 24f,
        alpha: Int = 128,
        x: Float = 10f,
        y: Float? = null
    ): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val canvas = Canvas(result)

        val paint = Paint().apply {
            this.alpha = alpha.coerceIn(0, 255)
            this.textSize = textSize
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            setShadowLayer(2f, 1f, 1f, android.graphics.Color.BLACK)
        }

        val yPos = y ?: (bitmap.height - 10f)
        canvas.drawText(text, x, yPos, paint)

        return result
    }

    /**
     * Compare two bitmaps for similarity
     *
     * @param bitmap1 First bitmap
     * @param bitmap2 Second bitmap
     * @param sampleSize Sample every Nth pixel for performance (default: 10)
     * @return Similarity score from 0.0 (completely different) to 1.0 (identical)
     */
    fun compareBitmaps(
        bitmap1: Bitmap,
        bitmap2: Bitmap,
        sampleSize: Int = 10
    ): Float {
        // Different dimensions = different images
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            return 0f
        }

        var totalPixels = 0
        var matchingPixels = 0

        for (y in 0 until bitmap1.height step sampleSize) {
            for (x in 0 until bitmap1.width step sampleSize) {
                totalPixels++

                val pixel1 = bitmap1.getPixel(x, y)
                val pixel2 = bitmap2.getPixel(x, y)

                if (pixel1 == pixel2) {
                    matchingPixels++
                } else {
                    // Calculate color difference tolerance
                    val colorDiff = calculateColorDifference(pixel1, pixel2)
                    if (colorDiff < 30) { // Threshold for "similar enough"
                        matchingPixels++
                    }
                }
            }
        }

        return if (totalPixels > 0) {
            matchingPixels.toFloat() / totalPixels.toFloat()
        } else {
            0f
        }
    }

    /**
     * Calculate color difference between two pixels
     */
    private fun calculateColorDifference(color1: Int, color2: Int): Int {
        val r1 = android.graphics.Color.red(color1)
        val g1 = android.graphics.Color.green(color1)
        val b1 = android.graphics.Color.blue(color1)

        val r2 = android.graphics.Color.red(color2)
        val g2 = android.graphics.Color.green(color2)
        val b2 = android.graphics.Color.blue(color2)

        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2)
    }

    /**
     * Create a share intent for a screenshot file
     *
     * @param context The context
     * @param file The screenshot file to share
     * @param authority FileProvider authority (e.g., "com.yourapp.fileprovider")
     * @param title Share dialog title
     * @return Intent for sharing, or null if file doesn't exist
     */
    fun createShareIntent(
        context: Context,
        file: File,
        authority: String,
        title: String = "Share Screenshot"
    ): Intent? {
        if (!file.exists()) {
            return null
        }

        return try {
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Intent.createChooser(this, title)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Combine multiple bitmaps vertically
     *
     * @param bitmaps List of bitmaps to combine
     * @param spacing Spacing between bitmaps in pixels (default: 0)
     * @return Combined bitmap
     */
    fun combineVertically(bitmaps: List<Bitmap>, spacing: Int = 0): Bitmap? {
        if (bitmaps.isEmpty()) return null

        val width = bitmaps.maxOf { it.width }
        val totalHeight = bitmaps.sumOf { it.height } + (spacing * (bitmaps.size - 1))

        val result = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        var currentY = 0f
        bitmaps.forEach { bitmap ->
            canvas.drawBitmap(bitmap, 0f, currentY, null)
            currentY += bitmap.height + spacing
        }

        return result
    }

    /**
     * Combine multiple bitmaps horizontally
     *
     * @param bitmaps List of bitmaps to combine
     * @param spacing Spacing between bitmaps in pixels (default: 0)
     * @return Combined bitmap
     */
    fun combineHorizontally(bitmaps: List<Bitmap>, spacing: Int = 0): Bitmap? {
        if (bitmaps.isEmpty()) return null

        val totalWidth = bitmaps.sumOf { it.width } + (spacing * (bitmaps.size - 1))
        val height = bitmaps.maxOf { it.height }

        val result = Bitmap.createBitmap(totalWidth, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        var currentX = 0f
        bitmaps.forEach { bitmap ->
            canvas.drawBitmap(bitmap, currentX, 0f, null)
            currentX += bitmap.width + spacing
        }

        return result
    }

    /**
     * Get file size in human-readable format
     *
     * @param file The file to check
     * @return Size as string (e.g., "1.5 MB")
     */
    fun getReadableFileSize(file: File): String {
        if (!file.exists()) return "0 B"

        val bytes = file.length()
        val kilobyte = 1024L
        val megabyte = kilobyte * 1024
        val gigabyte = megabyte * 1024

        return when {
            bytes >= gigabyte -> String.format("%.2f GB", bytes.toFloat() / gigabyte)
            bytes >= megabyte -> String.format("%.2f MB", bytes.toFloat() / megabyte)
            bytes >= kilobyte -> String.format("%.2f KB", bytes.toFloat() / kilobyte)
            else -> "$bytes B"
        }
    }

    /**
     * Calculate bitmap memory size in bytes
     *
     * @param bitmap The bitmap
     * @return Memory size in bytes
     */
    fun getBitmapMemorySize(bitmap: Bitmap): Long {
        return (bitmap.rowBytes * bitmap.height).toLong()
    }
}

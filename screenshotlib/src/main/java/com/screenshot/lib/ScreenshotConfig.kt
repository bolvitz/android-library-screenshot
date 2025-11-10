package com.screenshot.lib

import android.graphics.Bitmap
import java.io.File

/**
 * Configuration class for screenshot capture
 *
 * @param format Image format (PNG, JPEG, WEBP)
 * @param quality Compression quality 0-100 (only affects JPEG/WEBP)
 * @param saveDirectory Custom directory for saving (null = default)
 * @param fileName Custom filename (null = auto-generated)
 * @param includeBackground Whether to include view background
 * @param recycleBitmapAfterSave Recycle bitmap after save to free memory (bitmap will be null in result)
 * @param returnBitmap Whether to return bitmap in result (false saves memory)
 */
data class ScreenshotConfig(
    val format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    val quality: Int = 100,
    val saveDirectory: File? = null,
    val fileName: String? = null,
    val includeBackground: Boolean = true,
    val recycleBitmapAfterSave: Boolean = false,
    val returnBitmap: Boolean = true
)

/**
 * Result of a screenshot capture operation
 */
sealed class ScreenshotResult {
    /**
     * @param file The saved screenshot file
     * @param bitmap The captured bitmap (may be null if recycleBitmapAfterSave=true or returnBitmap=false)
     */
    data class Success(val file: File, val bitmap: Bitmap?) : ScreenshotResult()
    data class Error(val exception: Exception, val message: String) : ScreenshotResult()
}

/**
 * Callback interface for screenshot operations
 */
interface ScreenshotCallback {
    /**
     * @param file The saved screenshot file
     * @param bitmap The captured bitmap (may be null if recycleBitmapAfterSave=true or returnBitmap=false)
     */
    fun onSuccess(file: File, bitmap: Bitmap?)
    fun onError(exception: Exception, message: String)
}

/**
 * Storage location options
 */
enum class StorageLocation {
    INTERNAL,  // App's private directory (no permissions needed)
    EXTERNAL   // Public directory (permissions required)
}

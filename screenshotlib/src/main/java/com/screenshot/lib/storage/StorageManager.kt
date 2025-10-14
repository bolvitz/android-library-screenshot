package com.screenshot.lib.storage

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import com.screenshot.lib.ScreenshotConfig
import com.screenshot.lib.StorageLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages storage operations for screenshots
 */
class StorageManager(private val context: Context) {

    companion object {
        private const val DEFAULT_FOLDER_NAME = "Screenshots"
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"

        // Thread-local SimpleDateFormat to avoid repeated creation
        // SimpleDateFormat is not thread-safe, so we use ThreadLocal
        private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat(DATE_FORMAT, Locale.US)
            }
        }
    }

    /**
     * Save bitmap to storage
     */
    suspend fun saveBitmap(
        bitmap: Bitmap,
        config: ScreenshotConfig,
        storageLocation: StorageLocation = StorageLocation.INTERNAL
    ): File = withContext(Dispatchers.IO) {
        val directory = getDirectory(config, storageLocation)
        val fileName = config.fileName ?: generateFileName(config.format)
        val file = File(directory, fileName)

        try {
            // Use BufferedOutputStream for better I/O performance
            java.io.BufferedOutputStream(FileOutputStream(file), 8192).use { outputStream ->
                bitmap.compress(config.format, config.quality, outputStream)
                // No manual flush needed - use() handles it
            }
            file
        } catch (e: IOException) {
            throw IOException("Failed to save screenshot: ${e.message}", e)
        }
    }

    /**
     * Get the directory where screenshots will be saved
     */
    private fun getDirectory(
        config: ScreenshotConfig,
        storageLocation: StorageLocation
    ): File {
        // If custom directory is specified, use it
        config.saveDirectory?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
            return it
        }

        // Otherwise, use default based on storage location
        return when (storageLocation) {
            StorageLocation.INTERNAL -> getInternalDirectory()
            StorageLocation.EXTERNAL -> getExternalDirectory()
        }
    }

    /**
     * Get app's private internal directory (no permissions needed)
     */
    private fun getInternalDirectory(): File {
        val directory = File(context.filesDir, DEFAULT_FOLDER_NAME)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    /**
     * Get external directory (may require permissions on older Android versions)
     */
    private fun getExternalDirectory(): File {
        val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use app-specific directory (no permissions needed on Android 10+)
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), DEFAULT_FOLDER_NAME)
        } else {
            // Use public directory (requires WRITE_EXTERNAL_STORAGE on Android < 10)
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                DEFAULT_FOLDER_NAME
            )
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    /**
     * Generate a unique file name with timestamp
     * Uses ThreadLocal SimpleDateFormat for better performance
     */
    private fun generateFileName(format: Bitmap.CompressFormat): String {
        // Use thread-local formatter to avoid creating new instance each time
        val timestamp = dateFormat.get()!!.format(Date())
        val extension = when (format) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.JPEG -> "jpg"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> "png"
        }
        return "screenshot_$timestamp.$extension"
    }

    /**
     * Delete a screenshot file
     */
    fun deleteScreenshot(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get all screenshots from default directory
     *
     * @param storageLocation Where to look for screenshots
     * @param limit Maximum number of files to return (default: all)
     * @param offset Number of files to skip (default: 0)
     */
    fun getAllScreenshots(
        storageLocation: StorageLocation = StorageLocation.INTERNAL,
        limit: Int = Int.MAX_VALUE,
        offset: Int = 0
    ): List<File> {
        val directory = when (storageLocation) {
            StorageLocation.INTERNAL -> getInternalDirectory()
            StorageLocation.EXTERNAL -> getExternalDirectory()
        }

        return directory.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(offset)
            ?.take(limit)
            ?: emptyList()
    }

    /**
     * Clear all screenshots from default directory
     */
    fun clearAllScreenshots(storageLocation: StorageLocation = StorageLocation.INTERNAL): Int {
        var deletedCount = 0
        getAllScreenshots(storageLocation).forEach { file ->
            if (file.delete()) {
                deletedCount++
            }
        }
        return deletedCount
    }
}

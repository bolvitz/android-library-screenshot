package com.screenshot.lib

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import com.screenshot.lib.utils.ViewDetector
import java.io.File

/**
 * Builder class for easy screenshot capture with fluent API
 *
 * Example usage:
 * ```
 * val builder = ScreenshotBuilder(context)
 * builder.view(myView)
 *     .format(Bitmap.CompressFormat.JPEG)
 *     .quality(85)
 *     .saveToInternal()
 *     .capture { file, bitmap ->
 *         // Success
 *     }
 *
 * // Don't forget to clean up when done
 * builder.release()
 * ```
 *
 * For memory optimization:
 * ```
 * ScreenshotBuilder(context)
 *     .view(myView)
 *     .recycleBitmapAfterSave(true)  // Auto-recycle to save memory
 *     .capture { file, bitmap ->
 *         // bitmap will be null here
 *     }
 * ```
 */
class ScreenshotBuilder(private val context: Context) {

    private var view: View? = null
    private var config = ScreenshotConfig()
    private var storageLocation = StorageLocation.INTERNAL
    private var autoDetect = false

    // Reuse ScreenshotCapture instance for better performance
    private val screenshotCapture: ScreenshotCapture by lazy {
        ScreenshotCapture(context)
    }

    /**
     * Set the view to capture
     */
    fun view(view: View): ScreenshotBuilder {
        this.view = view
        this.autoDetect = false
        return this
    }

    /**
     * Automatically detect and capture the best visible view.
     * Searches for media views (PlayerView, TextureView, WebView, VideoView, ImageView)
     * in priority order. Falls back to entire window if none found.
     *
     * Note: Requires Activity context
     */
    fun autoDetectView(): ScreenshotBuilder {
        this.autoDetect = true
        this.view = null
        return this
    }

    /**
     * Capture the entire window including status bar and navigation bar
     *
     * Note: Requires Activity context
     */
    fun captureWindow(): ScreenshotBuilder {
        if (context is Activity) {
            this.view = ViewDetector.getDecorView(context)
            this.autoDetect = false
        } else {
            throw IllegalStateException("captureWindow() requires Activity context")
        }
        return this
    }

    /**
     * Capture the content view (app content without system bars)
     *
     * Note: Requires Activity context
     */
    fun captureContent(): ScreenshotBuilder {
        if (context is Activity) {
            val contentView = ViewDetector.getContentView(context)
            if (contentView != null) {
                this.view = contentView
                this.autoDetect = false
            } else {
                throw IllegalStateException("Could not find content view")
            }
        } else {
            throw IllegalStateException("captureContent() requires Activity context")
        }
        return this
    }

    /**
     * Set the image format (PNG, JPEG, or WEBP)
     * Default: PNG
     */
    fun format(format: Bitmap.CompressFormat): ScreenshotBuilder {
        config = config.copy(format = format)
        return this
    }

    /**
     * Set the image quality (0-100)
     * Default: 100
     * Note: Quality only affects JPEG and WEBP formats
     */
    fun quality(quality: Int): ScreenshotBuilder {
        require(quality in 0..100) { "Quality must be between 0 and 100" }
        config = config.copy(quality = quality)
        return this
    }

    /**
     * Set a custom save directory
     */
    fun saveDirectory(directory: File): ScreenshotBuilder {
        config = config.copy(saveDirectory = directory)
        return this
    }

    /**
     * Set a custom file name
     */
    fun fileName(name: String): ScreenshotBuilder {
        config = config.copy(fileName = name)
        return this
    }

    /**
     * Include view background in the screenshot
     * Default: true
     */
    fun includeBackground(include: Boolean): ScreenshotBuilder {
        config = config.copy(includeBackground = include)
        return this
    }

    /**
     * Automatically recycle bitmap after saving to free memory
     * When enabled, the bitmap will be null in the result
     * Recommended for large screenshots or memory-constrained scenarios
     * Default: false
     */
    fun recycleBitmapAfterSave(recycle: Boolean): ScreenshotBuilder {
        config = config.copy(recycleBitmapAfterSave = recycle)
        return this
    }

    /**
     * Control whether to return bitmap in the result
     * If false, bitmap will be null in result and recycled after save
     * Useful when you only need the file, not the bitmap
     * Default: true
     */
    fun returnBitmap(returnBitmap: Boolean): ScreenshotBuilder {
        config = config.copy(returnBitmap = returnBitmap)
        return this
    }

    /**
     * Save to app's internal private directory (no permissions needed)
     * This is the default storage location
     */
    fun saveToInternal(): ScreenshotBuilder {
        storageLocation = StorageLocation.INTERNAL
        return this
    }

    /**
     * Save to external storage (may require permissions on Android < 10)
     */
    fun saveToExternal(): ScreenshotBuilder {
        storageLocation = StorageLocation.EXTERNAL
        return this
    }

    /**
     * Capture the screenshot and return only the bitmap (without saving)
     * Note: This is synchronous and may block the caller thread
     */
    fun captureBitmap(): Bitmap {
        val targetView = getTargetView()
        return screenshotCapture.captureBitmap(targetView, config.includeBackground)
    }

    /**
     * Capture the screenshot asynchronously and return only the bitmap (without saving)
     * This is the recommended method as it properly handles threading
     */
    suspend fun captureBitmapAsync(): Bitmap {
        val targetView = getTargetView()
        return screenshotCapture.captureBitmapAsync(targetView, config.includeBackground)
    }

    /**
     * Get the target view for capture, applying auto-detection if enabled
     */
    private fun getTargetView(): View {
        // If auto-detect is enabled, find the best view
        if (autoDetect) {
            if (context is Activity) {
                return ViewDetector.findBestViewToCapture(context)
            } else {
                throw IllegalStateException("Auto-detect requires Activity context")
            }
        }

        // Otherwise use the explicitly set view
        return view ?: throw IllegalStateException("View must be set before capturing (or use autoDetectView())")
    }

    /**
     * Capture and save the screenshot asynchronously with callback
     */
    fun capture(
        onSuccess: ((File, Bitmap?) -> Unit)? = null,
        onError: ((Exception, String) -> Unit)? = null
    ) {
        val targetView = getTargetView()

        screenshotCapture.captureAndSave(
            view = targetView,
            config = config,
            storageLocation = storageLocation,
            callback = object : ScreenshotCallback {
                override fun onSuccess(file: File, bitmap: Bitmap?) {
                    onSuccess?.invoke(file, bitmap)
                }

                override fun onError(exception: Exception, message: String) {
                    onError?.invoke(exception, message)
                }
            }
        )
    }

    /**
     * Capture and save the screenshot with a simple callback interface
     */
    fun capture(callback: ScreenshotCallback) {
        val targetView = getTargetView()

        screenshotCapture.captureAndSave(
            view = targetView,
            config = config,
            storageLocation = storageLocation,
            callback = callback
        )
    }

    /**
     * Capture and save the screenshot asynchronously using coroutines
     * Returns a ScreenshotResult with the file and optional bitmap
     */
    suspend fun captureAsync(): ScreenshotResult {
        val targetView = getTargetView()
        return screenshotCapture.captureAndSaveAsync(
            view = targetView,
            config = config,
            storageLocation = storageLocation
        )
    }

    /**
     * Release resources and cleanup
     * Call this when you're done using the builder
     * Important: After calling release(), this builder instance should not be used again
     */
    fun release() {
        screenshotCapture.cleanup()
    }

    companion object {
        /**
         * Quick capture - Take a screenshot with default settings
         */
        @JvmStatic
        fun quickCapture(
            context: Context,
            view: View,
            callback: ScreenshotCallback
        ) {
            ScreenshotBuilder(context)
                .view(view)
                .capture(callback)
        }

        /**
         * Quick capture bitmap - Get bitmap without saving
         */
        @JvmStatic
        fun quickCaptureBitmap(context: Context, view: View): Bitmap {
            return ScreenshotBuilder(context)
                .view(view)
                .captureBitmap()
        }

        /**
         * Check if storage permission is needed
         */
        @JvmStatic
        fun isPermissionNeeded(
            context: Context,
            isExternalStorage: Boolean
        ): Boolean {
            return com.screenshot.lib.permissions.PermissionHelper.isPermissionNeeded(
                context,
                isExternalStorage
            )
        }

        /**
         * Request storage permission
         */
        @JvmStatic
        fun requestPermission(activity: Activity) {
            com.screenshot.lib.permissions.PermissionHelper.requestStoragePermission(activity)
        }

        /**
         * Quick capture with auto-detection - Automatically finds and captures the best visible view
         *
         * @param activity The activity containing views to capture
         * @param callback Callback for success/error
         */
        @JvmStatic
        fun quickCaptureAuto(
            activity: Activity,
            callback: ScreenshotCallback
        ) {
            ScreenshotBuilder(activity)
                .autoDetectView()
                .capture(callback)
        }

        /**
         * Quick capture entire window - Captures full screen including system UI
         *
         * @param activity The activity to capture
         * @param callback Callback for success/error
         */
        @JvmStatic
        fun quickCaptureWindow(
            activity: Activity,
            callback: ScreenshotCallback
        ) {
            ScreenshotBuilder(activity)
                .captureWindow()
                .capture(callback)
        }

        /**
         * Quick capture content view - Captures app content without system bars
         *
         * @param activity The activity to capture
         * @param callback Callback for success/error
         */
        @JvmStatic
        fun quickCaptureContent(
            activity: Activity,
            callback: ScreenshotCallback
        ) {
            ScreenshotBuilder(activity)
                .captureContent()
                .capture(callback)
        }
    }
}

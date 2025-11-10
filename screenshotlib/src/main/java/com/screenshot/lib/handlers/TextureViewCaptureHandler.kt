package com.screenshot.lib.handlers

import android.graphics.Bitmap
import android.view.TextureView
import android.view.View
import com.screenshot.lib.utils.BitmapValidator
import com.screenshot.lib.utils.ViewValidator
import kotlinx.coroutines.delay

/**
 * Specialized handler for TextureView screenshots
 * TextureView is commonly used for video playback, camera preview, and OpenGL rendering
 *
 * Special considerations:
 * - Must use TextureView.getBitmap() instead of canvas drawing
 * - Requires TextureView to be available (surface created)
 * - May need stabilization delay for video frames
 * - Should validate bitmap isn't empty/black
 */
class TextureViewCaptureHandler(
    private val stabilizationDelayMs: Long = 200L,
    private val validateBitmap: Boolean = true
) : ViewCaptureHandler {

    override fun canHandle(view: View): Boolean = view is TextureView

    override fun captureBitmap(view: View, includeBackground: Boolean): Bitmap {
        val textureView = view as TextureView

        // Validate view is ready
        val validation = ViewValidator.validateViewWithDetails(textureView)
        if (!validation.isValid) {
            throw IllegalStateException("TextureView not ready for capture: ${validation.message}")
        }

        // Check if TextureView's SurfaceTexture is available
        if (!textureView.isAvailable) {
            throw IllegalStateException(
                "TextureView surface is not available. " +
                "The surface may be destroyed or not yet created."
            )
        }

        // Validate dimensions
        if (textureView.width <= 0 || textureView.height <= 0) {
            throw IllegalStateException(
                "TextureView has invalid dimensions: ${textureView.width}x${textureView.height}"
            )
        }

        // Capture bitmap from TextureView
        // Note: This must be called on the UI thread
        val bitmap = textureView.bitmap
            ?: throw IllegalStateException("TextureView.bitmap returned null - no frame available")

        // Validate the captured bitmap
        if (validateBitmap) {
            when {
                BitmapValidator.isBitmapEmpty(bitmap) -> {
                    bitmap.recycle()
                    throw IllegalStateException("Captured bitmap is empty (all pixels are the same)")
                }
                BitmapValidator.isBitmapBlack(bitmap) -> {
                    bitmap.recycle()
                    throw IllegalStateException("Captured bitmap is black (no video frame available)")
                }
                !BitmapValidator.hasColorVariation(bitmap) -> {
                    bitmap.recycle()
                    throw IllegalStateException("Captured bitmap lacks color variation (likely invalid frame)")
                }
            }
        }

        return bitmap
    }

    /**
     * Capture bitmap with automatic stabilization delay.
     * Useful for video playback where frames need time to render.
     *
     * This is a suspending function that should be called from a coroutine.
     *
     * @param view The TextureView to capture
     * @param includeBackground Not used for TextureView
     * @return Bitmap of the captured frame
     */
    suspend fun captureBitmapWithDelay(view: View, includeBackground: Boolean = true): Bitmap {
        val textureView = view as TextureView

        // Validate view is ready
        if (!ViewValidator.isViewVisible(textureView)) {
            throw IllegalStateException("TextureView is not visible")
        }

        if (!textureView.isAvailable) {
            throw IllegalStateException("TextureView surface is not available")
        }

        // Wait for frame to stabilize
        if (stabilizationDelayMs > 0) {
            delay(stabilizationDelayMs)
        }

        // Capture after delay
        return captureBitmap(textureView, includeBackground)
    }

    /**
     * Check if TextureView is ready for capture.
     *
     * @param textureView The TextureView to check
     * @return true if ready for capture
     */
    fun isReadyForCapture(textureView: TextureView): Boolean {
        if (!ViewValidator.isViewVisible(textureView)) return false
        if (!textureView.isAvailable) return false
        if (textureView.width <= 0 || textureView.height <= 0) return false
        return true
    }

    companion object {
        /**
         * Create a handler with custom settings
         */
        fun create(
            stabilizationDelayMs: Long = 200L,
            validateBitmap: Boolean = true
        ): TextureViewCaptureHandler {
            return TextureViewCaptureHandler(stabilizationDelayMs, validateBitmap)
        }

        /**
         * Create a handler for fast capture (no delay, no validation)
         * Use only when you're certain the frame is ready
         */
        fun createFast(): TextureViewCaptureHandler {
            return TextureViewCaptureHandler(0L, false)
        }

        /**
         * Create a handler with longer stabilization for slower devices
         */
        fun createStable(): TextureViewCaptureHandler {
            return TextureViewCaptureHandler(500L, true)
        }
    }
}

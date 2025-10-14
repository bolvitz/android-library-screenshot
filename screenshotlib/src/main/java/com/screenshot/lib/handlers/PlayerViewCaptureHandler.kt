package com.screenshot.lib.handlers

import android.graphics.Bitmap
import android.view.TextureView
import android.view.View
import com.screenshot.lib.utils.BitmapValidator
import com.screenshot.lib.utils.ViewValidator
import kotlinx.coroutines.delay

/**
 * Specialized handler for Media3 PlayerView screenshots
 * Captures video frames from AndroidX Media3 PlayerView
 *
 * Note: This handler requires androidx.media3:media3-ui dependency
 * If Media3 is not available, this handler won't be used.
 */
class PlayerViewCaptureHandler(
    private val stabilizationDelayMs: Long = 200L,
    private val validateBitmap: Boolean = true
) : ViewCaptureHandler {

    companion object {
        // Use string comparison to avoid hard dependency on Media3
        private const val PLAYER_VIEW_CLASS = "androidx.media3.ui.PlayerView"
    }

    override fun canHandle(view: View): Boolean {
        // Check if view is a PlayerView without requiring Media3 at compile time
        return view.javaClass.name == PLAYER_VIEW_CLASS ||
               view.javaClass.name.endsWith(".PlayerView")
    }

    override fun captureBitmap(view: View, includeBackground: Boolean): Bitmap {
        // Validate view is ready
        val validation = ViewValidator.validateViewWithDetails(view)
        if (!validation.isValid) {
            throw IllegalStateException("PlayerView not ready for capture: ${validation.message}")
        }

        // Extract TextureView from PlayerView
        val textureView = extractTextureView(view)
            ?: throw IllegalStateException(
                "Could not extract TextureView from PlayerView. " +
                "Video surface may not be initialized or is using SurfaceView."
            )

        // Check if TextureView is available
        if (!textureView.isAvailable) {
            throw IllegalStateException(
                "PlayerView's TextureView is not available. " +
                "Surface may be destroyed or video not yet started."
            )
        }

        // Validate dimensions
        if (textureView.width <= 0 || textureView.height <= 0) {
            throw IllegalStateException(
                "PlayerView's TextureView has invalid dimensions: ${textureView.width}x${textureView.height}"
            )
        }

        // Capture bitmap from TextureView
        val bitmap = textureView.bitmap
            ?: throw IllegalStateException("TextureView.bitmap returned null - no video frame available")

        // Validate the captured bitmap
        if (validateBitmap) {
            when {
                BitmapValidator.isBitmapEmpty(bitmap) -> {
                    bitmap.recycle()
                    throw IllegalStateException("Captured video frame is empty")
                }
                BitmapValidator.isBitmapBlack(bitmap) -> {
                    bitmap.recycle()
                    throw IllegalStateException("Captured video frame is black (video may not be playing)")
                }
            }
        }

        return bitmap
    }

    /**
     * Capture bitmap with automatic stabilization delay.
     * Recommended for video playback to ensure frame is fully rendered.
     *
     * @param view The PlayerView to capture
     * @param includeBackground Not used for PlayerView
     * @return Bitmap of the captured video frame
     */
    suspend fun captureBitmapWithDelay(view: View, includeBackground: Boolean = true): Bitmap {
        // Validate view is visible
        if (!ViewValidator.isViewVisible(view)) {
            throw IllegalStateException("PlayerView is not visible")
        }

        // Wait for frame to stabilize
        if (stabilizationDelayMs > 0) {
            delay(stabilizationDelayMs)
        }

        // Capture after delay
        return captureBitmap(view, includeBackground)
    }

    /**
     * Extract TextureView from PlayerView using reflection.
     * PlayerView can use either TextureView or SurfaceView.
     * We look for TextureView as it supports getBitmap().
     *
     * @param playerView The PlayerView instance
     * @return TextureView if found, null otherwise
     */
    private fun extractTextureView(playerView: View): TextureView? {
        try {
            // Try to access videoSurfaceView field
            val field = playerView.javaClass.getDeclaredField("videoSurfaceView")
            field.isAccessible = true
            val surfaceView = field.get(playerView)

            // Check if it's a TextureView
            if (surfaceView is TextureView) {
                return surfaceView
            }

            // If not directly accessible, search view hierarchy
            return findTextureViewInHierarchy(playerView)

        } catch (e: Exception) {
            // Fallback: search view hierarchy
            return findTextureViewInHierarchy(playerView)
        }
    }

    /**
     * Recursively search for TextureView in view hierarchy.
     *
     * @param view Root view to search from
     * @return First TextureView found, or null
     */
    private fun findTextureViewInHierarchy(view: View): TextureView? {
        if (view is TextureView) {
            return view
        }

        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val textureView = findTextureViewInHierarchy(child)
                if (textureView != null) {
                    return textureView
                }
            }
        }

        return null
    }

    /**
     * Check if PlayerView is ready for capture.
     *
     * @param playerView The PlayerView to check
     * @return true if ready for capture
     */
    fun isReadyForCapture(playerView: View): Boolean {
        if (!ViewValidator.isViewVisible(playerView)) return false

        val textureView = extractTextureView(playerView) ?: return false
        if (!textureView.isAvailable) return false
        if (textureView.width <= 0 || textureView.height <= 0) return false

        return true
    }

    /**
     * Check if PlayerView is currently playing video.
     * Note: This requires Media3 dependency at runtime.
     *
     * @param playerView The PlayerView to check
     * @return true if playing, false if paused/stopped or unable to determine
     */
    fun isPlaying(playerView: View): Boolean {
        return try {
            val method = playerView.javaClass.getMethod("getPlayer")
            val player = method.invoke(playerView) ?: return false

            val isPlayingMethod = player.javaClass.getMethod("isPlaying")
            isPlayingMethod.invoke(player) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
}

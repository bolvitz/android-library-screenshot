package com.screenshot.lib.handlers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.VideoView
import com.screenshot.lib.utils.ViewValidator

/**
 * Specialized handler for VideoView screenshots
 * Captures the current frame being displayed
 */
class VideoViewCaptureHandler : ViewCaptureHandler {

    override fun canHandle(view: View): Boolean = view is VideoView

    override fun captureBitmap(view: View, includeBackground: Boolean): Bitmap {
        val videoView = view as VideoView

        // Validate view visibility
        val validation = ViewValidator.validateViewWithDetails(videoView)
        if (!validation.isValid) {
            throw IllegalStateException("VideoView not ready for capture: ${validation.message}")
        }

        val width = videoView.width
        val height = videoView.height

        if (width <= 0 || height <= 0) {
            throw IllegalStateException("VideoView has invalid dimensions: ${width}x${height}")
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (includeBackground) {
            canvas.drawColor(Color.BLACK)  // Videos typically have black background
            videoView.background?.draw(canvas)
        }

        // Draw the VideoView directly using modern Canvas API
        videoView.draw(canvas)

        return bitmap
    }
}

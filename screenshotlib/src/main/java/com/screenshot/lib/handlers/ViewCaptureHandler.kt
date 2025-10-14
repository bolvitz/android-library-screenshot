package com.screenshot.lib.handlers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.TextureView
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.VideoView
import com.screenshot.lib.utils.ViewValidator

/**
 * Base interface for view capture handlers
 */
interface ViewCaptureHandler {
    fun canHandle(view: View): Boolean
    fun captureBitmap(view: View, includeBackground: Boolean = true): Bitmap
}

/**
 * Factory to get appropriate handler for a view
 *
 * Handlers are checked in priority order:
 * 1. PlayerView (Media3) - For video playback
 * 2. TextureView - For video/camera/OpenGL
 * 3. WebView - For web content
 * 4. VideoView - For simple video playback
 * 5. ImageView - For images
 * 6. Standard View - Fallback for all other views
 */
object ViewCaptureHandlerFactory {

    private val handlers = listOf(
        PlayerViewCaptureHandler(),   // Media3 PlayerView (highest priority for video)
        TextureViewCaptureHandler(),  // TextureView (video/camera/OpenGL)
        WebViewCaptureHandler(),      // WebView (web content)
        VideoViewCaptureHandler(),    // VideoView (simple video)
        ImageViewCaptureHandler(),    // ImageView (images)
        StandardViewCaptureHandler()  // Keep this last as fallback
    )

    /**
     * Get appropriate handler for a view
     *
     * @param view The view to capture
     * @param validateVisibility Whether to validate view visibility before capture (default true)
     * @return ViewCaptureHandler appropriate for the view type
     */
    fun getHandler(view: View, validateVisibility: Boolean = true): ViewCaptureHandler {
        // Validate view visibility if requested
        if (validateVisibility) {
            val validation = ViewValidator.validateViewWithDetails(view)
            if (!validation.isValid) {
                throw IllegalStateException("View not ready for capture: ${validation.message}")
            }
        }

        return handlers.firstOrNull { it.canHandle(view) }
            ?: StandardViewCaptureHandler()
    }

    /**
     * Get all handlers that can handle this view type
     * Useful for debugging or custom selection
     *
     * @param view The view to check
     * @return List of compatible handlers
     */
    fun getCompatibleHandlers(view: View): List<ViewCaptureHandler> {
        return handlers.filter { it.canHandle(view) }
    }
}

/**
 * Handler for standard Android views (TextView, Button, LinearLayout, etc.)
 * This is the fallback handler for all views that don't have specialized handlers
 */
class StandardViewCaptureHandler : ViewCaptureHandler {

    override fun canHandle(view: View): Boolean = true

    override fun captureBitmap(view: View, includeBackground: Boolean): Bitmap {
        // Validate view
        val validation = ViewValidator.validateViewWithDetails(view)
        if (!validation.isValid) {
            throw IllegalStateException("View not ready for capture: ${validation.message}")
        }

        // Ensure view is laid out
        if (view.width == 0 || view.height == 0) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }

        val width = view.width
        val height = view.height

        if (width <= 0 || height <= 0) {
            throw IllegalStateException("View has invalid dimensions: ${width}x${height}")
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (includeBackground) {
            canvas.drawColor(Color.WHITE)
            view.background?.draw(canvas)
        }

        view.draw(canvas)
        return bitmap
    }
}

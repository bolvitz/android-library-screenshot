package com.screenshot.lib.handlers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.view.View
import android.webkit.WebView
import com.screenshot.lib.utils.ViewValidator

/**
 * Specialized handler for WebView screenshots
 */
class WebViewCaptureHandler : ViewCaptureHandler {

    override fun canHandle(view: View): Boolean = view is WebView

    override fun captureBitmap(view: View, includeBackground: Boolean): Bitmap {
        val webView = view as WebView

        // Validate view visibility
        val validation = ViewValidator.validateViewWithDetails(webView)
        if (!validation.isValid) {
            throw IllegalStateException("WebView not ready for capture: ${validation.message}")
        }

        // Enable drawing cache for better WebView capture
        val wasDrawingCacheEnabled = webView.isDrawingCacheEnabled

        try {
            // Measure the full content height of the WebView
            val width = webView.width
            val height = webView.contentHeight

            if (width <= 0 || height <= 0) {
                throw IllegalStateException("WebView has invalid dimensions: ${width}x${height}")
            }

            // Create bitmap for the entire content
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            if (includeBackground) {
                canvas.drawColor(Color.WHITE)
                webView.background?.draw(canvas)
            }

            // Save the current scroll position
            val scrollX = webView.scrollX
            val scrollY = webView.scrollY

            // Scroll to top to capture full content
            webView.scrollTo(0, 0)

            // Draw the WebView content
            webView.draw(canvas)

            // Restore scroll position
            webView.scrollTo(scrollX, scrollY)

            return bitmap

        } finally {
            // Restore drawing cache state
            webView.isDrawingCacheEnabled = wasDrawingCacheEnabled
        }
    }

    /**
     * Capture only the visible portion of the WebView (not the full content)
     */
    fun captureVisibleArea(webView: WebView, includeBackground: Boolean = true): Bitmap {
        val width = webView.width
        val height = webView.height

        if (width <= 0 || height <= 0) {
            throw IllegalStateException("WebView has invalid dimensions: ${width}x${height}")
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (includeBackground) {
            canvas.drawColor(Color.WHITE)
            webView.background?.draw(canvas)
        }

        webView.draw(canvas)
        return bitmap
    }
}

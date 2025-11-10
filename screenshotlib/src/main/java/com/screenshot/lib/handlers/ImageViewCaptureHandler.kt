package com.screenshot.lib.handlers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.screenshot.lib.utils.ViewValidator

/**
 * Specialized handler for ImageView screenshots
 * Optimized to directly capture the drawable when possible
 */
class ImageViewCaptureHandler : ViewCaptureHandler {

    override fun canHandle(view: View): Boolean = view is ImageView

    override fun captureBitmap(view: View, includeBackground: Boolean): Bitmap {
        val imageView = view as ImageView

        // Validate view visibility
        val validation = ViewValidator.validateViewWithDetails(imageView)
        if (!validation.isValid) {
            throw IllegalStateException("ImageView not ready for capture: ${validation.message}")
        }

        val drawable = imageView.drawable

        // If drawable is a bitmap, we can optimize
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return captureBitmapDrawable(imageView, drawable, includeBackground)
        }

        // For other drawables, use standard capture
        return captureDrawable(imageView, drawable, includeBackground)
    }

    private fun captureBitmapDrawable(
        imageView: ImageView,
        drawable: BitmapDrawable,
        includeBackground: Boolean
    ): Bitmap {
        val sourceBitmap = drawable.bitmap

        // Use view dimensions
        val width = imageView.width
        val height = imageView.height

        if (width <= 0 || height <= 0) {
            // Fallback to bitmap dimensions
            return sourceBitmap.copy(sourceBitmap.config ?: Bitmap.Config.ARGB_8888, false)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (includeBackground) {
            canvas.drawColor(Color.WHITE)
            imageView.background?.draw(canvas)
        }

        // Draw the image respecting ScaleType
        imageView.draw(canvas)

        return bitmap
    }

    private fun captureDrawable(
        imageView: ImageView,
        drawable: Drawable?,
        includeBackground: Boolean
    ): Bitmap {
        val width = imageView.width
        val height = imageView.height

        if (width <= 0 || height <= 0) {
            throw IllegalStateException("ImageView has invalid dimensions: ${width}x${height}")
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (includeBackground) {
            canvas.drawColor(Color.WHITE)
            imageView.background?.draw(canvas)
        }

        drawable?.let {
            it.setBounds(0, 0, width, height)
            it.draw(canvas)
        }

        return bitmap
    }
}

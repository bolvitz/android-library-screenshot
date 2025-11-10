package com.screenshot.lib.utils

import android.graphics.Bitmap

/**
 * Utility class for validating bitmaps
 * Detects empty, black, or invalid screenshots
 */
object BitmapValidator {

    /**
     * Check if a bitmap is empty (all pixels are the same color).
     * This detects black screens or uninitialized captures.
     *
     * @param bitmap The bitmap to check
     * @return true if bitmap appears empty
     */
    fun isBitmapEmpty(bitmap: Bitmap?): Boolean {
        if (bitmap == null) return true

        return try {
            // Create a blank bitmap with same dimensions and config
            val emptyBitmap = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                bitmap.config ?: Bitmap.Config.ARGB_8888
            )

            // Compare if they're identical
            val isEmpty = bitmap.sameAs(emptyBitmap)

            // Clean up
            emptyBitmap.recycle()

            isEmpty
        } catch (e: Exception) {
            // If we can't determine, assume it's valid to avoid false negatives
            false
        }
    }

    /**
     * Check if a bitmap is completely black (all pixels are black).
     * This is useful for detecting video frames that haven't loaded yet.
     *
     * OPTIMIZED: Uses getPixels() with IntArray for better performance
     *
     * @param bitmap The bitmap to check
     * @param sampleSize Sample every Nth pixel (default 10 for performance)
     * @return true if bitmap appears to be all black
     */
    fun isBitmapBlack(bitmap: Bitmap?, sampleSize: Int = 10): Boolean {
        if (bitmap == null) return true

        return try {
            val width = bitmap.width
            val height = bitmap.height

            if (width <= 0 || height <= 0) return true

            // Calculate sampled dimensions
            val sampledWidth = (width + sampleSize - 1) / sampleSize
            val sampledHeight = (height + sampleSize - 1) / sampleSize
            val pixelCount = sampledWidth * sampledHeight

            // Use IntArray for batch pixel access (much faster than getPixel())
            val pixels = IntArray(pixelCount)
            var index = 0

            // Sample pixels in a grid pattern
            for (y in 0 until height step sampleSize) {
                for (x in 0 until width step sampleSize) {
                    if (index < pixelCount) {
                        pixels[index++] = bitmap.getPixel(x, y)
                    }
                }
            }

            // Count black pixels
            var blackPixelCount = 0
            for (pixel in pixels) {
                // Check if pixel is black or transparent
                if (pixel == 0 || pixel == -16777216) { // 0 = transparent, -16777216 = black
                    blackPixelCount++
                }
            }

            // If more than 95% of sampled pixels are black, consider it a black bitmap
            blackPixelCount.toFloat() / pixelCount > 0.95f

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Comprehensive validation to check if a bitmap is valid for use.
     * Checks for null, invalid dimensions, empty, and black bitmaps.
     *
     * @param bitmap The bitmap to validate
     * @param checkEmpty Whether to check if bitmap is empty (default true)
     * @param checkBlack Whether to check if bitmap is all black (default true)
     * @return true if bitmap is valid and usable
     */
    fun isValidBitmap(
        bitmap: Bitmap?,
        checkEmpty: Boolean = true,
        checkBlack: Boolean = true
    ): Boolean {
        if (bitmap == null) return false

        // Check dimensions
        if (bitmap.width <= 0 || bitmap.height <= 0) return false

        // Check if bitmap is recycled
        if (bitmap.isRecycled) return false

        // Check if empty
        if (checkEmpty && isBitmapEmpty(bitmap)) return false

        // Check if all black
        if (checkBlack && isBitmapBlack(bitmap)) return false

        return true
    }

    /**
     * Get a confidence score for bitmap quality (0.0 to 1.0).
     * Higher score means better quality/validity.
     *
     * @param bitmap The bitmap to score
     * @return Quality score from 0.0 (invalid/poor) to 1.0 (good)
     */
    fun getBitmapQualityScore(bitmap: Bitmap?): Float {
        if (bitmap == null) return 0f

        var score = 1.0f

        // Invalid dimensions
        if (bitmap.width <= 0 || bitmap.height <= 0) return 0f

        // Recycled bitmap
        if (bitmap.isRecycled) return 0f

        // Penalize if empty
        if (isBitmapEmpty(bitmap)) score -= 0.5f

        // Penalize if mostly black
        if (isBitmapBlack(bitmap)) score -= 0.3f

        return score.coerceIn(0f, 1f)
    }

    /**
     * Check if bitmap has sufficient color variation (not monochrome).
     * Samples pixels and calculates color diversity.
     *
     * OPTIMIZED: Uses batch pixel sampling for better performance
     *
     * @param bitmap The bitmap to check
     * @param sampleSize Sample every Nth pixel (default 20)
     * @param minUniqueColors Minimum unique colors to consider valid (default 10)
     * @return true if bitmap has sufficient color variation
     */
    fun hasColorVariation(
        bitmap: Bitmap?,
        sampleSize: Int = 20,
        minUniqueColors: Int = 10
    ): Boolean {
        if (bitmap == null) return false

        return try {
            val width = bitmap.width
            val height = bitmap.height

            if (width <= 0 || height <= 0) return false

            // Calculate sample count
            val sampledWidth = (width + sampleSize - 1) / sampleSize
            val sampledHeight = (height + sampleSize - 1) / sampleSize
            val maxSamples = sampledWidth * sampledHeight

            // Use smaller buffer for early exit optimization
            val batchSize = minOf(maxSamples, 100)
            val pixels = IntArray(batchSize)
            val uniqueColors = mutableSetOf<Int>()

            var index = 0
            var samplesCollected = 0

            // Sample pixels in batches
            outer@ for (y in 0 until height step sampleSize) {
                for (x in 0 until width step sampleSize) {
                    if (index < batchSize) {
                        pixels[index++] = bitmap.getPixel(x, y)
                        samplesCollected++

                        // Process batch when full or at end
                        if (index == batchSize || samplesCollected == maxSamples) {
                            // Add to unique colors set
                            for (i in 0 until index) {
                                uniqueColors.add(pixels[i])

                                // Early exit if we found enough variety
                                if (uniqueColors.size >= minUniqueColors) {
                                    return true
                                }
                            }

                            // Reset for next batch
                            index = 0

                            // Exit if we've sampled everything
                            if (samplesCollected >= maxSamples) {
                                break@outer
                            }
                        }
                    }
                }
            }

            uniqueColors.size >= minUniqueColors

        } catch (e: Exception) {
            false
        }
    }
}

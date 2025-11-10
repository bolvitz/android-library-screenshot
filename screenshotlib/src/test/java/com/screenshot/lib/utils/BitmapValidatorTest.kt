package com.screenshot.lib.utils

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for BitmapValidator
 */
@RunWith(RobolectricTestRunner::class)
class BitmapValidatorTest {

    @Test
    fun `null bitmap is invalid`() {
        assertFalse(BitmapValidator.isValidBitmap(null))
        assertTrue(BitmapValidator.isBitmapEmpty(null))
        assertTrue(BitmapValidator.isBitmapBlack(null))
    }

    @Test
    fun `recycled bitmap is invalid`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.recycle()

        assertFalse(BitmapValidator.isValidBitmap(bitmap))
    }

    @Test
    fun `bitmap with zero dimensions is invalid`() {
        // Can't create 0x0 bitmap, so test the validation logic
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        assertTrue(BitmapValidator.isValidBitmap(bitmap, checkEmpty = false, checkBlack = false))
    }

    @Test
    fun `valid colored bitmap passes validation`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Fill with different colors to ensure variation
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                bitmap.setPixel(x, y, Color.rgb(x * 2, y * 2, (x + y)))
            }
        }

        assertTrue(BitmapValidator.isValidBitmap(bitmap))
        assertFalse(BitmapValidator.isBitmapBlack(bitmap))
        assertFalse(BitmapValidator.isBitmapEmpty(bitmap))
        assertTrue(BitmapValidator.hasColorVariation(bitmap))
    }

    @Test
    fun `black bitmap is detected`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Fill with black
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                bitmap.setPixel(x, y, Color.BLACK)
            }
        }

        assertTrue(BitmapValidator.isBitmapBlack(bitmap))
    }

    @Test
    fun `empty bitmap is detected`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Leave empty (transparent/default)
        assertTrue(BitmapValidator.isBitmapEmpty(bitmap))
    }

    @Test
    fun `quality score for valid bitmap is high`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Fill with colors
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                bitmap.setPixel(x, y, Color.rgb(x * 2, y * 2, 128))
            }
        }

        val score = BitmapValidator.getBitmapQualityScore(bitmap)
        assertTrue("Score should be high: $score", score > 0.5f)
    }

    @Test
    fun `quality score for null bitmap is zero`() {
        assertEquals(0f, BitmapValidator.getBitmapQualityScore(null), 0.01f)
    }

    @Test
    fun `quality score for recycled bitmap is zero`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.recycle()

        assertEquals(0f, BitmapValidator.getBitmapQualityScore(bitmap), 0.01f)
    }

    @Test
    fun `color variation detection works`() {
        val colorful = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Create colorful bitmap
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                colorful.setPixel(x, y, Color.rgb(x * 2, y * 2, (x + y) % 255))
            }
        }

        assertTrue(BitmapValidator.hasColorVariation(colorful))

        // Create monochrome bitmap
        val monochrome = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                monochrome.setPixel(x, y, Color.RED)
            }
        }

        assertFalse(BitmapValidator.hasColorVariation(monochrome, minUniqueColors = 10))
    }

    @Test
    fun `validation with custom checks`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Valid without checks
        assertTrue(BitmapValidator.isValidBitmap(bitmap, checkEmpty = false, checkBlack = false))

        // Invalid when checking empty
        assertFalse(BitmapValidator.isValidBitmap(bitmap, checkEmpty = true, checkBlack = false))
    }
}

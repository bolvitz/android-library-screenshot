package com.screenshot.lib

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Unit tests for ScreenshotResult sealed class
 */
@RunWith(RobolectricTestRunner::class)
class ScreenshotResultTest {

    @Test
    fun `success result contains file and bitmap`() {
        val file = File("/test/screenshot.png")
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val result = ScreenshotResult.Success(file, bitmap)

        assertEquals(file, result.file)
        assertEquals(bitmap, result.bitmap)
    }

    @Test
    fun `success result can have null bitmap`() {
        val file = File("/test/screenshot.png")

        val result = ScreenshotResult.Success(file, null)

        assertEquals(file, result.file)
        assertNull(result.bitmap)
    }

    @Test
    fun `error result contains exception and message`() {
        val exception = RuntimeException("Test error")
        val message = "Failed to capture"

        val result = ScreenshotResult.Error(exception, message)

        assertEquals(exception, result.exception)
        assertEquals(message, result.message)
    }

    @Test
    fun `results can be used in when expression`() {
        val successResult: ScreenshotResult = ScreenshotResult.Success(
            File("/test.png"),
            null
        )
        val errorResult: ScreenshotResult = ScreenshotResult.Error(
            Exception(),
            "Error"
        )

        val successHandled = when (successResult) {
            is ScreenshotResult.Success -> true
            is ScreenshotResult.Error -> false
        }

        val errorHandled = when (errorResult) {
            is ScreenshotResult.Success -> false
            is ScreenshotResult.Error -> true
        }

        assertTrue(successHandled)
        assertTrue(errorHandled)
    }
}

package com.screenshot.lib

import android.app.Activity
import android.graphics.Bitmap
import android.widget.TextView
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ScreenshotBuilder
 */
@RunWith(RobolectricTestRunner::class)
class ScreenshotBuilderTest {

    private lateinit var activity: Activity
    private lateinit var builder: ScreenshotBuilder
    private lateinit var testView: TextView

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java)
            .create()
            .start()
            .resume()
            .get()

        builder = ScreenshotBuilder(activity)

        testView = TextView(activity).apply {
            layout(0, 0, 100, 100)
            text = "Test"
        }
    }

    @Test
    fun `builder can set view`() {
        val result = builder.view(testView)

        assertSame(builder, result) // Check fluent API
    }

    @Test
    fun `builder can set format`() {
        val result = builder.format(Bitmap.CompressFormat.JPEG)

        assertSame(builder, result)
    }

    @Test
    fun `builder can set quality`() {
        val result = builder.quality(85)

        assertSame(builder, result)
    }

    @Test
    fun `builder rejects invalid quality`() {
        try {
            builder.quality(-1)
            fail("Should reject negative quality")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("between 0 and 100") == true)
        }

        try {
            builder.quality(101)
            fail("Should reject quality > 100")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("between 0 and 100") == true)
        }
    }

    @Test
    fun `builder can set fileName`() {
        val result = builder.fileName("my_screenshot.png")

        assertSame(builder, result)
    }

    @Test
    fun `builder can set includeBackground`() {
        val result = builder.includeBackground(false)

        assertSame(builder, result)
    }

    @Test
    fun `builder can set recycleBitmapAfterSave`() {
        val result = builder.recycleBitmapAfterSave(true)

        assertSame(builder, result)
    }

    @Test
    fun `builder can set returnBitmap`() {
        val result = builder.returnBitmap(false)

        assertSame(builder, result)
    }

    @Test
    fun `builder can set storage location`() {
        val internalResult = builder.saveToInternal()
        assertSame(builder, internalResult)

        val externalResult = builder.saveToExternal()
        assertSame(builder, externalResult)
    }

    @Test
    fun `builder can capture window`() {
        val result = builder.captureWindow()

        assertSame(builder, result)
    }

    @Test
    fun `builder can capture content`() {
        val result = builder.captureContent()

        assertSame(builder, result)
    }

    @Test
    fun `builder can capture bitmap synchronously`() {
        builder.view(testView)

        val bitmap = builder.captureBitmap()

        assertNotNull(bitmap)
        assertEquals(100, bitmap.width)
        assertEquals(100, bitmap.height)
    }

    @Test
    fun `builder can capture bitmap asynchronously`() = runTest {
        builder.view(testView)

        val bitmap = builder.captureBitmapAsync()

        assertNotNull(bitmap)
        assertEquals(100, bitmap.width)
        assertEquals(100, bitmap.height)
    }

    @Test
    fun `builder throws when no view is set`() {
        try {
            builder.captureBitmap()
            fail("Should throw when no view is set")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("View must be set") == true)
        }
    }

    @Test
    fun `builder can capture and save with callback`() = runTest {
        builder.view(testView)
            .fileName("test_callback.png")

        var successCalled = false
        var capturedFile: java.io.File? = null

        builder.capture(
            onSuccess = { file, bitmap ->
                successCalled = true
                capturedFile = file
            },
            onError = { _, _ ->
                fail("Should not call onError")
            }
        )

        // Wait a bit for async operation
        Thread.sleep(500)

        assertTrue(successCalled)
        assertNotNull(capturedFile)
        assertTrue(capturedFile?.exists() == true)

        // Cleanup
        capturedFile?.delete()
    }

    @Test
    fun `builder can capture and save async`() = runTest {
        builder.view(testView)
            .fileName("test_async.png")

        val result = builder.captureAsync()

        assertTrue(result is ScreenshotResult.Success)

        val successResult = result as ScreenshotResult.Success
        assertTrue(successResult.file.exists())

        // Cleanup
        successResult.file.delete()
    }

    @Test
    fun `quickCaptureBitmap works`() {
        val bitmap = ScreenshotBuilder.quickCaptureBitmap(activity, testView)

        assertNotNull(bitmap)
        assertEquals(100, bitmap.width)
        assertEquals(100, bitmap.height)
    }

    @Test
    fun `autoDetectView enables auto detection`() {
        val result = builder.autoDetectView()

        assertSame(builder, result)

        // Should capture decorView since no special views are present
        val bitmap = builder.captureBitmap()
        assertNotNull(bitmap)
    }

    @Test
    fun `release cleans up resources`() {
        builder.view(testView)
        builder.captureBitmap()

        // Should not throw
        builder.release()
    }

    @Test
    fun `fluent API chain works`() = runTest {
        val result = builder
            .view(testView)
            .format(Bitmap.CompressFormat.JPEG)
            .quality(85)
            .fileName("fluent_test.jpg")
            .includeBackground(true)
            .saveToInternal()
            .captureAsync()

        assertTrue(result is ScreenshotResult.Success)

        val successResult = result as ScreenshotResult.Success
        assertEquals("fluent_test.jpg", successResult.file.name)

        // Cleanup
        successResult.file.delete()
        builder.release()
    }

    @Test
    fun `builder with recycle option returns null bitmap`() = runTest {
        val result = builder
            .view(testView)
            .fileName("recycle_test.png")
            .recycleBitmapAfterSave(true)
            .captureAsync()

        assertTrue(result is ScreenshotResult.Success)

        val successResult = result as ScreenshotResult.Success
        assertNull(successResult.bitmap) // Should be null due to recycling

        // Cleanup
        successResult.file.delete()
        builder.release()
    }

    @Test
    fun `quickCaptureWindow works`() {
        var successCalled = false

        ScreenshotBuilder.quickCaptureWindow(activity, object : ScreenshotCallback {
            override fun onSuccess(file: java.io.File, bitmap: Bitmap?) {
                successCalled = true
                file.delete()
            }

            override fun onError(exception: Exception, message: String) {
                fail("Should not error")
            }
        })

        Thread.sleep(500)
        assertTrue(successCalled)
    }

    @Test
    fun `quickCaptureContent works`() {
        var successCalled = false

        ScreenshotBuilder.quickCaptureContent(activity, object : ScreenshotCallback {
            override fun onSuccess(file: java.io.File, bitmap: Bitmap?) {
                successCalled = true
                file.delete()
            }

            override fun onError(exception: Exception, message: String) {
                fail("Should not error")
            }
        })

        Thread.sleep(500)
        assertTrue(successCalled)
    }
}

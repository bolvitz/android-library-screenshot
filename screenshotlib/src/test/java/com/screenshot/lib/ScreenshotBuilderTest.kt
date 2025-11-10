package com.screenshot.lib

import android.app.Activity
import android.graphics.Bitmap
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Unit tests for ScreenshotBuilder
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScreenshotBuilderTest {

    private lateinit var activity: Activity
    private lateinit var builder: ScreenshotBuilder
    private lateinit var testView: TextView
    private lateinit var rootView: FrameLayout
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        // Set up unconfined test dispatcher for instant coroutine execution
        Dispatchers.setMain(testDispatcher)
        activity = Robolectric.buildActivity(Activity::class.java)
            .create()
            .start()
            .resume()
            .visible()  // Make activity window visible
            .get()

        builder = ScreenshotBuilder(activity)

        // Create root view for proper hierarchy
        rootView = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.VISIBLE
        }
        activity.setContentView(rootView)

        // Create and attach test view properly
        testView = TextView(activity).apply {
            text = "Test"
            minimumWidth = 100
            minimumHeight = 100
            layoutParams = ViewGroup.LayoutParams(100, 100)
            visibility = View.VISIBLE
        }
        rootView.addView(testView)

        // Measure and layout
        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        rootView.layout(0, 0, 1000, 1000)

        testView.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY)
        )
        testView.layout(0, 0, 100, 100)

        // Idle looper
        shadowOf(Looper.getMainLooper()).idle()
    }

    @After
    fun tearDown() {
        // Reset the Main dispatcher after each test
        Dispatchers.resetMain()
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

        // Idle looper instead of sleep for faster test
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(50)  // Minimal sleep for async completion

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

        // Idle looper and minimal sleep
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(50)
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

        // Idle looper and minimal sleep
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(50)
        assertTrue(successCalled)
    }
}

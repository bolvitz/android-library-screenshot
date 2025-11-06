package com.screenshot.lib.handlers

import android.app.Activity
import android.graphics.Bitmap
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ViewCaptureHandler and ViewCaptureHandlerFactory
 */
@RunWith(RobolectricTestRunner::class)
class ViewCaptureHandlerTest {

    private lateinit var activity: Activity
    private lateinit var rootView: FrameLayout

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java)
            .create()
            .start()
            .resume()
            .get()

        // Create a root container for our test views
        rootView = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        activity.setContentView(rootView)
    }

    /**
     * Helper method to attach a view to the hierarchy and make it visible
     */
    private fun attachView(view: View, width: Int = 100, height: Int = 100) {
        view.layoutParams = ViewGroup.LayoutParams(width, height)
        rootView.addView(view)

        // Trigger layout
        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        rootView.layout(0, 0, 1000, 1000)
    }

    @Test
    fun `StandardViewCaptureHandler can handle any view`() {
        val handler = StandardViewCaptureHandler()
        val view = TextView(activity)

        assertTrue(handler.canHandle(view))
    }

    @Test
    fun `StandardViewCaptureHandler captures bitmap`() {
        val handler = StandardViewCaptureHandler()
        val view = TextView(activity).apply {
            text = "Test"
        }
        attachView(view)

        val bitmap = handler.captureBitmap(view, includeBackground = true)

        assertNotNull(bitmap)
        assertEquals(100, bitmap.width)
        assertEquals(100, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun `StandardViewCaptureHandler throws for invalid view`() {
        val handler = StandardViewCaptureHandler()
        val view = TextView(activity) // No layout, 0 dimensions

        try {
            handler.captureBitmap(view)
            fail("Should throw IllegalStateException for invalid view")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("not ready") == true ||
                      e.message?.contains("dimensions") == true)
        }
    }

    @Test
    fun `factory returns correct handler for TextView`() {
        val view = TextView(activity)
        attachView(view)

        val handler = ViewCaptureHandlerFactory.getHandler(view)

        assertTrue(handler is StandardViewCaptureHandler)
    }

    @Test
    fun `factory returns correct handler for ImageView`() {
        val view = ImageView(activity)
        attachView(view)

        val handler = ViewCaptureHandlerFactory.getHandler(view)

        assertTrue(handler is ImageViewCaptureHandler)
    }

    @Test
    fun `factory returns correct handler for WebView`() {
        val view = WebView(activity)
        attachView(view)

        val handler = ViewCaptureHandlerFactory.getHandler(view)

        assertTrue(handler is WebViewCaptureHandler)
    }

    @Test
    fun `factory returns correct handler for TextureView`() {
        val view = TextureView(activity)
        attachView(view)

        val handler = ViewCaptureHandlerFactory.getHandler(view)

        assertTrue(handler is TextureViewCaptureHandler)
    }

    @Test
    fun `factory returns correct handler for VideoView`() {
        val view = VideoView(activity)
        attachView(view)

        val handler = ViewCaptureHandlerFactory.getHandler(view)

        assertTrue(handler is VideoViewCaptureHandler)
    }

    @Test
    fun `factory throws for invalid view when validation enabled`() {
        val view = TextView(activity) // No layout

        try {
            ViewCaptureHandlerFactory.getHandler(view, validateVisibility = true)
            fail("Should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("not ready") == true)
        }
    }

    @Test
    fun `factory returns handler without validation when disabled`() {
        val view = TextView(activity) // No layout

        val handler = ViewCaptureHandlerFactory.getHandler(view, validateVisibility = false)

        assertNotNull(handler)
        assertTrue(handler is StandardViewCaptureHandler)
    }

    @Test
    fun `getCompatibleHandlers returns all compatible handlers`() {
        val view = ImageView(activity)
        attachView(view)

        val handlers = ViewCaptureHandlerFactory.getCompatibleHandlers(view)

        // ImageView should be handled by ImageViewCaptureHandler and StandardViewCaptureHandler
        assertTrue(handlers.size >= 2)
        assertTrue(handlers.any { it is ImageViewCaptureHandler })
        assertTrue(handlers.any { it is StandardViewCaptureHandler })
    }

    @Test
    fun `ImageViewCaptureHandler can handle ImageView`() {
        val handler = ImageViewCaptureHandler()
        val imageView = ImageView(activity)

        assertTrue(handler.canHandle(imageView))
    }

    @Test
    fun `ImageViewCaptureHandler cannot handle TextView`() {
        val handler = ImageViewCaptureHandler()
        val textView = TextView(activity)

        assertFalse(handler.canHandle(textView))
    }

    @Test
    fun `WebViewCaptureHandler can handle WebView`() {
        val handler = WebViewCaptureHandler()
        val webView = WebView(activity)

        assertTrue(handler.canHandle(webView))
    }

    @Test
    fun `TextureViewCaptureHandler can handle TextureView`() {
        val handler = TextureViewCaptureHandler()
        val textureView = TextureView(activity)

        assertTrue(handler.canHandle(textureView))
    }

    @Test
    fun `VideoViewCaptureHandler can handle VideoView`() {
        val handler = VideoViewCaptureHandler()
        val videoView = VideoView(activity)

        assertTrue(handler.canHandle(videoView))
    }

    @Test
    fun `capture with and without background`() {
        val handler = StandardViewCaptureHandler()
        val view = TextView(activity)
        attachView(view)

        val withBg = handler.captureBitmap(view, includeBackground = true)
        val withoutBg = handler.captureBitmap(view, includeBackground = false)

        assertNotNull(withBg)
        assertNotNull(withoutBg)

        // Both should have same dimensions
        assertEquals(withBg.width, withoutBg.width)
        assertEquals(withBg.height, withoutBg.height)
    }
}

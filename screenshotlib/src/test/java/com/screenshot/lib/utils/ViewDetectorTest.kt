package com.screenshot.lib.utils

import android.app.Activity
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ViewDetector
 */
@RunWith(RobolectricTestRunner::class)
class ViewDetectorTest {

    private lateinit var activity: Activity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java)
            .create()
            .start()
            .resume()
            .get()

        // Clear cache before each test
        ViewDetector.clearCache()
    }

    @Test
    fun `findBestViewToCapture returns decorView when nothing special found`() {
        val bestView = ViewDetector.findBestViewToCapture(activity, useCache = false)

        assertEquals(activity.window.decorView, bestView)
    }

    @Test
    fun `findBestViewToCapture prioritizes ImageView`() {
        val rootLayout = FrameLayout(activity).apply {
            layout(0, 0, 500, 500)
        }

        val imageView = ImageView(activity).apply {
            layout(0, 0, 100, 100)
            visibility = View.VISIBLE
        }

        rootLayout.addView(imageView)
        activity.setContentView(rootLayout)

        val bestView = ViewDetector.findBestViewToCapture(activity, useCache = false)

        assertEquals(imageView, bestView)
    }

    @Test
    fun `findBestViewToCapture prioritizes WebView`() {
        val rootLayout = FrameLayout(activity).apply {
            layout(0, 0, 500, 500)
        }

        val webView = WebView(activity).apply {
            layout(0, 0, 300, 300)
            visibility = View.VISIBLE
        }

        val textView = TextView(activity).apply {
            layout(0, 0, 100, 100)
            visibility = View.VISIBLE
        }

        rootLayout.addView(webView)
        rootLayout.addView(textView)
        activity.setContentView(rootLayout)

        val bestView = ViewDetector.findBestViewToCapture(activity, useCache = false)

        assertEquals(webView, bestView)
    }

    @Test
    fun `cache returns same result within TTL`() {
        val rootLayout = FrameLayout(activity).apply {
            layout(0, 0, 500, 500)
        }

        val imageView = ImageView(activity).apply {
            layout(0, 0, 100, 100)
            visibility = View.VISIBLE
        }

        rootLayout.addView(imageView)
        activity.setContentView(rootLayout)

        // First call
        val firstResult = ViewDetector.findBestViewToCapture(activity, useCache = true)

        // Second call should use cache
        val secondResult = ViewDetector.findBestViewToCapture(activity, useCache = true)

        assertSame(firstResult, secondResult)
    }

    @Test
    fun `cache can be disabled`() {
        val result1 = ViewDetector.findBestViewToCapture(activity, useCache = false)
        val result2 = ViewDetector.findBestViewToCapture(activity, useCache = false)

        // Should return same view, but without caching mechanism
        assertNotNull(result1)
        assertNotNull(result2)
    }

    @Test
    fun `clearCache removes cached results`() {
        ViewDetector.findBestViewToCapture(activity, useCache = true)

        ViewDetector.clearCache()

        // Should not throw, just work without cache
        val result = ViewDetector.findBestViewToCapture(activity, useCache = true)
        assertNotNull(result)
    }

    @Test
    fun `clearCacheForActivity removes specific activity cache`() {
        ViewDetector.findBestViewToCapture(activity, useCache = true)

        ViewDetector.clearCacheForActivity(activity)

        // Should work normally
        val result = ViewDetector.findBestViewToCapture(activity, useCache = true)
        assertNotNull(result)
    }

    @Test
    fun `getContentView returns content view`() {
        val contentView = ViewDetector.getContentView(activity)

        assertNotNull(contentView)
    }

    @Test
    fun `getDecorView returns decor view`() {
        val decorView = ViewDetector.getDecorView(activity)

        assertEquals(activity.window.decorView, decorView)
    }

    @Test
    fun `findVisibleImageView finds visible ImageView`() {
        val rootLayout = LinearLayout(activity).apply {
            layout(0, 0, 500, 500)
        }

        val imageView = ImageView(activity).apply {
            layout(0, 0, 100, 100)
            visibility = View.VISIBLE
        }

        rootLayout.addView(imageView)

        val found = ViewDetector.findVisibleImageView(rootLayout)

        assertEquals(imageView, found)
    }

    @Test
    fun `findVisibleWebView finds visible WebView`() {
        val rootLayout = LinearLayout(activity).apply {
            layout(0, 0, 500, 500)
        }

        val webView = WebView(activity).apply {
            layout(0, 0, 300, 300)
            visibility = View.VISIBLE
        }

        rootLayout.addView(webView)

        val found = ViewDetector.findVisibleWebView(rootLayout)

        assertEquals(webView, found)
    }

    @Test
    fun `findVisibleImageView returns null for invisible view`() {
        val rootLayout = LinearLayout(activity).apply {
            layout(0, 0, 500, 500)
        }

        val imageView = ImageView(activity).apply {
            layout(0, 0, 100, 100)
            visibility = View.GONE
        }

        rootLayout.addView(imageView)

        val found = ViewDetector.findVisibleImageView(rootLayout)

        assertNull(found)
    }

    @Test
    fun `findAllVisibleViewsOfType finds all matching views`() {
        val rootLayout = LinearLayout(activity).apply {
            layout(0, 0, 500, 500)
        }

        val imageView1 = ImageView(activity).apply {
            layout(0, 0, 100, 100)
            visibility = View.VISIBLE
        }

        val imageView2 = ImageView(activity).apply {
            layout(0, 0, 100, 100)
            visibility = View.VISIBLE
        }

        rootLayout.addView(imageView1)
        rootLayout.addView(imageView2)

        val found = ViewDetector.findAllVisibleViewsOfType(rootLayout, ImageView::class.java)

        assertEquals(2, found.size)
        assertTrue(found.contains(imageView1))
        assertTrue(found.contains(imageView2))
    }

    @Test
    fun `getViewHierarchyInfo returns hierarchy string`() {
        val rootLayout = LinearLayout(activity).apply {
            layout(0, 0, 500, 500)
        }

        val textView = TextView(activity).apply {
            layout(0, 0, 100, 100)
            visibility = View.VISIBLE
        }

        rootLayout.addView(textView)

        val hierarchyInfo = ViewDetector.getViewHierarchyInfo(rootLayout)

        assertNotNull(hierarchyInfo)
        assertTrue(hierarchyInfo.contains("LinearLayout"))
        assertTrue(hierarchyInfo.contains("TextView"))
        assertTrue(hierarchyInfo.contains("VISIBLE"))
    }

    @Test
    fun `findViewByIdIfVisible returns view when visible`() {
        val imageView = ImageView(activity).apply {
            id = android.R.id.icon
            layout(0, 0, 100, 100)
            visibility = View.VISIBLE
        }

        activity.setContentView(imageView)

        val found = ViewDetector.findViewByIdIfVisible<ImageView>(activity, android.R.id.icon)

        assertEquals(imageView, found)
    }

    @Test
    fun `findViewByIdIfVisible returns null for invisible view`() {
        val imageView = ImageView(activity).apply {
            id = android.R.id.icon
            layout(0, 0, 100, 100)
            visibility = View.GONE
        }

        activity.setContentView(imageView)

        val found = ViewDetector.findViewByIdIfVisible<ImageView>(activity, android.R.id.icon)

        assertNull(found)
    }
}

package com.screenshot.lib.utils

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Unit tests for ViewValidator
 */
@RunWith(RobolectricTestRunner::class)
class ViewValidatorTest {

    private lateinit var activity: Activity
    private lateinit var rootView: FrameLayout
    private lateinit var view: View

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java)
            .create()
            .start()
            .resume()
            .visible()  // Make the activity window visible
            .get()

        // Create a root container for our test views
        rootView = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.VISIBLE
        }
        activity.setContentView(rootView)

        // Ensure rootView is laid out and visible
        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        rootView.layout(0, 0, 1000, 1000)

        view = TextView(activity)
    }

    /**
     * Helper method to attach a view to the hierarchy and make it visible
     */
    private fun attachAndLayoutView(view: View, width: Int = 100, height: Int = 100) {
        view.minimumWidth = width
        view.minimumHeight = height
        view.layoutParams = ViewGroup.LayoutParams(width, height)
        view.visibility = View.VISIBLE
        rootView.addView(view)

        // Idle the main looper
        shadowOf(Looper.getMainLooper()).idle()

        // Trigger layout
        rootView.requestLayout()
        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        rootView.layout(0, 0, 1000, 1000)

        // Force explicit dimensions on the view
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        val left = view.left.coerceAtLeast(0)
        val top = view.top.coerceAtLeast(0)
        view.layout(left, top, left + width, top + height)

        // Final idle
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `null view is invalid`() {
        assertFalse(ViewValidator.isViewValid(null))
        assertFalse(ViewValidator.isViewVisible(null))
    }

    @Test
    fun `view with zero dimensions is invalid`() {
        // New view without layout has 0 dimensions
        assertFalse(ViewValidator.isViewValid(view))

        val validation = ViewValidator.validateViewWithDetails(view)
        assertFalse(validation.isValid)
        assertNotNull(validation.message)
    }

    @Test
    fun `view with dimensions is valid`() {
        attachAndLayoutView(view, 100, 100)

        assertTrue(ViewValidator.isViewValid(view))
        assertTrue(ViewValidator.isViewVisible(view))

        val validation = ViewValidator.validateViewWithDetails(view)
        assertTrue(validation.isValid)
    }

    @Test
    fun `invisible view is detected`() {
        attachAndLayoutView(view, 100, 100)
        view.visibility = View.INVISIBLE

        assertFalse(ViewValidator.isViewVisible(view))
    }

    @Test
    fun `gone view is detected`() {
        attachAndLayoutView(view, 100, 100)
        view.visibility = View.GONE

        assertFalse(ViewValidator.isViewVisible(view))
    }

    @Test
    fun `visible view is detected`() {
        attachAndLayoutView(view, 100, 100)
        view.visibility = View.VISIBLE

        assertTrue(ViewValidator.isViewVisible(view))
    }

    @Test
    fun `validation provides helpful message`() {
        // Zero dimensions - unattached view will fail isShown() check first
        val validation1 = ViewValidator.validateViewWithDetails(view)
        assertFalse(validation1.isValid)
        // Make case-insensitive check since message contains "isShown" not "shown"
        val msg1 = validation1.message.lowercase()
        assertTrue(msg1.contains("dimensions") ||
                   msg1.contains("size") ||
                   msg1.contains("attached") ||
                   msg1.contains("shown") ||
                   msg1.contains("hidden"))

        // Gone view
        attachAndLayoutView(view, 100, 100)
        view.visibility = View.GONE

        // Idle looper after visibility change
        shadowOf(Looper.getMainLooper()).idle()

        val validation2 = ViewValidator.validateViewWithDetails(view)
        assertFalse(validation2.isValid)
        val msg2 = validation2.message.lowercase()
        assertTrue(msg2.contains("visible") ||
                   msg2.contains("gone"))
    }

    @Test
    fun `attached view check`() {
        attachAndLayoutView(view, 100, 100)

        // View should be attached to window after using attachAndLayoutView
        val validation = ViewValidator.validateViewWithDetails(view)

        // Should be valid since we properly attached the view
        assertTrue(validation.isValid)
    }
}

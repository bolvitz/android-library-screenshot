package com.screenshot.lib.utils

import android.app.Activity
import android.view.View
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ViewValidator
 */
@RunWith(RobolectricTestRunner::class)
class ViewValidatorTest {

    private lateinit var activity: Activity
    private lateinit var view: View

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java)
            .create()
            .start()
            .resume()
            .get()

        view = TextView(activity)
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
        view.layout(0, 0, 100, 100)

        assertTrue(ViewValidator.isViewValid(view))
        assertTrue(ViewValidator.isViewVisible(view))

        val validation = ViewValidator.validateViewWithDetails(view)
        assertTrue(validation.isValid)
    }

    @Test
    fun `invisible view is detected`() {
        view.layout(0, 0, 100, 100)
        view.visibility = View.INVISIBLE

        assertFalse(ViewValidator.isViewVisible(view))
    }

    @Test
    fun `gone view is detected`() {
        view.layout(0, 0, 100, 100)
        view.visibility = View.GONE

        assertFalse(ViewValidator.isViewVisible(view))
    }

    @Test
    fun `visible view is detected`() {
        view.layout(0, 0, 100, 100)
        view.visibility = View.VISIBLE

        assertTrue(ViewValidator.isViewVisible(view))
    }

    @Test
    fun `validation provides helpful message`() {
        // Zero dimensions
        val validation1 = ViewValidator.validateViewWithDetails(view)
        assertFalse(validation1.isValid)
        assertTrue(validation1.message.contains("dimensions") ||
                   validation1.message.contains("size"))

        // Gone view
        view.layout(0, 0, 100, 100)
        view.visibility = View.GONE
        val validation2 = ViewValidator.validateViewWithDetails(view)
        assertFalse(validation2.isValid)
        assertTrue(validation2.message.contains("visible") ||
                   validation2.message.contains("GONE"))
    }

    @Test
    fun `attached view check`() {
        view.layout(0, 0, 100, 100)

        // View not attached to window initially
        val validation = ViewValidator.validateViewWithDetails(view)

        // Should still be valid even if not attached (we just check dimensions and visibility)
        assertTrue(validation.isValid || validation.message.contains("attached"))
    }
}

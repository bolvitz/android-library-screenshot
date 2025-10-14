package com.screenshot.lib.utils

import android.view.View

/**
 * Utility class for validating views before capture
 */
object ViewValidator {

    /**
     * Check if a view is truly visible and ready for capture.
     *
     * This performs comprehensive visibility checks:
     * - View is not null
     * - Visibility is VISIBLE
     * - View.isShown() returns true (checks entire hierarchy)
     * - View has valid dimensions
     *
     * @param view The view to check
     * @return true if view is visible and capturable
     */
    fun isViewVisible(view: View?): Boolean {
        if (view == null) return false

        // Check visibility property
        if (view.visibility != View.VISIBLE) return false

        // Check if view is actually shown (checks entire hierarchy)
        if (!view.isShown) return false

        // Check dimensions
        if (view.width <= 0 || view.height <= 0) return false

        return true
    }

    /**
     * Check if view has valid dimensions for capture.
     *
     * @param view The view to check
     * @param minWidth Minimum width required (default 1)
     * @param minHeight Minimum height required (default 1)
     * @return true if dimensions are valid
     */
    fun hasValidDimensions(
        view: View?,
        minWidth: Int = 1,
        minHeight: Int = 1
    ): Boolean {
        if (view == null) return false
        return view.width >= minWidth && view.height >= minHeight
    }

    /**
     * Check if view is attached to window.
     *
     * @param view The view to check
     * @return true if view is attached to a window
     */
    fun isViewAttached(view: View?): Boolean {
        if (view == null) return false
        return view.isAttachedToWindow
    }

    /**
     * Check if view is laid out (has been through layout pass).
     *
     * @param view The view to check
     * @return true if view has been laid out
     */
    fun isViewLaidOut(view: View?): Boolean {
        if (view == null) return false
        return view.isLaidOut
    }

    /**
     * Comprehensive validation for view capture readiness.
     * Combines all validation checks.
     *
     * @param view The view to validate
     * @param requireAttached Whether view must be attached to window (default true)
     * @param requireLaidOut Whether view must be laid out (default true)
     * @return true if view is ready for capture
     */
    fun isViewReadyForCapture(
        view: View?,
        requireAttached: Boolean = true,
        requireLaidOut: Boolean = true
    ): Boolean {
        if (view == null) return false

        // Must be visible
        if (!isViewVisible(view)) return false

        // Must be attached if required
        if (requireAttached && !isViewAttached(view)) return false

        // Must be laid out if required
        if (requireLaidOut && !isViewLaidOut(view)) return false

        return true
    }

    /**
     * Get the visibility status as a human-readable string.
     *
     * @param view The view to check
     * @return String describing visibility status
     */
    fun getVisibilityStatus(view: View?): String {
        if (view == null) return "NULL"

        val visibility = when (view.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN"
        }

        val isShown = view.isShown
        val attached = view.isAttachedToWindow
        val laidOut = view.isLaidOut
        val dimensions = "${view.width}x${view.height}"

        return "View[$visibility, isShown=$isShown, attached=$attached, laidOut=$laidOut, size=$dimensions]"
    }

    /**
     * Check if view is larger than a minimum area (in pixels).
     * Useful to avoid capturing very small views.
     *
     * @param view The view to check
     * @param minArea Minimum area in pixels (default 100)
     * @return true if view area is sufficient
     */
    fun hasMinimumArea(view: View?, minArea: Int = 100): Boolean {
        if (view == null) return false
        val area = view.width * view.height
        return area >= minArea
    }

    /**
     * Check if view's alpha is sufficient for capture.
     * Views that are nearly transparent may not produce useful screenshots.
     *
     * @param view The view to check
     * @param minAlpha Minimum alpha value (0.0 to 1.0, default 0.1)
     * @return true if view has sufficient alpha
     */
    fun hasSufficientAlpha(view: View?, minAlpha: Float = 0.1f): Boolean {
        if (view == null) return false
        return view.alpha >= minAlpha
    }

    /**
     * Validate view with detailed result including error message.
     *
     * @param view The view to validate
     * @return ValidationResult with success status and optional error message
     */
    fun validateViewWithDetails(view: View?): ValidationResult {
        if (view == null) {
            return ValidationResult(false, "View is null")
        }

        if (view.visibility != View.VISIBLE) {
            return ValidationResult(false, "View visibility is not VISIBLE: ${view.visibility}")
        }

        if (!view.isShown) {
            return ValidationResult(false, "View.isShown() returned false (hidden in hierarchy)")
        }

        if (view.width <= 0 || view.height <= 0) {
            return ValidationResult(false, "Invalid dimensions: ${view.width}x${view.height}")
        }

        if (!view.isAttachedToWindow) {
            return ValidationResult(false, "View is not attached to window")
        }

        if (!view.isLaidOut) {
            return ValidationResult(false, "View has not been laid out yet")
        }

        if (view.alpha < 0.01f) {
            return ValidationResult(false, "View alpha is too low: ${view.alpha}")
        }

        return ValidationResult(true, "View is ready for capture")
    }

    /**
     * Result of view validation with details
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}

package com.screenshot.lib.utils

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebView
import android.widget.ImageView
import android.widget.VideoView

/**
 * Utility for detecting and finding views automatically
 *
 * OPTIMIZED: Includes caching to avoid repeated view hierarchy traversal
 */
object ViewDetector {

    // Cache for view detection results
    // Map of Activity hashCode to (detected View, timestamp)
    private val viewCache = mutableMapOf<Int, Pair<View, Long>>()
    private const val CACHE_TTL_MS = 500L // Cache valid for 500ms

    // Maximum traversal depth to prevent excessive recursion
    private const val MAX_TRAVERSAL_DEPTH = 20

    /**
     * Find the best view to capture from an activity.
     * Searches for visible media views in priority order:
     * 1. PlayerView (Media3)
     * 2. TextureView (video/camera)
     * 3. WebView (web content)
     * 4. VideoView (video)
     * 5. ImageView (images)
     * 6. Falls back to window decorView
     *
     * OPTIMIZED: Uses caching to avoid repeated traversals
     *
     * @param activity The activity to search
     * @param useCache Whether to use cached result (default: true)
     * @return The best view to capture, or decorView if nothing found
     */
    fun findBestViewToCapture(activity: Activity, useCache: Boolean = true): View {
        val activityHash = activity.hashCode()
        val currentTime = System.currentTimeMillis()

        // Check cache if enabled
        if (useCache) {
            viewCache[activityHash]?.let { (cachedView, timestamp) ->
                if (currentTime - timestamp < CACHE_TTL_MS) {
                    // Cache hit - verify view is still valid
                    if (ViewValidator.isViewVisible(cachedView)) {
                        return cachedView
                    }
                }
            }
        }

        // Cache miss or expired - perform detection
        val rootView = activity.window.decorView
        val detectedView = detectBestView(rootView)

        // Update cache
        if (useCache && detectedView != rootView) {
            viewCache[activityHash] = Pair(detectedView, currentTime)

            // Clean up old cache entries
            cleanupCache(currentTime)
        }

        return detectedView
    }

    /**
     * Perform actual view detection without caching
     */
    private fun detectBestView(rootView: View): View {
        // Try to find visible media views
        findVisiblePlayerView(rootView)?.let { return it }
        findVisibleTextureView(rootView)?.let { return it }
        findVisibleWebView(rootView)?.let { return it }
        findVisibleVideoView(rootView)?.let { return it }
        findVisibleImageView(rootView)?.let { return it }

        // Fallback to entire window
        return rootView
    }

    /**
     * Clear the view cache
     * Call this if you want to force fresh detection
     */
    fun clearCache() {
        viewCache.clear()
    }

    /**
     * Clear cache for specific activity
     */
    fun clearCacheForActivity(activity: Activity) {
        viewCache.remove(activity.hashCode())
    }

    /**
     * Remove expired cache entries
     */
    private fun cleanupCache(currentTime: Long) {
        val iterator = viewCache.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value.second > CACHE_TTL_MS * 2) {
                iterator.remove()
            }
        }
    }

    /**
     * Find a visible PlayerView in the view hierarchy
     */
    fun findVisiblePlayerView(root: View): View? {
        return findVisibleViewByClassName(root, "androidx.media3.ui.PlayerView")
    }

    /**
     * Find a visible TextureView in the view hierarchy
     */
    fun findVisibleTextureView(root: View): android.view.TextureView? {
        return findVisibleViewOfType(root)
    }

    /**
     * Find a visible WebView in the view hierarchy
     */
    fun findVisibleWebView(root: View): WebView? {
        return findVisibleViewOfType(root)
    }

    /**
     * Find a visible VideoView in the view hierarchy
     */
    fun findVisibleVideoView(root: View): VideoView? {
        return findVisibleViewOfType(root)
    }

    /**
     * Find a visible ImageView in the view hierarchy
     */
    fun findVisibleImageView(root: View): ImageView? {
        return findVisibleViewOfType(root)
    }

    /**
     * Find a specific view by ID from activity
     *
     * @param activity The activity containing the view
     * @param id The resource ID of the view
     * @return The view if found and visible, null otherwise
     */
    fun <T : View> findViewByIdIfVisible(activity: Activity, id: Int): T? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val view = activity.findViewById<T>(id)
            if (view != null && ViewValidator.isViewVisible(view)) {
                view
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generic function to find a visible view of a specific type
     * OPTIMIZED: Limits traversal depth to prevent excessive recursion
     */
    private inline fun <reified T : View> findVisibleViewOfType(root: View): T? {
        return findVisibleViewOfTypeHelper(root, T::class.java, 0)
    }

    /**
     * Helper function for findVisibleViewOfType (non-inline to allow recursion)
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : View> findVisibleViewOfTypeHelper(
        root: View,
        targetClass: Class<T>,
        depth: Int
    ): T? {
        // Limit traversal depth
        if (depth > MAX_TRAVERSAL_DEPTH) {
            return null
        }

        if (targetClass.isInstance(root) && ViewValidator.isViewVisible(root)) {
            return root as T
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                val result = findVisibleViewOfTypeHelper(child, targetClass, depth + 1)
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    /**
     * Find a view by class name (useful for views we don't have compile-time dependency on)
     * OPTIMIZED: Limits traversal depth
     */
    private fun findVisibleViewByClassName(
        root: View,
        className: String,
        depth: Int = 0
    ): View? {
        // Limit traversal depth
        if (depth > MAX_TRAVERSAL_DEPTH) {
            return null
        }

        if (root.javaClass.name == className && ViewValidator.isViewVisible(root)) {
            return root
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                val result = findVisibleViewByClassName(child, className, depth + 1)
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    /**
     * Get all visible views of a specific type
     */
    fun <T : View> findAllVisibleViewsOfType(root: View, type: Class<T>): List<T> {
        val results = mutableListOf<T>()

        @Suppress("UNCHECKED_CAST")
        if (type.isInstance(root) && ViewValidator.isViewVisible(root)) {
            results.add(root as T)
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                results.addAll(findAllVisibleViewsOfType(child, type))
            }
        }

        return results
    }

    /**
     * Get the content view from an activity
     * This is typically what you want to capture for app content
     */
    fun getContentView(activity: Activity): View? {
        return activity.findViewById(android.R.id.content)
    }

    /**
     * Get the decor view from an activity
     * This includes system UI like status bar and navigation bar
     */
    fun getDecorView(activity: Activity): View {
        return activity.window.decorView
    }

    /**
     * Get information about visible views in the hierarchy
     * Useful for debugging
     */
    fun getViewHierarchyInfo(root: View): String {
        val builder = StringBuilder()
        appendViewInfo(root, builder, 0)
        return builder.toString()
    }

    private fun appendViewInfo(view: View, builder: StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth)
        val visibility = when (view.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN"
        }

        builder.append("$indent${view.javaClass.simpleName} - $visibility (${view.width}x${view.height})\n")

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                appendViewInfo(view.getChildAt(i), builder, depth + 1)
            }
        }
    }
}

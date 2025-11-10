package com.screenshot.lib

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.screenshot.lib.handlers.ViewCaptureHandlerFactory
import com.screenshot.lib.permissions.PermissionHelper
import com.screenshot.lib.storage.StorageManager
import kotlinx.coroutines.*
import java.io.File
import java.lang.ref.WeakReference

/**
 * Main screenshot capture engine
 *
 * Memory Safety:
 * - Uses WeakReference for Activity contexts to prevent memory leaks
 * - Stores Application context directly (no leak risk)
 * - Automatically cancels coroutines on cleanup()
 */
class ScreenshotCapture(context: Context) {

    // Store context safely based on type
    private val contextRef: WeakReference<Context>? = if (context is Activity) {
        WeakReference(context)  // Use weak reference for Activities
    } else {
        null  // Application context is safe
    }
    private val appContext: Context? = if (context is Activity) {
        null  // Don't store Activity as strong reference
    } else {
        context  // Application context is safe to store
    }

    private val storageManager = StorageManager(getContext() ?: context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Get the context, checking WeakReference if necessary
     */
    private fun getContext(): Context? {
        return appContext ?: contextRef?.get()
    }

    /**
     * Ensure context is available
     */
    private fun requireContext(): Context {
        return getContext() ?: throw IllegalStateException(
            "Context no longer available (Activity may have been destroyed)"
        )
    }

    /**
     * Capture a screenshot of a view synchronously
     * Returns the bitmap without saving
     *
     * IMPORTANT: This method performs view operations and should ideally be called
     * on the main thread. However, it's synchronous so it may block the caller thread.
     * Consider using captureBitmapAsync() for better performance.
     *
     * @param view The view to capture
     * @param includeBackground Whether to include the view's background
     * @return The captured bitmap
     */
    @MainThread
    fun captureBitmap(view: View, includeBackground: Boolean = true): Bitmap {
        val handler = ViewCaptureHandlerFactory.getHandler(view)
        return handler.captureBitmap(view, includeBackground)
    }

    /**
     * Capture a screenshot of a view asynchronously using coroutines
     * This is the recommended method as it properly handles threading.
     *
     * @param view The view to capture
     * @param includeBackground Whether to include the view's background
     * @return The captured bitmap
     */
    suspend fun captureBitmapAsync(view: View, includeBackground: Boolean = true): Bitmap =
        withContext(Dispatchers.Main) {
            val handler = ViewCaptureHandlerFactory.getHandler(view)
            handler.captureBitmap(view, includeBackground)
        }

    /**
     * Capture and save a screenshot asynchronously with callback
     *
     * @param view The view to capture
     * @param config Screenshot configuration including recycling options
     * @param storageLocation Where to save the screenshot
     * @param callback Callback for success/error
     */
    @AnyThread
    fun captureAndSave(
        view: View,
        config: ScreenshotConfig = ScreenshotConfig(),
        storageLocation: StorageLocation = StorageLocation.INTERNAL,
        callback: ScreenshotCallback? = null
    ) {
        scope.launch {
            try {
                val result = captureAndSaveAsync(view, config, storageLocation)
                when (result) {
                    is ScreenshotResult.Success -> {
                        callback?.onSuccess(result.file, result.bitmap)
                    }
                    is ScreenshotResult.Error -> {
                        callback?.onError(result.exception, result.message)
                    }
                }
            } catch (e: Exception) {
                callback?.onError(e, "Unexpected error: ${e.message}")
            }
        }
    }

    /**
     * Capture and save a screenshot using coroutines
     * Supports bitmap recycling for memory optimization
     *
     * @param view The view to capture
     * @param config Screenshot configuration including recycling options
     * @param storageLocation Where to save the screenshot
     * @return ScreenshotResult with file and optional bitmap
     */
    suspend fun captureAndSaveAsync(
        view: View,
        config: ScreenshotConfig = ScreenshotConfig(),
        storageLocation: StorageLocation = StorageLocation.INTERNAL
    ): ScreenshotResult = withContext(Dispatchers.Main) {
        try {
            // Check for cancellation
            ensureActive()

            val ctx = getContext()
            if (ctx == null) {
                return@withContext ScreenshotResult.Error(
                    IllegalStateException("Context no longer available"),
                    "Activity has been destroyed, cannot capture screenshot"
                )
            }

            // Check permissions if needed
            val needsPermission = PermissionHelper.isPermissionNeeded(
                ctx,
                storageLocation == StorageLocation.EXTERNAL
            )

            if (needsPermission && !PermissionHelper.hasStoragePermission(ctx)) {
                return@withContext ScreenshotResult.Error(
                    SecurityException("Storage permission not granted"),
                    "Storage permission is required to save screenshots to external storage"
                )
            }

            // Capture bitmap on main thread (required for view operations)
            val bitmap = captureBitmap(view, config.includeBackground)

            // Check for cancellation before expensive IO
            ensureActive()

            // Save to storage on IO thread
            val file = withContext(Dispatchers.IO) {
                ensureActive()
                storageManager.saveBitmap(bitmap, config, storageLocation)
            }

            // Handle bitmap recycling based on config
            val resultBitmap = when {
                !config.returnBitmap -> {
                    bitmap.recycle()
                    null
                }
                config.recycleBitmapAfterSave -> {
                    bitmap.recycle()
                    null
                }
                else -> bitmap
            }

            ScreenshotResult.Success(file, resultBitmap)

        } catch (e: CancellationException) {
            throw e  // Propagate cancellation
        } catch (e: IllegalStateException) {
            ScreenshotResult.Error(e, "Invalid view state: ${e.message}")
        } catch (e: SecurityException) {
            ScreenshotResult.Error(e, "Permission denied: ${e.message}")
        } catch (e: Exception) {
            ScreenshotResult.Error(e, "Failed to capture screenshot: ${e.message}")
        }
    }

    /**
     * Get storage manager for additional operations
     */
    @AnyThread
    fun getStorageManager(): StorageManager = storageManager

    /**
     * Clean up resources and cancel ongoing operations
     * Call this when you're done with the ScreenshotCapture instance
     */
    @AnyThread
    fun cleanup() {
        scope.cancel()
    }
}

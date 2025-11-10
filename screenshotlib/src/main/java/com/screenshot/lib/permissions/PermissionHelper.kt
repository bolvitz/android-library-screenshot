package com.screenshot.lib.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper class for handling storage permissions
 */
object PermissionHelper {

    const val REQUEST_CODE_STORAGE = 1001

    /**
     * Check if storage permission is needed based on Android version and save location
     */
    @Suppress("UNUSED_PARAMETER")
    fun isPermissionNeeded(context: Context, isExternalStorage: Boolean): Boolean {
        // No permission needed for internal storage
        if (!isExternalStorage) {
            return false
        }

        // Android 10+ uses scoped storage, no permission needed for app-specific directories
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return false
        }

        // Android 6.0 - 9.0 requires WRITE_EXTERNAL_STORAGE
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    /**
     * Check if the app has storage permission
     */
    fun hasStoragePermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true // Scoped storage, no permission needed
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request storage permission from the user
     */
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_STORAGE
            )
        }
    }

    /**
     * Check if permission request was granted
     */
    @Suppress("UNUSED_PARAMETER")
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == REQUEST_CODE_STORAGE) {
            return grantResults.isNotEmpty() &&
                   grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    /**
     * Check if we should show permission rationale
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return false
        }

        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

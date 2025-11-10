package com.screenshot.lib.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.screenshot.lib.ScreenshotConfig
import com.screenshot.lib.StorageLocation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Unit tests for StorageManager
 */
@RunWith(RobolectricTestRunner::class)
class StorageManagerTest {

    private lateinit var context: Context
    private lateinit var storageManager: StorageManager
    private lateinit var bitmap: Bitmap

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        storageManager = StorageManager(context)

        // Create a test bitmap
        bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
    }

    @Test
    fun `saveBitmap creates file in internal storage`() = runTest {
        val config = ScreenshotConfig()

        val file = storageManager.saveBitmap(bitmap, config, StorageLocation.INTERNAL)

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.endsWith(".png"))
        assertTrue(file.path.contains("Screenshots"))

        // Cleanup
        file.delete()
    }

    @Test
    fun `saveBitmap with custom filename`() = runTest {
        val config = ScreenshotConfig(fileName = "test_screenshot.png")

        val file = storageManager.saveBitmap(bitmap, config, StorageLocation.INTERNAL)

        assertEquals("test_screenshot.png", file.name)
        assertTrue(file.exists())

        // Cleanup
        file.delete()
    }

    @Test
    fun `saveBitmap with JPEG format`() = runTest {
        val config = ScreenshotConfig(
            format = Bitmap.CompressFormat.JPEG,
            quality = 85
        )

        val file = storageManager.saveBitmap(bitmap, config, StorageLocation.INTERNAL)

        assertTrue(file.name.endsWith(".jpg"))
        assertTrue(file.exists())

        // Cleanup
        file.delete()
    }

    @Test
    fun `saveBitmap with WEBP format`() = runTest {
        val config = ScreenshotConfig(
            format = Bitmap.CompressFormat.WEBP,
            quality = 90
        )

        val file = storageManager.saveBitmap(bitmap, config, StorageLocation.INTERNAL)

        assertTrue(file.name.endsWith(".webp"))
        assertTrue(file.exists())

        // Cleanup
        file.delete()
    }

    @Test
    fun `saveBitmap with custom directory`() = runTest {
        val customDir = File(context.filesDir, "CustomScreenshots")
        val config = ScreenshotConfig(saveDirectory = customDir)

        val file = storageManager.saveBitmap(bitmap, config, StorageLocation.INTERNAL)

        assertTrue(file.path.contains("CustomScreenshots"))
        assertTrue(file.exists())

        // Cleanup
        file.delete()
        customDir.delete()
    }

    @Test
    fun `getAllScreenshots returns saved screenshots`() = runTest {
        // Save multiple screenshots
        val config1 = ScreenshotConfig(fileName = "test1.png")
        val config2 = ScreenshotConfig(fileName = "test2.png")

        val file1 = storageManager.saveBitmap(bitmap, config1, StorageLocation.INTERNAL)
        Thread.sleep(10) // Ensure different timestamps
        val file2 = storageManager.saveBitmap(bitmap, config2, StorageLocation.INTERNAL)

        val screenshots = storageManager.getAllScreenshots(StorageLocation.INTERNAL)

        assertTrue(screenshots.size >= 2)
        assertTrue(screenshots.any { it.name == "test1.png" })
        assertTrue(screenshots.any { it.name == "test2.png" })

        // Cleanup
        file1.delete()
        file2.delete()
    }

    @Test
    fun `getAllScreenshots with limit and offset`() = runTest {
        // Clear existing screenshots first
        storageManager.clearAllScreenshots(StorageLocation.INTERNAL)

        // Save 5 screenshots
        for (i in 1..5) {
            val config = ScreenshotConfig(fileName = "test$i.png")
            storageManager.saveBitmap(bitmap, config, StorageLocation.INTERNAL)
            Thread.sleep(10)
        }

        // Get with limit
        val limited = storageManager.getAllScreenshots(
            StorageLocation.INTERNAL,
            limit = 3
        )
        assertEquals(3, limited.size)

        // Get with offset
        val offset = storageManager.getAllScreenshots(
            StorageLocation.INTERNAL,
            limit = 3,
            offset = 2
        )
        assertEquals(3, offset.size)

        // Cleanup
        storageManager.clearAllScreenshots(StorageLocation.INTERNAL)
    }

    @Test
    fun `deleteScreenshot removes file`() = runTest {
        val config = ScreenshotConfig(fileName = "to_delete.png")
        val file = storageManager.saveBitmap(bitmap, config, StorageLocation.INTERNAL)

        assertTrue(file.exists())

        val deleted = storageManager.deleteScreenshot(file)

        assertTrue(deleted)
        assertFalse(file.exists())
    }

    @Test
    fun `clearAllScreenshots removes all files`() = runTest {
        // Save multiple screenshots
        for (i in 1..3) {
            val config = ScreenshotConfig(fileName = "clear_test$i.png")
            storageManager.saveBitmap(bitmap, config, StorageLocation.INTERNAL)
        }

        val deletedCount = storageManager.clearAllScreenshots(StorageLocation.INTERNAL)

        assertTrue(deletedCount >= 3)

        val remaining = storageManager.getAllScreenshots(StorageLocation.INTERNAL)
        assertEquals(0, remaining.size)
    }

    @Test
    fun `generated filenames have timestamp`() = runTest {
        val config = ScreenshotConfig() // No custom filename

        val file = storageManager.saveBitmap(bitmap, config, StorageLocation.INTERNAL)

        assertTrue(file.name.startsWith("screenshot_"))
        assertTrue(file.name.matches(Regex("screenshot_\\d{8}_\\d{6}\\.png")))

        // Cleanup
        file.delete()
    }

    @Test
    fun `quality setting affects file size for JPEG`() = runTest {
        val highQuality = ScreenshotConfig(
            format = Bitmap.CompressFormat.JPEG,
            quality = 100,
            fileName = "high.jpg"
        )
        val lowQuality = ScreenshotConfig(
            format = Bitmap.CompressFormat.JPEG,
            quality = 10,
            fileName = "low.jpg"
        )

        val highFile = storageManager.saveBitmap(bitmap, highQuality, StorageLocation.INTERNAL)
        val lowFile = storageManager.saveBitmap(bitmap, lowQuality, StorageLocation.INTERNAL)

        assertTrue(highFile.length() > lowFile.length())

        // Cleanup
        highFile.delete()
        lowFile.delete()
    }
}

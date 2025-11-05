package com.screenshot.lib

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests for ScreenshotConfig
 */
class ScreenshotConfigTest {

    @Test
    fun `default config has correct values`() {
        val config = ScreenshotConfig()

        assertEquals(Bitmap.CompressFormat.PNG, config.format)
        assertEquals(100, config.quality)
        assertNull(config.saveDirectory)
        assertNull(config.fileName)
        assertTrue(config.includeBackground)
        assertFalse(config.recycleBitmapAfterSave)
        assertTrue(config.returnBitmap)
    }

    @Test
    fun `config copy works correctly`() {
        val original = ScreenshotConfig(
            format = Bitmap.CompressFormat.JPEG,
            quality = 85
        )

        val modified = original.copy(quality = 90)

        assertEquals(Bitmap.CompressFormat.JPEG, modified.format)
        assertEquals(90, modified.quality)
    }

    @Test
    fun `config with custom directory`() {
        val customDir = File("/custom/path")
        val config = ScreenshotConfig(saveDirectory = customDir)

        assertEquals(customDir, config.saveDirectory)
    }

    @Test
    fun `config with custom filename`() {
        val config = ScreenshotConfig(fileName = "my_screenshot.png")

        assertEquals("my_screenshot.png", config.fileName)
    }

    @Test
    fun `config with recycle option`() {
        val config = ScreenshotConfig(
            recycleBitmapAfterSave = true,
            returnBitmap = false
        )

        assertTrue(config.recycleBitmapAfterSave)
        assertFalse(config.returnBitmap)
    }
}

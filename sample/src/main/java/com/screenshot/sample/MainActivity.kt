package com.screenshot.sample

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.screenshot.lib.ScreenshotBuilder
import com.screenshot.lib.ScreenshotCallback
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var sampleView: android.view.View
    private lateinit var sampleImageView: ImageView
    private lateinit var sampleWebView: WebView
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        sampleView = findViewById(R.id.sampleView)
        sampleImageView = findViewById(R.id.sampleImageView)
        sampleWebView = findViewById(R.id.sampleWebView)
        resultText = findViewById(R.id.resultText)

        // Setup WebView
        setupWebView()

        // Setup buttons
        findViewById<Button>(R.id.btnCaptureView).setOnClickListener {
            captureViewExample()
        }

        findViewById<Button>(R.id.btnCaptureImageView).setOnClickListener {
            captureImageViewExample()
        }

        findViewById<Button>(R.id.btnCaptureWithConfig).setOnClickListener {
            captureWithConfigExample()
        }

        findViewById<Button>(R.id.btnCaptureWebView).setOnClickListener {
            captureWebViewExample()
        }

        findViewById<Button>(R.id.btnCaptureBitmapOnly).setOnClickListener {
            captureBitmapOnlyExample()
        }

        findViewById<Button>(R.id.btnOpenComposeDemo).setOnClickListener {
            startActivity(Intent(this, ComposeActivity::class.java))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        sampleWebView.settings.javaScriptEnabled = true
        sampleWebView.loadData(
            "<html><body style='background-color: #f0f0f0; padding: 20px;'>" +
                    "<h1 style='color: #333;'>WebView Content</h1>" +
                    "<p>This is a sample WebView that can be captured as a screenshot.</p>" +
                    "</body></html>",
            "text/html",
            "UTF-8"
        )
    }

    /**
     * Example 1: Simple capture with default settings
     */
    private fun captureViewExample() {
        ScreenshotBuilder.quickCapture(
            context = this,
            view = sampleView,
            callback = object : ScreenshotCallback {
                override fun onSuccess(file: File, bitmap: Bitmap?) {
                    showResult("Screenshot saved to: ${file.absolutePath}")
                    bitmap?.let {
                        Toast.makeText(
                            this@MainActivity,
                            "Size: ${it.width}x${it.height}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(exception: Exception, message: String) {
                    showResult("Error: $message")
                }
            }
        )
    }

    /**
     * Example 2: Capture ImageView specifically
     */
    private fun captureImageViewExample() {
        ScreenshotBuilder(this)
            .view(sampleImageView)
            .format(Bitmap.CompressFormat.PNG)
            .fileName("imageview_screenshot.png")
            .capture(
                onSuccess = { file, bitmap ->
                    showResult("ImageView captured: ${file.name}")
                },
                onError = { exception, message ->
                    showResult("Error: $message")
                }
            )
    }

    /**
     * Example 3: Capture with custom configuration
     */
    private fun captureWithConfigExample() {
        ScreenshotBuilder(this)
            .view(sampleView)
            .format(Bitmap.CompressFormat.JPEG)  // Use JPEG format
            .quality(85)                          // 85% quality
            .fileName("custom_screenshot.jpg")    // Custom file name
            .includeBackground(true)              // Include background
            .saveToInternal()                     // Save to internal storage (no permissions needed)
            .capture(
                onSuccess = { file, bitmap ->
                    showResult("Custom screenshot saved: ${file.name}\nSize: ${file.length() / 1024}KB")
                },
                onError = { exception, message ->
                    showResult("Error: $message")
                }
            )
    }

    /**
     * Example 4: Capture WebView
     */
    private fun captureWebViewExample() {
        sampleWebView.visibility = android.view.View.VISIBLE

        // Wait a bit for WebView to render
        sampleWebView.postDelayed({
            ScreenshotBuilder(this)
                .view(sampleWebView)
                .format(Bitmap.CompressFormat.PNG)
                .fileName("webview_screenshot.png")
                .capture(
                    onSuccess = { file, bitmap ->
                        showResult("WebView captured: ${file.name}")
                        // Hide WebView after capture
                        sampleWebView.visibility = android.view.View.GONE
                    },
                    onError = { exception, message ->
                        showResult("Error: $message")
                        sampleWebView.visibility = android.view.View.GONE
                    }
                )
        }, 500)
    }

    /**
     * Example 5: Capture bitmap only (without saving)
     */
    private fun captureBitmapOnlyExample() {
        try {
            val bitmap = ScreenshotBuilder.quickCaptureBitmap(this, sampleView)
            showResult("Bitmap captured: ${bitmap.width}x${bitmap.height}")
            // Use bitmap as needed...
        } catch (e: Exception) {
            showResult("Error: ${e.message}")
        }
    }

    private fun showResult(message: String) {
        resultText.text = message
    }

    /**
     * Handle permission result if using external storage
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (com.screenshot.lib.permissions.PermissionHelper.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
        ) {
            Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}

# Android Screenshot Library

[![Maven Central](https://img.shields.io/maven-central/v/io.github.bolvitz/screenshot-android)](https://search.maven.org/artifact/io.github.bolvitz/screenshot-android)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A lightweight, powerful Android library for capturing screenshots of any view with advanced features like auto-detection, memory optimization, and specialized view handlers.

## ✨ Key Features

- 📸 **Universal View Support** - Capture any Android view (TextView, ImageView, VideoView, WebView, custom views, etc.)
- 🎯 **Auto-Detection** - Automatically finds and captures the best visible media view
- 🎬 **Media View Support** - Specialized handlers for PlayerView (Media3), TextureView, VideoView, ImageView, WebView
- 💾 **Memory Optimized** - Built-in bitmap recycling and memory management options
- 🔐 **Smart Permissions** - Automatic permission handling with Android version awareness
- ⚡ **High Performance** - Thread-safe, coroutine-based with minimal overhead
- 🎨 **Fully Configurable** - Control format (PNG/JPEG/WEBP), quality, filename, and storage location
- 📱 **Modern Android** - Supports API 21+ with scoped storage for Android 10+
- 🪶 **Lightweight** - < 100KB with minimal dependencies

## 📱 Demo

See the library in action with our sample app featuring both traditional XML views and modern Jetpack Compose examples:

<p align="center">
  <img src="screenshots/main_activity_demo.png" alt="Sample App Demo" width="300"/>
</p>

The sample app demonstrates:
- ✅ Basic view capture with various configurations
- ✅ ImageView, WebView, and VideoView capture
- ✅ Custom screenshot configurations (format, quality, filename)
- ✅ Bitmap-only capture (no file saving)
- ✅ **Jetpack Compose integration** with @Preview support
- ✅ Permission handling examples

## 📦 Installation

### Maven Central (Recommended)

Add to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'io.github.bolvitz:screenshot-android:1.0.0'
}
```

### JitPack (Alternative)

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.bolvitz:android-library-screenshot:1.0.0'
}
```

## 🚀 Quick Start

### Simple Capture

```kotlin
// One-line screenshot with default settings
ScreenshotBuilder.quickCapture(context, myView, object : ScreenshotCallback {
    override fun onSuccess(file: File, bitmap: Bitmap?) {
        Toast.makeText(context, "Saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
    }

    override fun onError(exception: Exception, message: String) {
        Toast.makeText(context, "Error: $message", Toast.LENGTH_SHORT).show()
    }
})
```

### Builder Pattern

```kotlin
ScreenshotBuilder(context)
    .view(myView)
    .format(Bitmap.CompressFormat.JPEG)
    .quality(90)
    .fileName("my_screenshot.jpg")
    .saveToInternal()
    .capture { file, bitmap ->
        // Screenshot saved successfully
    }
```

## 📚 Usage Examples

### 1. Auto-Detect Best View

Automatically finds and captures visible media views in priority order: PlayerView → TextureView → WebView → VideoView → ImageView → Full Window

```kotlin
// Automatically detect and capture the best view
ScreenshotBuilder.quickCaptureAuto(activity, object : ScreenshotCallback {
    override fun onSuccess(file: File, bitmap: Bitmap?) {
        Log.d("Screenshot", "Auto-captured: ${file.name}")
    }

    override fun onError(exception: Exception, message: String) {
        Log.e("Screenshot", "Error: $message")
    }
})

// Or with builder
ScreenshotBuilder(activity)
    .autoDetectView()
    .capture { file, bitmap ->
        // Best view automatically captured
    }
```

### 2. Capture Entire Window

Captures the full screen including status bar and navigation bar:

```kotlin
ScreenshotBuilder.quickCaptureWindow(activity, object : ScreenshotCallback {
    override fun onSuccess(file: File, bitmap: Bitmap?) {
        // Full window screenshot saved
    }

    override fun onError(exception: Exception, message: String) {
        // Handle error
    }
})
```

### 3. Capture Content View Only

Captures app content without system UI:

```kotlin
ScreenshotBuilder(activity)
    .captureContent()  // Content view only
    .format(Bitmap.CompressFormat.PNG)
    .capture { file, bitmap ->
        // Content captured without system bars
    }
```

### 4. Capture PlayerView (Media3)

Perfect for video streaming apps:

```kotlin
val playerView = findViewById<androidx.media3.ui.PlayerView>(R.id.player_view)

ScreenshotBuilder(this)
    .view(playerView)
    .fileName("video_frame.png")
    .capture { file, bitmap ->
        // Current video frame captured
    }
```

### 5. Capture TextureView

For camera preview or video rendering:

```kotlin
val textureView = findViewById<TextureView>(R.id.texture_view)

ScreenshotBuilder(this)
    .view(textureView)
    .format(Bitmap.CompressFormat.JPEG)
    .quality(95)
    .capture { file, bitmap ->
        // TextureView content captured
    }
```

### 6. Capture WebView (Full Content)

Captures entire WebView content, not just visible area:

```kotlin
val webView = findViewById<WebView>(R.id.web_view)

ScreenshotBuilder(this)
    .view(webView)
    .includeBackground(true)
    .capture { file, bitmap ->
        // Full WebView content captured (scrollable area)
    }
```

### 7. Memory Optimization

Automatically recycle bitmaps to save memory:

```kotlin
ScreenshotBuilder(this)
    .view(myView)
    .recycleBitmapAfterSave(true)  // Auto-recycle after saving
    .capture { file, bitmap ->
        // bitmap will be null here (already recycled)
        // Only file is available - perfect for large screenshots
    }

// Or if you don't need bitmap at all
ScreenshotBuilder(this)
    .view(myView)
    .returnBitmap(false)  // Don't return bitmap (save memory)
    .capture { file, bitmap ->
        // bitmap is null, only file path available
    }
```

### 8. Custom Configuration

```kotlin
ScreenshotBuilder(this)
    .view(myView)
    .format(Bitmap.CompressFormat.JPEG)  // Use JPEG
    .quality(85)                          // 85% quality
    .fileName("custom_${System.currentTimeMillis()}.jpg")
    .saveDirectory(File(getExternalFilesDir(null), "MyScreenshots"))
    .includeBackground(true)
    .capture { file, bitmap ->
        Log.d("Screenshot", "Saved: ${file.absolutePath}")
        bitmap?.let {
            // Use bitmap if needed
            Log.d("Screenshot", "Size: ${it.width}x${it.height}")
        }
    }
```

### 9. Get Bitmap Without Saving

```kotlin
// Synchronous (blocks current thread)
try {
    val bitmap = ScreenshotBuilder.quickCaptureBitmap(this, myView)
    imageView.setImageBitmap(bitmap)
} catch (e: Exception) {
    Log.e("Screenshot", "Error: ${e.message}")
}

// Asynchronous (recommended)
lifecycleScope.launch {
    try {
        val bitmap = ScreenshotBuilder(this@MainActivity)
            .view(myView)
            .captureBitmapAsync()
        imageView.setImageBitmap(bitmap)
    } catch (e: Exception) {
        Log.e("Screenshot", "Error: ${e.message}")
    }
}
```

### 10. Using Kotlin Coroutines

```kotlin
lifecycleScope.launch {
    val result = ScreenshotBuilder(this@MainActivity)
        .view(myView)
        .format(Bitmap.CompressFormat.PNG)
        .captureAsync()  // Suspend function

    when (result) {
        is ScreenshotResult.Success -> {
            Log.d("Screenshot", "Saved: ${result.file.absolutePath}")
            result.bitmap?.let {
                // Use bitmap if available
            }
        }
        is ScreenshotResult.Error -> {
            Log.e("Screenshot", "Error: ${result.message}")
        }
    }
}
```

### 11. Jetpack Compose Integration

Capture Compose UI by wrapping composables in AndroidView:

```kotlin
@Composable
fun ScreenshotDemo() {
    val context = LocalContext.current
    var captureableView by remember { mutableStateOf<View?>(null) }
    var resultText by remember { mutableStateOf("") }

    Column {
        // Wrap your Compose content in AndroidView to make it capturable
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                ComposeView(ctx).apply {
                    setContent {
                        // Your composable content here
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("This Compose UI can be captured!")
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .background(Color.Gray)
                                )
                            }
                        }
                    }
                    captureableView = this
                }
            }
        )

        // Capture button
        Button(
            onClick = {
                captureableView?.let { view ->
                    ScreenshotBuilder.quickCapture(
                        context = context,
                        view = view,
                        callback = object : ScreenshotCallback {
                            override fun onSuccess(file: File, bitmap: Bitmap?) {
                                resultText = "Screenshot saved: ${file.name}"
                            }
                            override fun onError(exception: Exception, message: String) {
                                resultText = "Error: $message"
                            }
                        }
                    )
                }
            }
        ) {
            Text("Capture Compose Content")
        }

        // Alternative: Capture full screen
        Button(
            onClick = {
                val activity = context as? ComponentActivity
                activity?.window?.decorView?.rootView?.let { rootView ->
                    ScreenshotBuilder(context)
                        .view(rootView)
                        .fileName("compose_fullscreen.png")
                        .capture { file, bitmap ->
                            resultText = "Full screen captured: ${file.name}"
                        }
                }
            }
        ) {
            Text("Capture Full Screen")
        }

        if (resultText.isNotEmpty()) {
            Text(resultText)
        }
    }
}
```

**Note**: Compose UI must be wrapped in `ComposeView` within `AndroidView` to be capturable as a traditional Android View. See the sample app's `ComposeActivity` for a complete working example with @Preview support.

## 💾 Storage Options

### Internal Storage (Default - No Permissions Required)

```kotlin
ScreenshotBuilder(this)
    .view(myView)
    .saveToInternal()  // App's private directory
    .capture { file, bitmap -> }
```

**Location**: `/data/data/your.package.name/files/Screenshots/`

✅ No permissions needed
✅ Secure (only your app can access)
❌ Deleted when app is uninstalled

### External Storage

```kotlin
ScreenshotBuilder(this)
    .view(myView)
    .saveToExternal()  // External storage
    .capture { file, bitmap -> }
```

**Behavior by Android Version**:
- **Android 10+ (API 29+)**: App-specific directory, no permissions needed
- **Android 6-9 (API 23-28)**: Requires `WRITE_EXTERNAL_STORAGE` permission
- **Android < 6 (API < 23)**: No runtime permissions needed

**Location**:
- Android 10+: `/Android/data/your.package/files/Pictures/Screenshots/`
- Android < 10: `/Pictures/Screenshots/`

## 🔐 Permission Handling

The library automatically handles permissions based on Android version:

### Check Permission

```kotlin
if (ScreenshotBuilder.isPermissionNeeded(this, isExternalStorage = true)) {
    // Permission is required
    ScreenshotBuilder.requestPermission(this)
}
```

### Handle Permission Result

```kotlin
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    if (PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
        // Permission granted - proceed with screenshot
        captureScreenshot()
    } else {
        // Permission denied
        Toast.makeText(this, "Permission required for external storage", Toast.LENGTH_LONG).show()
    }
}
```

### Add to Manifest (for Android 6-9)

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

## 📁 Storage Management

### Get All Screenshots

```kotlin
val screenshotCapture = ScreenshotCapture(context)
val storageManager = screenshotCapture.getStorageManager()

// Get all screenshots from internal storage
val screenshots = storageManager.getAllScreenshots(StorageLocation.INTERNAL)

screenshots.forEach { file ->
    Log.d("Screenshot", "File: ${file.name}, Size: ${file.length() / 1024}KB")
}

// With pagination
val recent10 = storageManager.getAllScreenshots(
    storageLocation = StorageLocation.INTERNAL,
    limit = 10,
    offset = 0
)
```

### Delete Screenshot

```kotlin
val deleted = storageManager.deleteScreenshot(file)
if (deleted) {
    Log.d("Screenshot", "File deleted successfully")
}
```

### Clear All Screenshots

```kotlin
val count = storageManager.clearAllScreenshots(StorageLocation.INTERNAL)
Log.d("Screenshot", "Deleted $count screenshots")
```

## 🎯 API Reference

### ScreenshotBuilder Methods

| Method | Description | Default |
|--------|-------------|---------|
| `view(View)` | Set the view to capture | Required |
| `autoDetectView()` | Auto-detect best visible view | - |
| `captureWindow()` | Capture entire window (with system UI) | - |
| `captureContent()` | Capture content view only (without system UI) | - |
| `format(CompressFormat)` | Image format (PNG/JPEG/WEBP) | PNG |
| `quality(Int)` | Quality 0-100 (JPEG/WEBP only) | 100 |
| `fileName(String)` | Custom filename | Auto-generated timestamp |
| `saveDirectory(File)` | Custom save directory | App's default directory |
| `includeBackground(Boolean)` | Include view background | `true` |
| `recycleBitmapAfterSave(Boolean)` | Auto-recycle bitmap after save | `false` |
| `returnBitmap(Boolean)` | Return bitmap in callback | `true` |
| `saveToInternal()` | Save to internal storage | Default |
| `saveToExternal()` | Save to external storage | - |
| `capture(callback)` | Execute capture with callback | - |
| `captureAsync()` | Suspend function for coroutines | - |
| `captureBitmap()` | Get bitmap without saving (sync) | - |
| `captureBitmapAsync()` | Get bitmap without saving (async) | - |
| `release()` | Release resources | Call when done |

### Quick Static Methods

```kotlin
// Quick capture with default settings
ScreenshotBuilder.quickCapture(context, view, callback)

// Auto-detect best view
ScreenshotBuilder.quickCaptureAuto(activity, callback)

// Capture window
ScreenshotBuilder.quickCaptureWindow(activity, callback)

// Capture content
ScreenshotBuilder.quickCaptureContent(activity, callback)

// Get bitmap only
ScreenshotBuilder.quickCaptureBitmap(context, view)

// Permission helpers
ScreenshotBuilder.isPermissionNeeded(context, isExternal)
ScreenshotBuilder.requestPermission(activity)
```

### ScreenshotConfig

```kotlin
data class ScreenshotConfig(
    val format: Bitmap.CompressFormat = PNG,
    val quality: Int = 100,
    val saveDirectory: File? = null,
    val fileName: String? = null,
    val includeBackground: Boolean = true,
    val recycleBitmapAfterSave: Boolean = false,
    val returnBitmap: Boolean = true
)
```

### ScreenshotCallback

```kotlin
interface ScreenshotCallback {
    fun onSuccess(file: File, bitmap: Bitmap?)
    fun onError(exception: Exception, message: String)
}
```

### ScreenshotResult (for Coroutines)

```kotlin
sealed class ScreenshotResult {
    data class Success(val file: File, val bitmap: Bitmap?) : ScreenshotResult()
    data class Error(val exception: Exception, val message: String) : ScreenshotResult()
}
```

## ⚡ Performance Tips

1. **Use `recycleBitmapAfterSave(true)`** for large screenshots to save memory
2. **Use `returnBitmap(false)`** if you only need the file path
3. **Use `captureAsync()`** with coroutines for better threading
4. **Call `release()`** on ScreenshotBuilder when done to free resources
5. **Use JPEG format** with 85-90% quality for smaller file sizes
6. **Capture on background thread** for UI responsiveness

```kotlin
// Memory-efficient capture
lifecycleScope.launch(Dispatchers.Default) {
    val result = ScreenshotBuilder(this@MainActivity)
        .view(largeView)
        .format(Bitmap.CompressFormat.JPEG)
        .quality(85)
        .recycleBitmapAfterSave(true)
        .captureAsync()

    withContext(Dispatchers.Main) {
        when (result) {
            is ScreenshotResult.Success -> {
                // Update UI
            }
            is ScreenshotResult.Error -> {
                // Show error
            }
        }
    }
}
```

## 🎬 Specialized View Handlers

The library automatically selects the best handler for each view type:

- **StandardViewHandler** - Regular views (TextView, Button, LinearLayout, etc.)
- **ImageViewHandler** - Optimized for ImageView (direct bitmap access)
- **WebViewHandler** - Captures full scrollable WebView content
- **VideoViewHandler** - Captures current video frame from VideoView
- **TextureViewHandler** - Captures TextureView content (camera, video)
- **PlayerViewHandler** - Captures Media3 PlayerView (ExoPlayer)

No configuration needed - handlers are selected automatically!

## ❌ Error Handling

```kotlin
ScreenshotBuilder(this)
    .view(myView)
    .capture(
        onSuccess = { file, bitmap ->
            // Success
        },
        onError = { exception, message ->
            when (exception) {
                is SecurityException -> {
                    // Permission denied
                    Log.e("Screenshot", "Permission issue: $message")
                }
                is IllegalStateException -> {
                    // Invalid view state or configuration
                    Log.e("Screenshot", "Invalid state: $message")
                }
                is IOException -> {
                    // Storage/file system error
                    Log.e("Screenshot", "Storage error: $message")
                }
                else -> {
                    Log.e("Screenshot", "Unknown error: $message")
                }
            }
        }
    )
```

## 📋 Requirements

- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 34 (Android 14)
- **Kotlin**: 1.9.20+
- **Dependencies**:
  - `androidx.core:core-ktx:1.12.0`
  - `androidx.appcompat:appcompat:1.6.1`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`
- **Optional Dependencies** (for PlayerView support):
  - `androidx.media3:media3-ui:1.2.0` (compile-only)
  - `androidx.media3:media3-common:1.2.0` (compile-only)

## 📱 Sample App

Check the `/app` module for a complete sample application with examples of:

**Traditional XML Layout (MainActivity)**:
- Basic view capture
- Auto-detection
- Full window capture
- ImageView, WebView, VideoView capture
- Custom configuration
- Permission handling
- Storage management
- Memory optimization

**Jetpack Compose (ComposeActivity)**:
- Capturing Compose UI via AndroidView
- Full screen capture in Compose apps
- @Preview annotations for quick UI testing
- Code examples and live demonstrations
- Modern Material3 design with black & white theme

Run the sample:
```bash
./gradlew :app:installDebug
```

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

```
Copyright 2024 bolvitz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🔗 Links

- **Maven Central**: https://search.maven.org/artifact/io.github.bolvitz/screenshot-android
- **GitHub**: https://github.com/bolvitz/android-library-screenshot
- **Issues**: https://github.com/bolvitz/android-library-screenshot/issues
- **Changelog**: https://github.com/bolvitz/android-library-screenshot/releases

## ⭐ Support

If you find this library helpful, please:
- ⭐ Star the repository
- 🐛 Report issues
- 💡 Suggest features
- 🤝 Contribute code

---

Made with ❤️ by [bolvitz](https://github.com/bolvitz)

# Consumer proguard rules for the screenshot library
# These rules are automatically applied to apps that use this library

# Keep public API visible to consumers
-keep public class com.screenshot.lib.** {
    public protected *;
}

# Keep callback interfaces for reflection/lambda usage
-keep interface com.screenshot.lib.ScreenshotCallback {
    *;
}

# Keep data classes used in public API
-keepclassmembers class com.screenshot.lib.ScreenshotConfig {
    *;
}

# Keep sealed classes hierarchy
-keepclassmembers class com.screenshot.lib.ScreenshotResult {
    *;
}

-keepclassmembers class com.screenshot.lib.ScreenshotResult$* {
    *;
}

# Keep enum members
-keepclassmembers enum com.screenshot.lib.StorageLocation {
    *;
}

# Preserve file names for better stack traces
-keepattributes SourceFile,LineNumberTable

# Keep annotations
-keepattributes *Annotation*

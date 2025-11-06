# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep public API - Main entry points
-keep public class com.screenshot.lib.ScreenshotBuilder {
    public *;
}

-keep public class com.screenshot.lib.ScreenshotCapture {
    public *;
}

-keep public class com.screenshot.lib.ScreenshotConfig {
    public *;
}

-keep public interface com.screenshot.lib.ScreenshotCallback {
    public *;
}

# Keep result types
-keep public class com.screenshot.lib.ScreenshotResult {
    public *;
}

-keep public class com.screenshot.lib.ScreenshotResult$* {
    public *;
}

# Keep storage location enum
-keep public enum com.screenshot.lib.StorageLocation {
    public *;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep StorageManager public methods (used by library consumers)
-keep public class com.screenshot.lib.storage.StorageManager {
    public *;
}

# Optimization flags for R8/ProGuard
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Coroutines optimization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Parcelable implementation
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Remove unused code
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

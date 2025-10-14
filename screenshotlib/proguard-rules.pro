# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep public API
-keep public class com.screenshot.lib.ScreenshotBuilder {
    public *;
}

-keep public class com.screenshot.lib.ScreenshotConfig {
    public *;
}

-keep public interface com.screenshot.lib.ScreenshotCallback {
    public *;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

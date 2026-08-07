# Whispry ProGuard Rules
# --------------------

# 1. Debugging & Stack Traces
# Preserve line numbers and source file names to make stack traces readable even in release builds.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. Samsung-Specific Fixes (HoneySpace / SipController)
# Samsung's One UI (specifically HoneySpace and SipController) uses reflection to inspect
# app views for "Smart Suggestions" and "Candidate Apps".
# Failure to find these internal Compose classes or their accessibility providers leads to InvocationTargetException.
-keep class androidx.compose.ui.platform.AndroidComposeView { *; }
-keep class androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat { *; }
-keep class androidx.compose.ui.platform.WrappedComposition { *; }
-keep class androidx.compose.ui.platform.ViewLayerContainer { *; }
-keep class androidx.compose.ui.platform.ViewLayer { *; }

# Keep accessibility node providers as Samsung's OS reflection-invokes getAccessibilityNodeProvider()
-keep class * extends android.view.accessibility.AccessibilityNodeProvider { *; }

# Samsung OS often looks for these specific methods via reflection.
-keepclassmembers class * extends android.view.View {
    *** notifyImeShown(...);
    public *** getAccessibilityNodeProvider();
    public *** onProvideAutofillVirtualStructure(android.view.ViewStructure, int);
    public *** onProvideContentCaptureStructure(android.view.ViewStructure, int);
}

# 3. Third-party UI Libraries (Kyant Backdrop & Capsule)
# These libraries use custom graphics and shapes that can be broken by aggressive shrinking.
-keep class com.kyant.backdrop.** { *; }
-keep class com.kyant.capsule.** { *; }

# 4. Hilt & Dependency Injection
# While Hilt usually provides its own rules, keeping these ensures that entry points
# and managers are not accidentally stripped or renamed in ways that break reflection.
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep class dagger.hilt.android.internal.managers.** { *; }

# 5. Room Persistence
# Ensure Room entities and DAOs are preserved if they are used via reflection.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.Dao

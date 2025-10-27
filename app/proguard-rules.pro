# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep data classes (for JSON parsing)
-keepclassmembers class net.clausr.ntfytvnotifications.service.NtfyMessage {
    *;
}

# Keep WebSocket listener methods
-keep class * extends okhttp3.WebSocketListener {
    <methods>;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}
-keep class * extends androidx.room.Dao
-keepclassmembers @androidx.room.Dao class * {
    *;
}

# Keep database entities
-keep class net.clausr.ntfytvnotifications.data.db.entities.** { *; }
-keepclassmembers class net.clausr.ntfytvnotifications.data.db.entities.** {
    *;
}

# Keep DAOs
-keep interface net.clausr.ntfytvnotifications.data.db.dao.** { *; }

# Keep repository classes and sealed class results
-keep class net.clausr.ntfytvnotifications.data.repository.** { *; }
-keepclassmembers class net.clausr.ntfytvnotifications.data.repository.** {
    public <methods>;
}

# Keep sealed classes for result types
-keep class net.clausr.ntfytvnotifications.data.repository.AddSubscriptionResult { *; }
-keep class net.clausr.ntfytvnotifications.data.repository.AddSubscriptionResult$* { *; }

# Keep utility classes
-keep class net.clausr.ntfytvnotifications.util.** { *; }
-keepclassmembers class net.clausr.ntfytvnotifications.util.TopicValidator {
    public <methods>;
}
-keepclassmembers class net.clausr.ntfytvnotifications.util.TopicValidator$* { *; }

# NotificationHelper - preserve public API only
# Private methods (sanitizeForNotification, showSummaryNotification, getPriorityColor, isAndroidTV)
# are intentionally not kept - they will be inlined/optimized by ProGuard
# since they're only called internally
-keep class net.clausr.ntfytvnotifications.util.NotificationHelper {
    public static <methods>;
}

# Compose - more targeted rules
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.tv.material3.** { *; }

# Lifecycle
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * implements androidx.lifecycle.LifecycleOwner {
    *;
}

# SavedState
-keep class androidx.savedstate.** { *; }

# Keep service classes
-keep class net.clausr.ntfytvnotifications.service.** { *; }

# WebSocket service singleton - must preserve getInstance
-keep class net.clausr.ntfytvnotifications.service.NtfyWebSocketService {
    public static *** getInstance(...);
    public <methods>;
}

# Room migrations
-keep class net.clausr.ntfytvnotifications.data.db.AppDatabase$Companion$MIGRATION_* { *; }

# General
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exception
-keepattributes SourceFile,LineNumberTable

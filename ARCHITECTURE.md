# Architecture Documentation

This document provides technical documentation for developers working with the Ntfy TV Notifications codebase.

## Project Overview

Ntfy TV Notifications is an Android TV app that receives real-time notifications from ntfy.sh via WebSocket and displays them as overlay notifications. The app uses a Foreground Service to maintain persistent connections and is optimized for TV remote control navigation.

---

## Critical Design Patterns

### Singleton WebSocket Service with Context Awareness
The `NtfyWebSocketService` is implemented as a singleton with the following characteristics:
- Topic is NOT part of constructor
- Call `setContext()` before using to enable network checks
- Topic passed to `connect(topic)` method to support dynamic changes
- Service maintains its own coroutine scope (Dispatchers.IO + SupervisorJob)

### Memory-Safe Overlay System
The `OverlayNotificationView` implements a custom lifecycle to prevent memory leaks:
- Creates its own custom `LifecycleOwner`/`ViewModelStoreOwner`/`SavedStateRegistryOwner`
- **NEVER pass Activity as LifecycleOwner** - this causes memory leaks
- Call `cleanup()` in Activity.onDestroy() to properly release resources
- Message queue prevents notification overlap (6s per message: 5s display + 1s gap)

### Foreground Service Pattern
- `NtfyForegroundService` (not WorkManager) maintains WebSocket connection
- Service type: `dataSync` (Android 14+ requirement)
- START_STICKY ensures restart after process kill
- Service started from MainActivity and runs independently

### Permission Handling with Activity Result API
- Two launchers registered BEFORE onCreate: `overlayPermissionLauncher` and `notificationPermissionLauncher`
- `PermissionHelper.requestOverlayPermission()` takes an `ActivityResultLauncher<Intent>` parameter
- Android 13+ requires runtime POST_NOTIFICATIONS permission request

---

## Key State Management

### WebSocket Connection State
- `connectionState: StateFlow<Boolean>` - reflects current connection status
- `messages: StateFlow<List<NtfyMessage>>` - last 100 messages (size-limited)
- `latestMessage: StateFlow<NtfyMessage?>` - triggers overlay display
- All StateFlows are read-only (exposed via asStateFlow())

### Reconnection Strategy
- Exponential backoff: 1s → 2s → 4s → 8s... up to 60s max
- Max 10 attempts before giving up
- Counter resets on successful connection
- Network check before each attempt

### Message Size Limits
- Messages list: max 100 items (uses `.takeLast(MAX_MESSAGES)`)
- Prevents unbounded memory growth
- Oldest messages automatically dropped

---

## Component Dependencies

```
MainActivity
├── NtfyWebSocketService (singleton, context-aware)
│   └── WebSocket connection to ntfy.sh
├── OverlayNotificationView (has own lifecycle)
│   └── WindowManager overlay display
├── NtfyConfig (SharedPreferences)
│   └── Topic configuration
├── AppDatabase (Room)
│   ├── SubscriptionRepository
│   └── MessageRepository
└── NtfyForegroundService (Android Service)
    └── Keeps WebSocket alive in background
```

---

## Data Flow

1. **Message Reception**:
   - WebSocket → `NtfyWebSocketService.onMessage()`
   - JSON parsing → Validation
   - Save to database via `MessageRepository`
   - Update `_latestMessage.value = message`

2. **Overlay Display**:
   - MainActivity observes `latestMessage`
   - `overlayView.enqueueMessage()`
   - Queue processing → WindowManager.addView()

3. **Persistence**:
   - Service runs independently of MainActivity
   - WebSocket survives Activity lifecycle
   - Database persists across app restarts

---

## Critical Implementation Details

### Correct Singleton Usage

```kotlin
// CORRECT
val service = NtfyWebSocketService.getInstance()
service.setContext(applicationContext)
service.connect("topic-name")

// WRONG - topic in getInstance is ignored
val service = NtfyWebSocketService.getInstance("different-topic")
```

### Correct Overlay Lifecycle

```kotlin
// CORRECT - overlay creates its own lifecycle
overlayView = OverlayNotificationView(applicationContext)
overlayView.enqueueMessage(message)

override fun onDestroy() {
    overlayView.cleanup() // Required to prevent leaks
}

// WRONG - passing Activity as lifecycle owner
overlayView.show(message, this@MainActivity) // Memory leak!
```

### Correct Permission Requests

```kotlin
// CORRECT - launchers registered as class properties BEFORE onCreate
private val overlayPermissionLauncher = registerForActivityResult(...)

override fun onCreate() {
    PermissionHelper.requestOverlayPermission(this, overlayPermissionLauncher)
}

// WRONG - deprecated API
PermissionHelper.requestOverlayPermission(this, 1001) // Old startActivityForResult
```

### Service Lifecycle

```kotlin
// Service started from MainActivity but runs independently
private fun startForegroundService() {
    val intent = Intent(this, NtfyForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent) // Required for Android O+
    }
}

// Service continues after MainActivity destroyed
// WebSocket connection persists in background
```

---

## Configuration & Customization

### Topic Configuration
- Stored in Room database via `SubscriptionRepository`
- SharedPreferences (`NtfyConfig`) exists for legacy compatibility
- Access: `subscriptionRepository.addSubscription("new-topic")`
- Persists across app restarts

### Display Duration Configuration
- Stored in SharedPreferences via `NtfyConfig`
- Property: `displayDurationSeconds` (Int, default: 5)
- Helper: `displayDurationMs` (Long, returns milliseconds)
- Access: `config.displayDurationSeconds = 7` // Set to 7 seconds
- User-configurable via Settings UI in Subscriptions screen
- Available options: 2, 3, 5 (default), 7, 10, 15 seconds
- Persists across app restarts

### Reconnection Tuning
Located in `NtfyWebSocketService.kt:42-44`:
```kotlin
private const val MAX_RECONNECT_ATTEMPTS = 10
private const val BASE_DELAY_MS = 1000L  // 1 second
private const val MAX_DELAY_MS = 60000L   // 60 seconds
```

### Message Limits
Located in `NtfyWebSocketService.kt:41`:
```kotlin
private const val MAX_MESSAGES = 100
```

### Overlay Display Duration
**User-configurable** via Settings UI in Subscriptions screen.
- Default: 5 seconds
- Available options: 2, 3, 5, 7, 10, 15 seconds
- Gap between queued notifications: Fixed at 1 second
- Implementation in `OverlayNotificationView.kt`:
  - Line 121: `delay(config.displayDurationMs + 1000)` // Queue delay
  - Line 168: `delay(config.displayDurationMs)` // Auto-dismiss timer

---

## Database Schema

### Subscription Entity
```kotlin
@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,               // Unique index
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

### StoredMessage Entity
```kotlin
@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = Subscription::class,
        parentColumns = ["topic"],
        childColumns = ["topic"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class StoredMessage(
    @PrimaryKey val id: String,
    val topic: String,
    val time: Long,
    val event: String,
    val message: String?,
    val title: String?,
    val priority: Int?,
    val tags: String? // JSON array stored as string
)
```

---

## ProGuard Considerations

ProGuard rules in `app/proguard-rules.pro` are essential for release builds:
- Keeps OkHttp WebSocket classes and methods
- Preserves Kotlin coroutines internals
- Retains data classes for JSON parsing
- Keeps Compose and Lifecycle classes
- Preserves Room entities and DAOs

**When adding new data classes for JSON parsing, add them to ProGuard rules.**

---

## Common Pitfalls

1. **Don't pass Activity as LifecycleOwner to OverlayNotificationView** - Use enqueueMessage() instead
2. **Don't call getInstance() with different topics** - Topic parameter is ignored; use connect(topic) instead
3. **Don't forget to call setContext() on WebSocketService** - Required for network checks
4. **Don't use WorkManager for persistent connections** - Use ForegroundService instead
5. **Always call cleanup() on OverlayNotificationView in onDestroy()** - Prevents memory leaks
6. **Register ActivityResultLaunchers before onCreate()** - They must be class properties
7. **Don't forget foreground notification** - Service will crash on Android O+ without it
8. **Don't commit sensitive data** - Add keystore, local.properties to .gitignore

---

## Android TV Specifics

- All interactive elements have 3dp focus border when focused
- Font sizes: 18-42sp for 10-foot viewing
- No touch gestures - navigation via D-pad only
- Semantic content descriptions for accessibility
- Remote control center button handled automatically by Compose

---

## Testing Notifications

Priority levels (1-5) map to colors: Green (1) → Blue (2-3) → Orange (4) → Red (5)

```bash
# Test all priorities
for i in {1..5}; do
  curl -H "Title: Priority $i" -H "Priority: $i" \
    -d "Test message priority $i" https://ntfy.sh/my-topic
  sleep 2
done

# Test with tags
curl -H "Title: Tagged" -H "Tags: warning,urgent" \
  -d "Message with tags" https://ntfy.sh/my-topic
```

---

## Key Files Reference

| File | Purpose |
|------|---------|
| `service/NtfyWebSocketService.kt` | WebSocket singleton, reconnection logic, message parsing |
| `service/NtfyForegroundService.kt` | Android Foreground Service for background operation |
| `ui/OverlayNotificationView.kt` | Overlay display with custom lifecycle |
| `ui/SubscriptionsScreen.kt` | Subscription management UI and notification settings |
| `data/db/AppDatabase.kt` | Room database configuration and migrations |
| `data/repository/SubscriptionRepository.kt` | Subscription CRUD operations |
| `data/repository/MessageRepository.kt` | Message persistence layer |
| `util/NtfyConfig.kt` | SharedPreferences configuration (display duration, legacy topic storage) |
| `util/PermissionHelper.kt` | Permission handling with Activity Result API |
| `util/NotificationHelper.kt` | Heads-up notifications and notification management |
| `util/TopicValidator.kt` | Topic name validation |
| `MainActivity.kt` | Main UI, permission requests, service initialization |
| `proguard-rules.pro` | Essential for release builds with minification |

---

## Build Commands

### Development
```bash
# Build debug APK
./gradlew assembleDebug

# Install debug build to connected device/emulator
./gradlew installDebug
# or
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release
```bash
# Build release APK (with ProGuard optimization)
./gradlew assembleRelease

# Build Android App Bundle (preferred for Play Store)
./gradlew bundleRelease
```

### Testing
```bash
# Run the app on connected Android TV/device
adb shell am start -n net.clausr.ntfytvnotifications/.MainActivity

# Send test notification
curl -H "Title: Test" -H "Priority: 5" -d "Test message" https://ntfy.sh/my-topic

# Check logs
adb logcat | grep -E "(Ntfy|WebSocket|Overlay)"

# Check permissions
adb shell dumpsys package net.clausr.ntfytvnotifications | grep permission
```

### Debugging
```bash
# Monitor specific components
adb logcat | grep NtfyWebSocketService
adb logcat | grep NtfyForegroundService
adb logcat | grep OverlayNotificationView

# Check service status
adb shell dumpsys activity services net.clausr.ntfytvnotifications
```

---

## Performance Considerations

### Memory
- Overlay views use custom lifecycle to prevent leaks
- Message list limited to 100 items in memory
- Database can store unlimited messages (consider cleanup strategy)
- Coroutine scopes properly canceled in onDestroy()

### Battery
- Exponential backoff reduces connection attempts
- Network checks prevent unnecessary connection attempts
- WebSocket ping interval: 30 seconds (configurable in OkHttp)
- Foreground service type: dataSync (appropriate for persistent connections)

### Threading
- WebSocket operations on Dispatchers.IO
- Database operations on Dispatchers.IO (Room default)
- UI updates on Dispatchers.Main
- Overlay operations on Dispatchers.Main.immediate (WindowManager requirement)

---

## Error Handling

### WebSocket Errors
- Connection failures trigger exponential backoff reconnection
- JSON parsing errors logged and message skipped
- Network unavailability detected before connection attempts

### Database Errors
- Foreign key violations prevented by cascade deletes
- Transaction failures logged, app continues functioning
- Database migration failures fall back to destructive migration (dev only)

### Permission Errors
- Overlay permission denied → Falls back to heads-up notifications
- Notification permission denied (Android 13+) → No notifications shown
- User notified of missing permissions via toasts

---

## Security Considerations

### Input Validation
- Topic names validated via `TopicValidator`
- Notification content sanitized in `NotificationHelper`
- Length limits enforced (title: 100 chars, message: 500 chars)

### Data Privacy
- All data stored locally (no cloud sync)
- Database unencrypted (relies on Android sandboxing)
- WebSocket traffic encrypted via TLS to ntfy.sh

### Permissions
- Minimal permissions requested
- Runtime permission checks before usage
- Clear user communication about permission purposes

---

## Contributing

When contributing to this project:

1. **Follow existing patterns** - Especially lifecycle management
2. **Test memory leaks** - Use LeakCanary or Android Studio profiler
3. **Test on real Android TV** - Emulator doesn't catch all issues
4. **Update ProGuard rules** - If adding data classes or reflection
5. **Document breaking changes** - In pull request description
6. **Test release builds** - ProGuard can break functionality

---

## Additional Resources

- [Android TV Guidelines](https://developer.android.com/training/tv)
- [Jetpack Compose for TV](https://developer.android.com/jetpack/compose/tv)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [OkHttp WebSocket](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-web-socket/)
- [ntfy.sh Documentation](https://docs.ntfy.sh)

---

## Contact

For questions or issues:
- **GitHub Issues**: https://github.com/clausilein/ntfy-tv-notifications/issues
- **Email**: android-ntfy@clausr.net

---

Last updated: January 2025

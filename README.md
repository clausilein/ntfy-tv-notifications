# Ntfy TV Notifications

An Android TV app that receives real-time notifications from [ntfy.sh](https://ntfy.sh) and displays them as overlay notifications.

## Features

- **Real-time WebSocket Connection**: Connects to ntfy.sh via WebSocket for instant notifications
- **Multiple Subscriptions**: Subscribe to and manage multiple ntfy topics simultaneously with persistent storage
- **Subscription Management UI**: Add, toggle, and delete topic subscriptions from the TV-optimized interface
- **Database Persistence**: Room database stores subscriptions and message history across app restarts
- **Foreground Service**: Uses a persistent foreground service to maintain connection in the background
- **Dual Notification System**: Overlay notifications (silent) or heads-up notifications (with sound) as fallback
- **Smart Notification Coordination**: Prevents duplicate notifications by coordinating between MainActivity and background service
- **TV-Optimized UI**: Remote-friendly interface designed for Android TV with accessibility support
- **Connection Management**: Connect/disconnect controls with live status indicator
- **Message History**: View last 100 received notifications in a scrollable list (in-memory + database storage)
- **Priority Support**: Color-coded notifications based on priority levels (1-5)
- **Tag Display**: Shows tags associated with each notification
- **Exponential Backoff**: Smart reconnection strategy to minimize battery drain
- **Network Awareness**: Checks network connectivity before attempting connections
- **Topic Validation**: Validates topic names to ensure compatibility with ntfy.sh
- **Android TV Detection**: Adapts notification behavior for TV vs mobile devices
- **Input Sanitization**: Protects against malformed notification data

## Requirements

- Android TV or Android device (API 29+)
- Internet connection
- SYSTEM_ALERT_WINDOW permission for overlay notifications
- POST_NOTIFICATIONS permission (Android 13+)

## Setup

1. **Build and Install**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Grant Permissions**
   - On first launch, the app will request required permissions:
     - **Overlay Permission (SYSTEM_ALERT_WINDOW)**: For displaying notifications on top of other apps
     - **Notification Permission (POST_NOTIFICATIONS)**: For Android 13+ devices
   - Follow the on-screen prompts to grant permissions

3. **Add Subscriptions**
   - On first launch, click "Manage Subscriptions" to add your ntfy topics
   - Add one or more topics to subscribe to
   - Topics are validated and stored in the Room database
   - Toggle subscriptions on/off without deleting them

## Usage

### Sending Test Notifications

You can send notifications to your Android TV using curl:

```bash
# Simple notification
curl -d "Hello from ntfy!" https://ntfy.sh/my-topic

# Notification with title
curl -H "Title: Test Notification" -d "This is a test message" https://ntfy.sh/my-topic

# Priority notification
curl -H "Title: Important!" -H "Priority: 5" -d "High priority message" https://ntfy.sh/my-topic

# Notification with tags
curl -H "Title: Tagged Message" -H "Tags: warning,server" -d "Server alert!" https://ntfy.sh/my-topic
```

### Using the ntfy CLI

```bash
# Install ntfy CLI
sudo snap install ntfy  # or download from https://ntfy.sh

# Send notifications
ntfy send my-topic "Hello from CLI"
ntfy send --title "Server Alert" --priority high my-topic "Disk space low"
```

### Priority Levels

- **1**: Low (Green)
- **2-3**: Default (Blue)
- **4**: High (Orange)
- **5**: Urgent (Red)

### Notification Behavior

The app uses a dual notification system:

1. **Overlay Notifications (Preferred)**: Silent, full-screen overlays when SYSTEM_ALERT_WINDOW permission is granted
   - Displayed via WindowManager
   - Auto-dismiss after 5 seconds
   - Queued to prevent overlap

2. **Heads-up Notifications (Fallback)**: Standard Android notifications with sound when overlay permission is denied or overlay is blocked by DRM content
   - Uses notification channel with sound
   - Grouped on mobile devices (not on Android TV)
   - Expandable with BigTextStyle
   - Priority-based colors

**Smart Coordination**: The app prevents duplicate notifications by coordinating between MainActivity (foreground) and ForegroundService (background). Notifications are only shown by the service when MainActivity is not visible.

## Architecture

### Components

1. **MainActivity** (`MainActivity.kt`)
   - TV-optimized Jetpack Compose UI with two screens (Main and Subscriptions)
   - Manages WebSocket service lifecycle
   - Displays message history (last 100 messages)
   - Handles permissions using Activity Result API
   - Coordinates with ForegroundService to prevent duplicate notifications
   - Accessibility support with content descriptions

2. **SubscriptionsScreen** (`ui/SubscriptionsScreen.kt`)
   - TV-optimized UI for managing topic subscriptions
   - Add, toggle, and delete subscriptions
   - Input validation with TopicValidator
   - Dialog-based interfaces for add/delete operations

3. **NtfyWebSocketService** (`service/NtfyWebSocketService.kt`)
   - Singleton service managing WebSocket connections to ntfy.sh
   - Supports multiple topics via ntfy.sh multi-topic API (comma-separated)
   - Parses incoming JSON messages with validation
   - Saves messages to database via MessageRepository
   - Provides StateFlow for reactive UI updates
   - Exponential backoff reconnection strategy (1s to 60s, max 10 attempts)
   - Network connectivity checks before connection attempts
   - Message list size limit (100 messages in-memory)
   - Thread-safe and context-aware

4. **NtfyForegroundService** (`service/NtfyForegroundService.kt`)
   - Android Foreground Service with dataSync type
   - Keeps WebSocket connection alive in the background
   - Observes latestMessage flow and displays notifications when MainActivity is backgrounded
   - Shows persistent foreground notification indicating service status
   - START_STICKY flag ensures service restart after termination
   - Proper lifecycle management with coroutine scope cleanup

5. **OverlayNotificationView** (`ui/OverlayNotificationView.kt`)
   - Displays notifications as full-screen overlay using WindowManager
   - Custom lifecycle owner to prevent memory leaks
   - Message queue system to prevent notification overlap
   - Auto-dismisses after 5 seconds with 1s gap between messages
   - Proper error handling for SecurityException and BadTokenException
   - Accessibility support

6. **NotificationHelper** (`util/NotificationHelper.kt`)
   - Manages heads-up notifications with sound as fallback when overlay is blocked
   - Android TV detection for platform-specific behavior
   - Notification grouping on mobile devices
   - Input sanitization for security (title, message, tags)
   - Notification ID management with wraparound
   - Priority-based colors and accessibility support
   - BigTextStyle for expanded view

7. **AppDatabase** (`data/db/AppDatabase.kt`)
   - Room database with version 2 schema
   - Entities: Subscription, StoredMessage
   - DAOs: SubscriptionDao, MessageDao
   - Database migrations with foreign key support
   - Singleton instance management

8. **SubscriptionRepository** (`data/repository/SubscriptionRepository.kt`)
   - Repository pattern for subscription management
   - CRUD operations for subscriptions
   - Duplicate prevention
   - Flow-based reactive data access

9. **MessageRepository** (`data/repository/MessageRepository.kt`)
   - Repository pattern for message persistence
   - Saves incoming messages to database
   - Query messages by topic

10. **TopicValidator** (`util/TopicValidator.kt`)
    - Validates topic names against ntfy.sh requirements
    - Checks length (1-64 characters)
    - Validates allowed characters (a-z, A-Z, 0-9, -, _)
    - Prevents invalid characters (/, commas, whitespace)

11. **PermissionHelper** (`util/PermissionHelper.kt`)
    - Manages SYSTEM_ALERT_WINDOW permission using modern Activity Result API
    - Helper methods for permission checks

12. **NtfyConfig** (`util/NtfyConfig.kt`)
    - Legacy configuration manager using SharedPreferences
    - Note: Subscription management now primarily uses Room database

## Permissions

- **INTERNET**: Required for WebSocket connection
- **SYSTEM_ALERT_WINDOW**: Required for overlay notifications
- **FOREGROUND_SERVICE**: For persistent background service
- **FOREGROUND_SERVICE_DATA_SYNC**: For Android 14+ foreground service type
- **POST_NOTIFICATIONS**: For notification support (Android 13+)
- **ACCESS_NETWORK_STATE**: For network connectivity checks

## TV Optimization

- Large touch targets (minimum 48dp)
- High-contrast UI elements
- Clear focus indicators on all interactive elements (3dp border when focused)
- Remote control navigation support (D-pad and center button)
- No touch-specific gestures required
- Font sizes optimized for 10-foot viewing distance (18-42sp)
- Semantic content descriptions for screen readers

## Code Quality & Best Practices

### Architecture
- ✅ Repository pattern for data access layer
- ✅ Clean separation of concerns (UI, data, service layers)
- ✅ Room database with migrations for schema evolution
- ✅ Dependency injection via constructor parameters

### Memory Management
- ✅ Custom lifecycle owner for overlay views prevents Activity leaks
- ✅ Proper coroutine scope management with SupervisorJob
- ✅ Message list size limit (100 in-memory) prevents unbounded growth
- ✅ Explicit cleanup in onDestroy()
- ✅ Flow collectors set up once in onCreate to prevent memory leaks

### Background Processing
- ✅ Foreground Service for persistent WebSocket connections
- ✅ Proper service lifecycle with START_STICKY
- ✅ Notification channel creation for Android O+
- ✅ Smart coordination between MainActivity and service to prevent duplicates

### Modern Android APIs
- ✅ Activity Result API instead of deprecated startActivityForResult
- ✅ Runtime permission requests for Android 13+
- ✅ Jetpack Compose with TV Material 3
- ✅ Kotlin Coroutines and Flow for reactive programming
- ✅ Room database for local persistence

### Data Validation & Security
- ✅ Topic name validation with TopicValidator
- ✅ Input sanitization for notification content (prevents injection attacks)
- ✅ Length limits on user-provided strings
- ✅ Foreign key constraints in database
- ✅ JSON parsing with validation and null safety

### Network Resilience
- ✅ Exponential backoff reconnection strategy
- ✅ Network connectivity checks before connection attempts
- ✅ Maximum reconnection attempt limit (10 attempts)
- ✅ Ping interval (30s) for connection health monitoring
- ✅ Thread-safe connection state management with Mutex

### Error Handling
- ✅ Specific exception handling (SecurityException, BadTokenException)
- ✅ Comprehensive logging for debugging
- ✅ Graceful degradation when permissions are denied
- ✅ Database operation error handling with try-catch
- ✅ Flow error handling with catch operators

### ProGuard Configuration
- ✅ Rules for OkHttp, Kotlin Coroutines, and Compose
- ✅ Keeps WebSocket listener methods
- ✅ Preserves data classes and service classes
- ✅ Room database entity preservation
- ✅ Source file and line number information for debugging

## Customization

### Manage Subscriptions

Subscriptions are stored in the Room database and managed via the UI:

1. **From UI**: Click "Manage Subscriptions" button in the main screen
2. **Add topics**: Enter topic names (validated automatically)
3. **Toggle**: Enable/disable subscriptions without deleting them
4. **Delete**: Remove subscriptions (also deletes associated messages)

**Programmatic Access**:
```kotlin
// Access via repository
val subscriptionRepository = SubscriptionRepository(database.subscriptionDao(), messageRepository)

// Add a subscription
subscriptionRepository.addSubscription("your-topic")

// Toggle status
subscriptionRepository.toggleSubscriptionStatus(subscriptionId, isActive = true)

// Delete with messages
subscriptionRepository.deleteSubscriptionWithMessages(subscription)
```

### Adjust Overlay Duration

Edit `OverlayNotificationView.kt:116` to change the display duration:
```kotlin
delay(6000) // 5s display + 1s gap (change as needed)
```

### Modify Priority Colors

Priority colors are defined in `OverlayNotificationView.kt:339-347`

### Reconnection Strategy

Adjust backoff parameters in `NtfyWebSocketService.kt:42-44`:
```kotlin
private const val MAX_RECONNECT_ATTEMPTS = 10
private const val BASE_DELAY_MS = 1000L
private const val MAX_DELAY_MS = 60000L
```

## Troubleshooting

### Overlay Not Showing

1. Check if SYSTEM_ALERT_WINDOW permission is granted
2. Navigate to Settings > Apps > Ntfy TV Notifications > Permissions
3. Enable "Display over other apps"
4. Check logs: `adb logcat | grep Ntfy`

### Connection Issues

1. Verify internet connection: `adb shell ping ntfy.sh`
2. Check if ntfy.sh is accessible from your network
3. Review WebSocket connection logs: `adb logcat | grep WebSocket`
4. Try manual reconnection using the Connect button

### Messages Not Appearing

1. Check that you have active subscriptions:
   - Open "Manage Subscriptions" to view your topics
   - Ensure at least one subscription is toggled ON (active)
   - Verify the topic name matches what you're sending to
2. Check WebSocket connection status in app UI (should show "Connected")
3. Ensure you're sending to a subscribed topic: `curl -d "Test" https://ntfy.sh/YOUR_TOPIC`
4. Verify message format matches ntfy.sh specifications
5. Check logs: `adb logcat | grep NtfyWebSocketService`

### No Subscriptions / Can't Add Topic

1. Verify topic name follows rules:
   - 1-64 characters
   - Only letters (a-z, A-Z), numbers (0-9), hyphens (-), and underscores (_)
   - No spaces, slashes, or commas
2. Check database: `adb shell run-as net.clausr.ntfytvnotifications ls databases/`
3. Try clearing app data and re-adding subscriptions

### Service Stops Running

1. Check if the foreground service notification is visible
2. Ensure battery optimization is disabled for the app
3. On some devices, manually allow background execution
4. Check service logs: `adb logcat | grep NtfyForegroundService`

### Permission Denied Errors

1. For Android 13+, grant POST_NOTIFICATIONS permission
2. For overlay, grant SYSTEM_ALERT_WINDOW permission
3. Check permission status: `adb shell dumpsys package net.clausr.ntfytvnotifications | grep permission`

## Dependencies

- **OkHttp**: WebSocket client for real-time connections
- **Kotlin Coroutines**: Async operations and reactive programming
- **Jetpack Compose**: UI framework with TV Material 3 components
- **AndroidX Lifecycle**: Lifecycle-aware components
- **AndroidX SavedState**: State preservation
- **Room Database**: Local database for subscription and message persistence
- **WorkManager**: Background task scheduling (present but not primary mechanism)

## Development

### Building Release APK

```bash
./gradlew assembleRelease
```

The APK will include ProGuard optimization for reduced size and improved performance.

### Testing

Send test notifications:
```bash
# Test all priority levels
for i in {1..5}; do
  curl -H "Title: Priority $i Test" -H "Priority: $i" \
    -d "Testing priority level $i" https://ntfy.sh/my-topic
  sleep 2
done
```

### Debugging

Enable verbose logging:
```bash
adb logcat | grep -E "(Ntfy|WebSocket|Overlay)"
```

## Known Limitations

- In-memory message list limited to last 100 messages (database stores more)
- Overlay notifications require SYSTEM_ALERT_WINDOW permission
- Heads-up notifications require POST_NOTIFICATIONS permission (Android 13+)
- Maximum 10 reconnection attempts before giving up
- WebSocket connection may be killed by aggressive battery optimizers
- Topic names restricted to alphanumeric characters, hyphens, and underscores (ntfy.sh requirement)

## Future Improvements

- [ ] Notification sound configuration (currently uses system default)
- [ ] Export/import message history
- [ ] Custom notification templates
- [ ] Message filtering by tags or priority
- [ ] Dark/Light theme toggle
- [ ] Per-topic notification settings (sound, priority, overlay behavior)
- [ ] Message search functionality
- [ ] Notification statistics and analytics

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Author & Contact

**Claus Regenbrecht**
- Email: android-ntfy@clausr.net
- GitHub: [@clausilein](https://github.com/clausilein)
- Repository: [ntfy-tv-notifications](https://github.com/clausilein/ntfy-tv-notifications)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

For bug reports and feature requests, please use [GitHub Issues](https://github.com/clausilein/ntfy-tv-notifications/issues).

## Acknowledgments

This app uses several excellent open-source libraries. See [ATTRIBUTIONS.md](ATTRIBUTIONS.md) for full credits.

## Links

- [ntfy.sh](https://ntfy.sh) - Simple notification service
- [ntfy Documentation](https://docs.ntfy.sh)
- [Android TV Guidelines](https://developer.android.com/training/tv)
- [Jetpack Compose for TV](https://developer.android.com/jetpack/compose/tv)

## Changelog

### Version 2.0 (Current)
- ✅ **Multiple Subscriptions**: Add and manage multiple ntfy topics
- ✅ **Database Persistence**: Room database for subscriptions and messages
- ✅ **Subscription Management UI**: TV-optimized interface for managing topics
- ✅ **Repository Pattern**: Clean architecture with data repositories
- ✅ **Topic Validation**: Automatic validation of topic names
- ✅ **Dual Notification System**: Overlay (silent) and heads-up (with sound) notifications
- ✅ **Smart Coordination**: Prevents duplicate notifications between MainActivity and service
- ✅ **Android TV Detection**: Platform-specific notification behavior
- ✅ **Input Sanitization**: Security improvements for notification content
- ✅ **Database Migrations**: Support for schema evolution
- ✅ **Multi-topic WebSocket**: Connect to multiple topics in a single WebSocket connection
- ✅ **Enhanced Error Handling**: Comprehensive exception handling throughout

### Version 1.0
- ✅ Fixed critical memory leak in overlay lifecycle management
- ✅ Implemented Foreground Service for persistent connections
- ✅ Updated to Activity Result API (removed deprecated startActivityForResult)
- ✅ Added runtime permission requests for Android 13+
- ✅ Implemented exponential backoff reconnection strategy
- ✅ Added network connectivity checks
- ✅ Implemented message list size limit (100 messages)
- ✅ Added comprehensive ProGuard rules
- ✅ Improved error handling with specific exception types
- ✅ Added accessibility support with content descriptions
- ✅ Implemented message queue to prevent notification overlap
- ✅ Added proper lifecycle cleanup

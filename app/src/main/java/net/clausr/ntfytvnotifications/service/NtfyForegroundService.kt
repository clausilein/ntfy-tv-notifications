package net.clausr.ntfytvnotifications.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import net.clausr.ntfytvnotifications.MainActivity
import net.clausr.ntfytvnotifications.R
import net.clausr.ntfytvnotifications.data.db.AppDatabase
import net.clausr.ntfytvnotifications.data.repository.MessageRepository
import net.clausr.ntfytvnotifications.data.repository.SubscriptionRepository
import net.clausr.ntfytvnotifications.ui.OverlayNotificationView
import net.clausr.ntfytvnotifications.util.NotificationHelper
import net.clausr.ntfytvnotifications.util.NtfyConfig
import net.clausr.ntfytvnotifications.util.PermissionHelper

class NtfyForegroundService : Service() {

    private val TAG = "NtfyForegroundService"
    // Use Main.immediate dispatcher for WindowManager operations
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private lateinit var config: NtfyConfig
    private lateinit var database: AppDatabase
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var ntfyService: NtfyWebSocketService

    // Nullable to allow defensive programming
    private var overlayView: OverlayNotificationView? = null

    // Flag to ensure collectors are only set up once
    private var collectorsInitialized = false

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "ntfy_service_channel"

        // Track MainActivity foreground state to prevent duplicate notifications
        private val isMainActivityForeground = AtomicBoolean(false)

        /**
         * Called from MainActivity to coordinate notification display.
         * When MainActivity is in foreground, it handles notifications.
         * When backgrounded, service takes over.
         */
        fun setMainActivityState(isForeground: Boolean) {
            isMainActivityForeground.set(isForeground)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Initialize configuration
        config = NtfyConfig(applicationContext)

        // Initialize database and repositories
        database = AppDatabase.getInstance(applicationContext)
        messageRepository = MessageRepository(database.messageDao())
        subscriptionRepository = SubscriptionRepository(database.subscriptionDao(), messageRepository)

        // Initialize WebSocket service with dependencies
        ntfyService = NtfyWebSocketService.getInstance(subscriptionRepository, messageRepository)
        ntfyService.setContext(applicationContext)

        // Initialize overlay view for showing notifications in background
        overlayView = OverlayNotificationView(applicationContext, config)

        // Initialize notification channels for message notifications
        NotificationHelper.createNotificationChannels(applicationContext)

        createNotificationChannel()

        // Set up flow collectors ONCE in onCreate to avoid memory leaks
        setupFlowCollectors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        // Start foreground service with notification
        val notification = createNotification("Starting", "Initializing notification service")
        startForeground(NOTIFICATION_ID, notification)

        // Connect to all active subscriptions
        // Collectors are already set up in onCreate(), so just connect
        ntfyService.connectToActiveSubscriptions()

        // START_STICKY ensures service is restarted if killed
        return START_STICKY
    }

    /**
     * Sets up flow collectors ONCE during onCreate to avoid memory leaks.
     * Multiple calls to onStartCommand won't create duplicate collectors.
     */
    private fun setupFlowCollectors() {
        // Prevent duplicate collector setup
        if (collectorsInitialized) {
            Log.d(TAG, "Flow collectors already initialized, skipping")
            return
        }
        collectorsInitialized = true

        Log.d(TAG, "Setting up flow collectors")

        // Observe connection state and update foreground notification
        scope.launch(Dispatchers.Main.immediate) {
            ntfyService.connectionState
                .catch { e ->
                    Log.e(TAG, "Error collecting connection state", e)
                    emit(false) // Default to disconnected on error
                }
                .collect { isConnected ->
                    try {
                        val activeSubscriptions = subscriptionRepository.getActiveSubscriptions().first()
                        val subscriptionText = when {
                            activeSubscriptions.isEmpty() -> "No active subscriptions"
                            activeSubscriptions.size == 1 -> "Listening to ${activeSubscriptions[0].topic}"
                            else -> "Listening to ${activeSubscriptions.size} topics"
                        }

                        updateNotification(
                            if (isConnected) "Connected" else "Disconnected",
                            if (isConnected) subscriptionText else "Attempting to reconnect..."
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating notification from connection state", e)
                    }
                }
        }

        // Observe incoming messages and show notifications
        // This is the critical fix for background notifications
        scope.launch(Dispatchers.Main.immediate) {
            ntfyService.latestMessage
                .catch { e ->
                    Log.e(TAG, "Error collecting messages", e)
                    emit(null) // Continue collecting even if there's an error
                }
                .collect { message ->
                    message?.let {
                        try {
                            showNotificationForMessage(it)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error showing notification for message", e)
                        }
                    }
                }
        }
    }

    /**
     * Shows notification for a received message.
     * Coordinates with MainActivity to prevent duplicate notifications.
     */
    private fun showNotificationForMessage(message: NtfyMessage) {
        // Skip if MainActivity is in foreground (it will handle notifications)
        if (isMainActivityForeground.get()) {
            Log.d(TAG, "MainActivity is foreground, skipping service notification for: ${message.title}")
            return
        }

        Log.d(TAG, "Message received in background: ${message.title}")

        // Check permissions and show appropriate notification type
        val hasOverlay = PermissionHelper.hasOverlayPermission(applicationContext)
        val hasNotification = NotificationHelper.hasNotificationPermission(applicationContext)

        when {
            hasOverlay -> {
                // Preferred: Show overlay notification
                overlayView?.enqueueMessage(message)
                Log.d(TAG, "Showing overlay notification for: ${message.title}")
            }
            hasNotification -> {
                // Fallback: Show heads-up notification
                NotificationHelper.showMessageNotification(applicationContext, message)
                Log.d(TAG, "Showing heads-up notification for: ${message.title}")
            }
            else -> {
                Log.w(TAG, "No notification permissions available for: ${message.title}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        ntfyService.disconnect()
        overlayView?.cleanup()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ntfy Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps ntfy notification service running"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notification = createNotification(title, text)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

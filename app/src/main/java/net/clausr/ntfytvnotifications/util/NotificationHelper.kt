package net.clausr.ntfytvnotifications.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import net.clausr.ntfytvnotifications.MainActivity
import net.clausr.ntfytvnotifications.R
import net.clausr.ntfytvnotifications.service.NtfyMessage
import java.util.concurrent.atomic.AtomicInteger

/**
 * Helper class for managing notifications and sounds.
 * Provides both sound alerts and heads-up notifications as fallback for when overlay is blocked.
 */
object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "ntfy_messages"
    private const val CHANNEL_NAME = "Ntfy Messages"

    // Notification ID management
    // Range: 2000-9999 for individual messages (8000 IDs available)
    // ID 1999: Reserved for group summary notification
    // ID 1-1998: Reserved for future use (service notifications, etc.)
    // Wraparound: When MAX_NOTIFICATION_ID is reached, counter resets to INITIAL_NOTIFICATION_ID
    // This allows ~8000 concurrent notifications before ID reuse (far exceeds practical limits)
    private const val INITIAL_NOTIFICATION_ID = 2000
    private const val MAX_NOTIFICATION_ID = 9999
    private val notificationIdCounter = AtomicInteger(INITIAL_NOTIFICATION_ID)

    // Notification grouping
    private const val GROUP_KEY = "ntfy_message_group"
    private const val SUMMARY_NOTIFICATION_ID = 1999

    // Active notification tracking (for smart summary display)
    private val activeNotificationIds = mutableSetOf<Int>()

    // Input sanitization limits
    // Title: 100 chars - balances readability with notification space constraints
    // Message: 500 chars - allows detailed messages while preventing notification overflow
    // Tags: 50 chars each, max 5 tags - prevents UI crowding and excessive processing
    // Note: BigTextStyle allows expanded view for full message content
    private const val MAX_TITLE_LENGTH = 100
    private const val MAX_MESSAGE_LENGTH = 500
    private const val MAX_TAG_LENGTH = 50
    private const val MAX_TAGS_COUNT = 5

    /**
     * Sanitizes a string for safe display in notifications.
     * Limits length, removes/replaces newlines, and trims whitespace.
     * Optimized to minimize string allocations.
     */
    private fun String.sanitizeForNotification(maxLength: Int): String {
        if (this.isEmpty()) return this

        // Trim first to potentially reduce processing length
        val trimmed = this.trim()
        if (trimmed.isEmpty()) return trimmed

        // Take max length first to limit processing
        val limited = if (trimmed.length > maxLength) trimmed.substring(0, maxLength) else trimmed

        // Only process if newlines are present
        if (!limited.contains('\n') && !limited.contains('\r')) {
            return limited
        }

        // Single pass replacement for newlines
        return buildString(limited.length) {
            var i = 0
            while (i < limited.length) {
                when {
                    i < limited.length - 1 && limited[i] == '\r' && limited[i + 1] == '\n' -> {
                        append(' ')
                        i += 2 // Skip both \r and \n
                    }
                    limited[i] == '\n' || limited[i] == '\r' -> {
                        append(' ')
                        i++
                    }
                    else -> {
                        append(limited[i])
                        i++
                    }
                }
            }
        }
    }

    /**
     * Checks if the app is running on Android TV.
     */
    private fun isAndroidTV(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /**
     * Checks if POST_NOTIFICATIONS permission is granted (Android 13+).
     * On older versions, always returns true as permission is not required.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Permission not required on older versions
            true
        }
    }

    /**
     * Initializes notification channels. Call this once during app startup.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Check if channel already exists
            val existingChannel = manager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel != null) {
                Log.d(TAG, "Notification channel already exists")
                return
            }

            // Channel for incoming messages
            val messageChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // HIGH for heads-up display
            ).apply {
                description = "Notifications for incoming ntfy messages"
                setShowBadge(true)
                enableVibration(false) // TV devices don't vibrate

                // Use default notification sound
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(soundUri, AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build())
            }

            manager.createNotificationChannel(messageChannel)
            Log.d(TAG, "Notification channels created")
        }
    }

    /**
     * Shows a heads-up notification for a message with sound (via channel).
     * This works even when overlay is blocked by DRM apps.
     */
    fun showMessageNotification(context: Context, message: NtfyMessage) {
        // Check permission first (Android 13+)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot show notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists (defensive programming)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel == null) {
                Log.w(TAG, "Notification channel not found, recreating")
                createNotificationChannels(context)
            }
        }

        // Thread-safe ID generation with wraparound
        val notificationId = notificationIdCounter.getAndUpdate { current ->
            if (current >= MAX_NOTIFICATION_ID) INITIAL_NOTIFICATION_ID else current + 1
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Sanitize all input strings
        val sanitizedTitle = message.title?.sanitizeForNotification(MAX_TITLE_LENGTH)
            ?: context.getString(R.string.notification_default_title)
        val sanitizedMessage = message.message?.sanitizeForNotification(MAX_MESSAGE_LENGTH)
            ?: context.getString(R.string.notification_default_message)

        // Detect if running on Android TV
        val isTV = isAndroidTV(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(sanitizedTitle)
            .setContentText(sanitizedMessage)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            // Only group on mobile devices, not on TV (TV has different notification UI)
            .apply {
                if (!isTV) {
                    setGroup(GROUP_KEY)
                }
            }
            // Accessibility: ticker text announced by screen readers
            .setTicker(context.getString(R.string.notification_ticker, sanitizedTitle))
            .apply {
                // Add expanded text for longer messages
                if (!message.message.isNullOrEmpty()) {
                    setStyle(NotificationCompat.BigTextStyle()
                        .bigText(sanitizedMessage)
                        .setBigContentTitle(sanitizedTitle))
                }

                // Set color and accessibility info based on priority
                message.priority?.let { priority ->
                    color = getPriorityColor(priority)
                    // Accessibility: content description for priority
                    val contentInfo = when (priority) {
                        5 -> context.getString(R.string.notification_priority_urgent)
                        4 -> context.getString(R.string.notification_priority_high)
                        else -> context.getString(R.string.notification_priority_normal)
                    }
                    setContentInfo(contentInfo)
                }

                // Add tags if present (sanitized and limited)
                if (!message.tags.isNullOrEmpty()) {
                    val sanitizedTags = message.tags
                        .take(MAX_TAGS_COUNT)
                        .map { it.sanitizeForNotification(MAX_TAG_LENGTH) }
                        .joinToString(", ")
                    setSubText(sanitizedTags)
                }
            }
            .build()

        // Use unique notification ID so multiple messages can be shown
        notificationManager.notify(notificationId, notification)

        // Track active notification and show summary only on mobile when needed
        if (!isTV) {
            synchronized(activeNotificationIds) {
                activeNotificationIds.add(notificationId)

                // Only show summary if we have 2+ notifications
                if (activeNotificationIds.size >= 2) {
                    showSummaryNotification(context, notificationManager)
                }
            }
        }

        Log.d(TAG, "Notification shown with ID $notificationId (active: ${activeNotificationIds.size}): $sanitizedTitle")
    }

    /**
     * Shows a summary notification for grouping multiple messages.
     * Only called on mobile devices when 2+ notifications are active.
     */
    private fun showSummaryNotification(context: Context, notificationManager: NotificationManager) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            SUMMARY_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_group_title))
            .setContentText(context.getString(R.string.notification_group_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
    }

    /**
     * Removes a notification ID from tracking when dismissed.
     * Call this from a BroadcastReceiver that handles notification dismissals.
     */
    fun onNotificationDismissed(notificationId: Int) {
        synchronized(activeNotificationIds) {
            activeNotificationIds.remove(notificationId)
        }
    }

    private fun getPriorityColor(priority: Int): Int {
        return when (priority) {
            5 -> 0xFFFF0000.toInt() // Red - Max/Urgent
            4 -> 0xFFFF9800.toInt() // Orange - High
            3 -> 0xFF2196F3.toInt() // Blue - Default
            2 -> 0xFF4CAF50.toInt() // Green - Low
            1 -> 0xFF9E9E9E.toInt() // Gray - Min
            else -> 0xFF2196F3.toInt() // Blue - Default
        }
    }
}

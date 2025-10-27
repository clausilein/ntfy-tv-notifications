package net.clausr.ntfytvnotifications.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.clausr.ntfytvnotifications.service.NtfyMessage
import net.clausr.ntfytvnotifications.ui.theme.NtfyTVNotificationsTheme
import net.clausr.ntfytvnotifications.util.NotificationHelper
import net.clausr.ntfytvnotifications.util.PermissionHelper

class OverlayNotificationView(private val context: Context) {

    private val TAG = "OverlayNotificationView"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val messageQueue = mutableListOf<NtfyMessage>()
    private var isProcessingQueue = false

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var isShowing by mutableStateOf(false)
    private var currentMessage by mutableStateOf<NtfyMessage?>(null)
    private var dismissJob: Job? = null

    // Custom lifecycle owner for the overlay view - prevents memory leaks
    private val overlayLifecycleOwner = object : LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle = lifecycleRegistry
        override val viewModelStore: ViewModelStore = store
        override val savedStateRegistry: SavedStateRegistry =
            savedStateRegistryController.savedStateRegistry

        fun create() {
            savedStateRegistryController.performAttach()
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        }
    }

    /**
     * Enqueues a message for display. Messages are shown one at a time with a queue.
     */
    fun enqueueMessage(message: NtfyMessage) {
        messageQueue.add(message)
        Log.d(TAG, "Message enqueued. Queue size: ${messageQueue.size}")
        processQueue()
    }

    private fun processQueue() {
        if (isProcessingQueue || messageQueue.isEmpty()) {
            return
        }

        isProcessingQueue = true
        scope.launch {
            while (messageQueue.isNotEmpty()) {
                val message = messageQueue.removeAt(0)
                showInternal(message)
                delay(6000) // Show for 5s + 1s gap
            }
            isProcessingQueue = false
        }
    }

    @SuppressLint("InflateParams")
    private suspend fun showInternal(message: NtfyMessage): Boolean {
        // Check permission before attempting to show
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.e(TAG, "Cannot show overlay: permission not granted")
            return false
        }

        if (isShowing) {
            hideInternal()
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        val layoutParams = createLayoutParams()

        currentMessage = message

        // Create overlay view if it doesn't exist
        if (overlayView == null) {
            overlayLifecycleOwner.create()

            overlayView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(overlayLifecycleOwner)
                setViewTreeViewModelStoreOwner(overlayLifecycleOwner)
                setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)
                setContent {
                    NtfyTVNotificationsTheme {
                        currentMessage?.let { NotificationCard(it) }
                    }
                }
            }
        }

        return try {
            windowManager?.addView(overlayView, layoutParams)
            isShowing = true

            // Auto-dismiss after 5 seconds
            dismissJob?.cancel()
            dismissJob = scope.launch {
                delay(5000)
                hideInternal()
            }
            Log.d(TAG, "Overlay shown: ${message.title}")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException showing overlay: permission may have been revoked", e)
            isShowing = false
            overlayView = null
            // Fallback to heads-up notification
            NotificationHelper.showMessageNotification(context, message)
            false
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "BadTokenException: window token invalid or overlay blocked by FLAG_SECURE", e)
            isShowing = false
            overlayView = null
            // Fallback to heads-up notification (likely DRM app blocking overlay)
            NotificationHelper.showMessageNotification(context, message)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
            isShowing = false
            overlayView = null
            // Fallback to heads-up notification
            NotificationHelper.showMessageNotification(context, message)
            false
        }
    }

    private fun hideInternal() {
        dismissJob?.cancel()
        dismissJob = null

        try {
            if (isShowing && overlayView != null) {
                windowManager?.removeView(overlayView)
                Log.d(TAG, "Overlay hidden")
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "View not attached to window manager", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlay", e)
        } finally {
            isShowing = false
        }
    }

    fun hide() {
        scope.launch {
            hideInternal()
        }
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up overlay view")
        messageQueue.clear()
        isProcessingQueue = false
        dismissJob?.cancel()
        dismissJob = null

        try {
            if (overlayView != null && isShowing) {
                windowManager?.removeView(overlayView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        } finally {
            overlayView = null
            windowManager = null
            isShowing = false
            overlayLifecycleOwner.destroy()
            scope.cancel()
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 50 // Offset from top
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    private fun NotificationCard(message: NtfyMessage) {
        val contentDesc = buildString {
            append("Notification: ")
            append(message.title ?: "No title")
            if (!message.message.isNullOrEmpty()) {
                append(". Message: ${message.message}")
            }
            message.priority?.let { append(". Priority: ${getPriorityLabel(it)}") }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 24.dp)
                .semantics { contentDescription = contentDesc }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = getPriorityColor(message.priority ?: 3),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.title ?: "Notification",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = Color.White
                    )

                    message.priority?.let { priority ->
                        Text(
                            text = getPriorityLabel(priority),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                if (!message.message.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 22.sp
                        ),
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }

                if (!message.tags.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        message.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getPriorityColor(priority: Int): Color {
        return when (priority) {
            1 -> Color(0xFF4CAF50) // Low - Green
            2 -> Color(0xFF2196F3) // Default - Blue
            3 -> Color(0xFF2196F3) // Default - Blue
            4 -> Color(0xFFFF9800) // High - Orange
            5 -> Color(0xFFF44336) // Urgent - Red
            else -> Color(0xFF2196F3) // Default - Blue
        }
    }

    private fun getPriorityLabel(priority: Int): String {
        return when (priority) {
            1 -> "LOW"
            2 -> "DEFAULT"
            3 -> "DEFAULT"
            4 -> "HIGH"
            5 -> "URGENT"
            else -> "DEFAULT"
        }
    }
}

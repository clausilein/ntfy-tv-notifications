package net.clausr.ntfytvnotifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.clausr.ntfytvnotifications.data.db.AppDatabase
import net.clausr.ntfytvnotifications.data.repository.MessageRepository
import net.clausr.ntfytvnotifications.data.repository.SubscriptionRepository
import net.clausr.ntfytvnotifications.service.NtfyForegroundService
import net.clausr.ntfytvnotifications.service.NtfyMessage
import net.clausr.ntfytvnotifications.service.NtfyWebSocketService
import net.clausr.ntfytvnotifications.ui.OverlayNotificationView
import net.clausr.ntfytvnotifications.ui.SubscriptionsScreen
import net.clausr.ntfytvnotifications.ui.theme.NtfyTVNotificationsTheme
import net.clausr.ntfytvnotifications.util.NtfyConfig
import net.clausr.ntfytvnotifications.util.NotificationHelper
import net.clausr.ntfytvnotifications.util.PermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var ntfyService: NtfyWebSocketService
    private lateinit var overlayView: OverlayNotificationView
    private lateinit var config: NtfyConfig
    private var messageCollectionJob: Job? = null

    // Activity Result Launchers - registered before onCreate
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Overlay permission required for notifications",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission recommended for alerts",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize configuration
        config = NtfyConfig(applicationContext)

        // Initialize database and repositories
        database = AppDatabase.getInstance(applicationContext)
        messageRepository = MessageRepository(database.messageDao())
        subscriptionRepository = SubscriptionRepository(database.subscriptionDao(), messageRepository)

        // Initialize services
        ntfyService = NtfyWebSocketService.getInstance(subscriptionRepository, messageRepository)
        ntfyService.setContext(applicationContext)
        overlayView = OverlayNotificationView(applicationContext)

        setContent {
            var showSubscriptionsScreen by remember { mutableStateOf(false) }

            NtfyTVNotificationsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showSubscriptionsScreen) {
                        SubscriptionsScreen(
                            subscriptionRepository = subscriptionRepository,
                            onBackPressed = {
                                showSubscriptionsScreen = false
                                // Reconnect with updated subscriptions
                                ntfyService.connectToActiveSubscriptions()
                            }
                        )
                    } else {
                        MainScreen(
                            onManageSubscriptions = { showSubscriptionsScreen = true }
                        )
                    }
                }
            }
        }

        // Create notification channels
        createNotificationChannels()

        // Request permissions
        requestPermissions()

        // Start WebSocket connection to all active subscriptions
        ntfyService.connectToActiveSubscriptions()

        // Observe messages and show notifications
        messageCollectionJob = lifecycleScope.launch {
            ntfyService.latestMessage.collect { message ->
                message?.let {
                    if (PermissionHelper.hasOverlayPermission(this@MainActivity)) {
                        // Try to show overlay (silent, no sound)
                        overlayView.enqueueMessage(it)
                    } else {
                        // Fallback to heads-up notification with sound (via channel)
                        NotificationHelper.showMessageNotification(applicationContext, it)
                    }
                }
            }
        }

        // Start foreground service for background operation
        startForegroundService()
    }

    private fun createNotificationChannels() {
        // Create notification channels for messages (with sound)
        NotificationHelper.createNotificationChannels(applicationContext)

        // Channel for service is created in NtfyForegroundService
    }

    private fun requestPermissions() {
        // Request overlay permission
        if (!PermissionHelper.hasOverlayPermission(this)) {
            PermissionHelper.requestOverlayPermission(this, overlayPermissionLauncher)
        }

        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale and request
                    Toast.makeText(
                        this,
                        "Notification permission needed for alerts",
                        Toast.LENGTH_LONG
                    ).show()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, NtfyForegroundService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Notify service that MainActivity is in foreground
        // Service will stop showing notifications while MainActivity is active
        NtfyForegroundService.setMainActivityState(true)
    }

    override fun onPause() {
        super.onPause()
        // Notify service that MainActivity is backgrounded
        // Service will resume showing notifications
        NtfyForegroundService.setMainActivityState(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        messageCollectionJob?.cancel()
        messageCollectionJob = null
        overlayView.cleanup()

        // Only cleanup service if app is actually finishing
        if (isFinishing) {
            // Service continues running in background
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun MainScreen(onManageSubscriptions: () -> Unit) {
        val messages by ntfyService.messages.collectAsState()
        val isConnected by ntfyService.connectionState.collectAsState()
        val subscriptions by subscriptionRepository.getAllSubscriptions()
            .collectAsState(initial = emptyList())
        val activeSubscriptionsCount = subscriptions.count { it.isActive }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ntfy TV Notifications",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ConnectionStatusIndicator(isConnected)
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = if (isConnected)
                                "Status: Connected to ntfy service"
                            else
                                "Status: Disconnected from ntfy service"
                        }
                    )
                }
            }

            // Subscription info
            Text(
                text = when {
                    subscriptions.isEmpty() -> "No subscriptions. Click 'Manage Subscriptions' to add topics."
                    activeSubscriptionsCount == 0 -> "${subscriptions.size} subscriptions (none active)"
                    else -> "$activeSubscriptionsCount active subscription${if (activeSubscriptionsCount != 1) "s" else ""}: " +
                            subscriptions.filter { it.isActive }.joinToString(", ") { it.topic }
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Button(
                    onClick = onManageSubscriptions,
                    modifier = Modifier.semantics {
                        contentDescription = "Manage subscription topics"
                    }
                ) {
                    Text(
                        text = "Manage Subscriptions",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 18.sp
                        )
                    )
                }

                Button(
                    onClick = { ntfyService.connectToActiveSubscriptions() },
                    enabled = !isConnected && activeSubscriptionsCount > 0,
                    modifier = Modifier.semantics {
                        contentDescription = "Reconnect to notification service"
                    }
                ) {
                    Text(
                        text = "Reconnect",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 18.sp
                        )
                    )
                }

                Button(
                    onClick = { ntfyService.disconnect() },
                    enabled = isConnected,
                    modifier = Modifier.semantics {
                        contentDescription = "Disconnect from notification service"
                    }
                ) {
                    Text(
                        text = "Disconnect",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 18.sp
                        )
                    )
                }
            }

            // Messages list
            Text(
                text = "Received Messages (${messages.size})",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet. Waiting for notifications...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages.reversed()) { message ->
                        MessageCard(message)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun MessageCard(message: NtfyMessage) {
        var isFocused by remember { mutableStateOf(false) }

        val contentDesc = buildString {
            append("Notification: ")
            append(message.title ?: "No title")
            if (!message.message.isNullOrEmpty()) {
                append(". Message: ${message.message}")
            }
            append(". Received at ${formatTime(message.time)}")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .background(
                    color = if (isFocused)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (isFocused) 3.dp else 1.dp,
                    color = if (isFocused)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(20.dp)
                .semantics {
                    this.contentDescription = contentDesc
                }
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.title ?: "Notification",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = formatTime(message.time),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (!message.message.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                if (!message.tags.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        message.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ConnectionStatusIndicator(isConnected: Boolean) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                    shape = CircleShape
                )
                .semantics {
                    role = Role.Image
                    contentDescription = if (isConnected)
                        "Connected indicator"
                    else
                        "Disconnected indicator"
                }
        )
    }

    private fun formatTime(timestamp: Long): String {
        // Create new instance each time (thread-safe approach for infrequent calls)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }
}

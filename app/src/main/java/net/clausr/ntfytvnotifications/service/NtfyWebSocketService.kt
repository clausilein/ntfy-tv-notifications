package net.clausr.ntfytvnotifications.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.clausr.ntfytvnotifications.data.repository.MessageRepository
import net.clausr.ntfytvnotifications.data.repository.SubscriptionRepository
import net.clausr.ntfytvnotifications.util.TopicValidator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

data class NtfyMessage(
    val id: String,
    val time: Long,
    val event: String,
    val topic: String,
    val message: String?,
    val title: String?,
    val priority: Int?,
    val tags: List<String>?
)

class NtfyWebSocketService private constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val messageRepository: MessageRepository
) {
    private val TAG = "NtfyWebSocketService"

    companion object {
        private const val MAX_MESSAGES = 100
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 60000L

        @Volatile
        private var instance: NtfyWebSocketService? = null

        fun getInstance(
            subscriptionRepository: SubscriptionRepository,
            messageRepository: MessageRepository
        ): NtfyWebSocketService {
            // Double-checked locking for thread-safe singleton
            return instance ?: synchronized(this) {
                // Re-check after acquiring lock
                val currentInstance = instance
                if (currentInstance != null) {
                    currentInstance
                } else {
                    val newInstance = NtfyWebSocketService(subscriptionRepository, messageRepository)
                    instance = newInstance
                    newInstance
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectionMutex = Mutex()
    private var isConnecting = false
    private var currentTopics: List<String> = emptyList()
    private var reconnectAttempts = 0
    private var context: Context? = null

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val _messages = MutableStateFlow<List<NtfyMessage>>(emptyList())
    val messages: StateFlow<List<NtfyMessage>> = _messages.asStateFlow()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _latestMessage = MutableStateFlow<NtfyMessage?>(null)
    val latestMessage: StateFlow<NtfyMessage?> = _latestMessage.asStateFlow()

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected to ntfy.sh with topics: ${currentTopics.joinToString()}")
            _connectionState.value = true
            reconnectAttempts = 0 // Reset on successful connection
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            scope.launch {
                try {
                    parseNtfyMessage(text)?.let { message ->
                        try {
                            // Save message to database
                            messageRepository.saveMessage(message)
                            Log.d(TAG, "Successfully saved message to database: ${message.id}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save message to database, keeping in memory only", e)
                            // Continue to update in-memory state even if database fails
                        }

                        // Keep in-memory list for current session
                        _messages.value = (_messages.value + message).takeLast(MAX_MESSAGES)
                        _latestMessage.value = message
                        Log.d(TAG, "Processed message: ${message.title} - ${message.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing message", e)
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error: ${t.message}", t)
            _connectionState.value = false

            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                val delay = calculateBackoffDelay(reconnectAttempts)
                reconnectAttempts++

                scope.launch {
                    Log.d(TAG, "Attempting reconnection in ${delay}ms (attempt $reconnectAttempts)")
                    kotlinx.coroutines.delay(delay)
                    if (!_connectionState.value) {
                        connectToActiveSubscriptions()
                    }
                }
            } else {
                Log.e(TAG, "Max reconnection attempts reached. Giving up.")
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code - $reason")
            webSocket.close(1000, null)
            _connectionState.value = false
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code - $reason")
            _connectionState.value = false
        }
    }

    fun setContext(context: Context) {
        this.context = context.applicationContext
    }

    fun connectToActiveSubscriptions() {
        scope.launch {
            // Prevent concurrent connection attempts
            if (isConnecting) {
                Log.d(TAG, "Connection already in progress, skipping")
                return@launch
            }

            connectionMutex.withLock {
                isConnecting = true
                try {
                    // Load active subscriptions from database
                    val activeSubscriptions = subscriptionRepository.getActiveSubscriptions().first()
                    val topics = activeSubscriptions.map { it.topic }

                    if (topics.isEmpty()) {
                        Log.w(TAG, "No active subscriptions to connect to")
                        _connectionState.value = false
                        return@launch
                    }

                    // Validate topics
                    topics.forEach { topic ->
                        when (val result = TopicValidator.validate(topic)) {
                            is TopicValidator.ValidationResult.Valid -> { /* OK */ }
                            is TopicValidator.ValidationResult.Invalid -> {
                                Log.e(TAG, "Invalid topic: $topic - ${result.message}")
                                throw IllegalArgumentException(result.message)
                            }
                        }
                    }

                    // Check if we're already connected to these exact topics
                    if (currentTopics == topics && _connectionState.value) {
                        Log.d(TAG, "Already connected to requested topics")
                        return@launch
                    }

                    currentTopics = topics

                    // Check network connectivity if context is available
                    context?.let { ctx ->
                        if (!isNetworkAvailable(ctx)) {
                            Log.w(TAG, "Cannot connect: No network available")
                            _connectionState.value = false
                            return@launch
                        }
                    }

                    disconnect()

                    // Connect to multiple topics using ntfy.sh multi-topic subscription
                    val topicsParam = topics.joinToString(",")
                    val url = "wss://ntfy.sh/$topicsParam/ws"
                    val request = Request.Builder()
                        .url(url)
                        .build()

                    Log.d(TAG, "Connecting to topics: $topicsParam")
                    webSocket = client.newWebSocket(request, webSocketListener)
                } finally {
                    isConnecting = false
                }
            }
        }
    }

    // Legacy method for backward compatibility
    @Deprecated("Use connectToActiveSubscriptions() instead", ReplaceWith("connectToActiveSubscriptions()"))
    fun connect(topic: String) {
        scope.launch {
            // Ensure subscription exists in database
            val existing = subscriptionRepository.getSubscriptionByTopic(topic)
            if (existing == null) {
                subscriptionRepository.addSubscription(topic)
            }
            connectToActiveSubscriptions()
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = false
    }

    fun cleanup() {
        disconnect()
        scope.cancel()
    }

    private fun parseNtfyMessage(text: String): NtfyMessage? {
        return try {
            val json = JSONObject(text)
            val event = json.optString("event", "")

            if (event != "message") {
                Log.d(TAG, "Skipping non-message event: $event")
                return null
            }

            // Validate required fields
            val id = json.optString("id", "")
            if (id.isEmpty()) {
                Log.w(TAG, "Message missing required 'id' field")
                return null
            }

            NtfyMessage(
                id = id,
                time = json.optLong("time", System.currentTimeMillis() / 1000),
                event = event,
                topic = json.optString("topic", ""),
                message = json.optString("message").takeIf { it.isNotEmpty() },
                title = json.optString("title").takeIf { it.isNotEmpty() },
                priority = json.optInt("priority", 3).coerceIn(1, 5),
                tags = json.optJSONArray("tags")?.let { array ->
                    (0 until array.length()).mapNotNull {
                        array.optString(it).takeIf { str -> str.isNotEmpty() }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}", e)
            null
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val delay = BASE_DELAY_MS * (2.0.pow(attempt).toLong())
        return min(delay, MAX_DELAY_MS)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.isConnected ?: false
        }
    }
}

package net.clausr.ntfytvnotifications.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import net.clausr.ntfytvnotifications.data.db.AppDatabase
import net.clausr.ntfytvnotifications.data.repository.MessageRepository
import net.clausr.ntfytvnotifications.data.repository.SubscriptionRepository

class NtfyWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "NtfyWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "NtfyWorker started")

        return try {
            // Initialize database and repositories
            val database = AppDatabase.getInstance(applicationContext)
            val messageRepository = MessageRepository(database.messageDao())
            val subscriptionRepository = SubscriptionRepository(database.subscriptionDao(), messageRepository)

            val service = NtfyWebSocketService.getInstance(subscriptionRepository, messageRepository)
            service.setContext(applicationContext)

            // Connect if not already connected
            if (!service.connectionState.value) {
                service.connectToActiveSubscriptions()
            }

            // Keep the worker alive for the WebSocket connection
            // This is a long-running task that monitors the connection
            delay(Long.MAX_VALUE)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "ntfy_websocket_worker"
    }
}

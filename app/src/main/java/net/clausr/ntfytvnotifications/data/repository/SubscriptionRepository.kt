package net.clausr.ntfytvnotifications.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import net.clausr.ntfytvnotifications.data.db.dao.SubscriptionDao
import net.clausr.ntfytvnotifications.data.db.entities.Subscription

sealed class AddSubscriptionResult {
    data class Success(val id: Long) : AddSubscriptionResult()
    object AlreadyExists : AddSubscriptionResult()
    data class Error(val message: String) : AddSubscriptionResult()
}

class SubscriptionRepository(
    private val subscriptionDao: SubscriptionDao,
    private val messageRepository: MessageRepository? = null
) {
    private val TAG = "SubscriptionRepository"

    fun getAllSubscriptions(): Flow<List<Subscription>> {
        return subscriptionDao.getAllSubscriptions()
    }

    fun getActiveSubscriptions(): Flow<List<Subscription>> {
        return subscriptionDao.getActiveSubscriptions()
    }

    suspend fun getSubscriptionById(id: Long): Subscription? {
        return subscriptionDao.getSubscriptionById(id)
    }

    suspend fun getSubscriptionByTopic(topic: String): Subscription? {
        return subscriptionDao.getSubscriptionByTopic(topic)
    }

    suspend fun addSubscription(topic: String): AddSubscriptionResult {
        return try {
            // Check if subscription already exists
            val existing = subscriptionDao.getSubscriptionByTopic(topic)
            if (existing != null) {
                Log.w(TAG, "Subscription for topic '$topic' already exists")
                return AddSubscriptionResult.AlreadyExists
            }

            val subscription = Subscription(topic = topic, isActive = true)
            val id = subscriptionDao.insertSubscription(subscription)
            Log.d(TAG, "Successfully added subscription for topic '$topic' with id $id")
            AddSubscriptionResult.Success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding subscription for topic '$topic'", e)
            AddSubscriptionResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun updateSubscription(subscription: Subscription) {
        subscriptionDao.updateSubscription(subscription)
    }

    suspend fun toggleSubscriptionStatus(id: Long, isActive: Boolean) {
        subscriptionDao.updateSubscriptionStatus(id, isActive)
    }

    suspend fun deleteSubscription(subscription: Subscription) {
        try {
            subscriptionDao.deleteSubscription(subscription)
            Log.d(TAG, "Successfully deleted subscription: ${subscription.topic}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting subscription: ${subscription.topic}", e)
            throw e
        }
    }

    suspend fun deleteSubscriptionById(id: Long) {
        try {
            subscriptionDao.deleteSubscriptionById(id)
            Log.d(TAG, "Successfully deleted subscription with id: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting subscription with id: $id", e)
            throw e
        }
    }

    suspend fun deleteSubscriptionWithMessages(subscription: Subscription) {
        try {
            // Delete messages first to avoid foreign key violations
            messageRepository?.deleteMessagesByTopic(subscription.topic)
            Log.d(TAG, "Deleted messages for topic: ${subscription.topic}")

            // Then delete the subscription
            subscriptionDao.deleteSubscription(subscription)
            Log.d(TAG, "Successfully deleted subscription and messages: ${subscription.topic}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting subscription with messages: ${subscription.topic}", e)
            throw e
        }
    }

    suspend fun getSubscriptionCount(): Int {
        return subscriptionDao.getSubscriptionCount()
    }
}

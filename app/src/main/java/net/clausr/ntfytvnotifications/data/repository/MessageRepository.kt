package net.clausr.ntfytvnotifications.data.repository

import kotlinx.coroutines.flow.Flow
import net.clausr.ntfytvnotifications.data.db.dao.MessageDao
import net.clausr.ntfytvnotifications.data.db.entities.StoredMessage
import net.clausr.ntfytvnotifications.service.NtfyMessage
import org.json.JSONArray

class MessageRepository(private val messageDao: MessageDao) {

    fun getMessagesByTopic(topic: String): Flow<List<StoredMessage>> {
        return messageDao.getMessagesByTopic(topic)
    }

    fun getAllMessages(): Flow<List<StoredMessage>> {
        return messageDao.getAllMessages()
    }

    fun getMessagesByTopics(topics: List<String>): Flow<List<StoredMessage>> {
        return messageDao.getMessagesByTopics(topics)
    }

    suspend fun saveMessage(ntfyMessage: NtfyMessage) {
        val storedMessage = StoredMessage(
            id = ntfyMessage.id,
            topic = ntfyMessage.topic,
            time = ntfyMessage.time,
            event = ntfyMessage.event,
            message = ntfyMessage.message,
            title = ntfyMessage.title,
            priority = ntfyMessage.priority,
            tags = ntfyMessage.tags?.let { JSONArray(it).toString() }
        )
        messageDao.insertMessageWithCleanup(storedMessage)
    }

    suspend fun deleteMessagesByTopic(topic: String) {
        messageDao.deleteMessagesByTopic(topic)
    }

    suspend fun deleteAllMessages() {
        messageDao.deleteAllMessages()
    }

    suspend fun getMessageCount(topic: String): Int {
        return messageDao.getMessageCount(topic)
    }

    // Convert StoredMessage to NtfyMessage for UI display
    fun StoredMessage.toNtfyMessage(): NtfyMessage {
        val tagsList = tags?.let {
            try {
                val jsonArray = JSONArray(it)
                (0 until jsonArray.length()).map { i -> jsonArray.getString(i) }
            } catch (e: Exception) {
                null
            }
        }

        return NtfyMessage(
            id = id,
            time = time,
            event = event,
            topic = topic,
            message = message,
            title = title,
            priority = priority,
            tags = tagsList
        )
    }
}

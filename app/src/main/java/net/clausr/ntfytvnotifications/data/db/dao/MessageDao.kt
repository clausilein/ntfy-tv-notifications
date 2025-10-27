package net.clausr.ntfytvnotifications.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.clausr.ntfytvnotifications.data.db.entities.StoredMessage

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE topic = :topic ORDER BY time DESC LIMIT 10")
    fun getMessagesByTopic(topic: String): Flow<List<StoredMessage>>

    @Query("SELECT * FROM messages ORDER BY time DESC")
    fun getAllMessages(): Flow<List<StoredMessage>>

    @Query("""
        SELECT * FROM messages
        WHERE topic IN (:topics)
        ORDER BY time DESC
    """)
    fun getMessagesByTopics(topics: List<String>): Flow<List<StoredMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: StoredMessage)

    @Transaction
    suspend fun insertMessageWithCleanup(message: StoredMessage) {
        insertMessage(message)
        cleanupOldMessages(message.topic)
    }

    @Query("""
        DELETE FROM messages
        WHERE topic = :topic
        AND id NOT IN (
            SELECT id FROM messages
            WHERE topic = :topic
            ORDER BY time DESC
            LIMIT 10
        )
    """)
    suspend fun cleanupOldMessages(topic: String)

    @Query("DELETE FROM messages WHERE topic = :topic")
    suspend fun deleteMessagesByTopic(topic: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("SELECT COUNT(*) FROM messages WHERE topic = :topic")
    suspend fun getMessageCount(topic: String): Int
}

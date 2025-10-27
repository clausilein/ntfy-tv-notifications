package net.clausr.ntfytvnotifications.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index(value = ["topic", "time"])],
    foreignKeys = [
        ForeignKey(
            entity = Subscription::class,
            parentColumns = ["topic"],
            childColumns = ["topic"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class StoredMessage(
    @PrimaryKey
    val id: String,
    val topic: String,
    val time: Long,
    val event: String,
    val message: String?,
    val title: String?,
    val priority: Int?,
    val tags: String? // JSON array serialized as string
)

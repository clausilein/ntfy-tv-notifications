package net.clausr.ntfytvnotifications.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subscriptions",
    indices = [Index(value = ["topic"], unique = true)]
)
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topic: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

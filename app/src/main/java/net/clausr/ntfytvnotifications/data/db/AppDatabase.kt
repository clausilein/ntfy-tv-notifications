package net.clausr.ntfytvnotifications.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.clausr.ntfytvnotifications.data.db.dao.MessageDao
import net.clausr.ntfytvnotifications.data.db.dao.SubscriptionDao
import net.clausr.ntfytvnotifications.data.db.entities.StoredMessage
import net.clausr.ntfytvnotifications.data.db.entities.Subscription

@Database(
    entities = [Subscription::class, StoredMessage::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to version 2
         * Adds foreign key relationship between messages and subscriptions
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new messages table with foreign key constraint
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS messages_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        topic TEXT NOT NULL,
                        time INTEGER NOT NULL,
                        event TEXT NOT NULL,
                        message TEXT,
                        title TEXT,
                        priority INTEGER,
                        tags TEXT,
                        FOREIGN KEY(topic) REFERENCES subscriptions(topic)
                            ON DELETE CASCADE
                            ON UPDATE CASCADE
                    )
                """.trimIndent())

                // Copy existing data, only keeping messages that have a valid subscription
                database.execSQL("""
                    INSERT INTO messages_new (id, topic, time, event, message, title, priority, tags)
                    SELECT id, topic, time, event, message, title, priority, tags
                    FROM messages
                    WHERE topic IN (SELECT topic FROM subscriptions)
                """.trimIndent())

                // Drop old table
                database.execSQL("DROP TABLE messages")

                // Rename new table to original name
                database.execSQL("ALTER TABLE messages_new RENAME TO messages")

                // Recreate indices
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_topic_time ON messages(topic, time)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ntfy_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

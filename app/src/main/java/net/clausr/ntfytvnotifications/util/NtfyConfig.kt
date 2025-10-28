package net.clausr.ntfytvnotifications.util

import android.content.Context
import android.content.SharedPreferences

class NtfyConfig(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ntfy_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_TOPIC = "topic"
        private const val DEFAULT_TOPIC = "my-topic"
        private const val KEY_DISPLAY_DURATION_SECONDS = "display_duration_seconds"
        private const val DEFAULT_DISPLAY_DURATION_SECONDS = 5
        const val MIN_DISPLAY_DURATION_SECONDS = 2
        const val MAX_DISPLAY_DURATION_SECONDS = 15
    }

    var topic: String
        get() = prefs.getString(KEY_TOPIC, DEFAULT_TOPIC) ?: DEFAULT_TOPIC
        set(value) = prefs.edit().putString(KEY_TOPIC, value).apply()

    var displayDurationSeconds: Int
        get() = prefs.getInt(
            KEY_DISPLAY_DURATION_SECONDS,
            DEFAULT_DISPLAY_DURATION_SECONDS
        ).coerceIn(MIN_DISPLAY_DURATION_SECONDS, MAX_DISPLAY_DURATION_SECONDS)
        set(value) {
            require(value in MIN_DISPLAY_DURATION_SECONDS..MAX_DISPLAY_DURATION_SECONDS) {
                "Display duration must be between $MIN_DISPLAY_DURATION_SECONDS and $MAX_DISPLAY_DURATION_SECONDS seconds"
            }
            prefs.edit().putInt(KEY_DISPLAY_DURATION_SECONDS, value).apply()
        }

    val displayDurationMs: Long
        get() = displayDurationSeconds * 1000L
}

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
    }

    var topic: String
        get() = prefs.getString(KEY_TOPIC, DEFAULT_TOPIC) ?: DEFAULT_TOPIC
        set(value) = prefs.edit().putString(KEY_TOPIC, value).apply()
}

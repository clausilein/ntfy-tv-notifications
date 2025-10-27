package net.clausr.ntfytvnotifications.util

object TopicValidator {
    private const val MAX_TOPIC_LENGTH = 64
    private const val MIN_TOPIC_LENGTH = 1

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    fun validate(topic: String): ValidationResult {
        return when {
            topic.isBlank() ->
                ValidationResult.Invalid("Topic cannot be empty")
            topic.length < MIN_TOPIC_LENGTH ->
                ValidationResult.Invalid("Topic too short")
            topic.length > MAX_TOPIC_LENGTH ->
                ValidationResult.Invalid("Topic too long (max $MAX_TOPIC_LENGTH characters)")
            topic.contains("/") ->
                ValidationResult.Invalid("Topic cannot contain slashes (/)")
            topic.contains(",") ->
                ValidationResult.Invalid("Topic cannot contain commas (,)")
            topic.any { it.isWhitespace() } ->
                ValidationResult.Invalid("Topic cannot contain whitespace")
            !topic.matches(Regex("^[a-zA-Z0-9_-]+$")) ->
                ValidationResult.Invalid("Topic can only contain letters, numbers, underscores, and hyphens")
            else ->
                ValidationResult.Valid
        }
    }

    fun isValid(topic: String): Boolean = validate(topic) is ValidationResult.Valid
}

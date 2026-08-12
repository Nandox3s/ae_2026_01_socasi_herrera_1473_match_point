package com.pucetec.users.logging

const val MDC_SUB = "sub"

const val ANONYMOUS_SUB = "anonimo"

fun logLine(event: String, msg: String, vararg fields: Pair<String, Any?>): String {
    val extra = fields
        .filter { it.second != null }
        .joinToString(" ") { (key, value) -> "$key=${renderValue(value)}" }
    val head = "event=$event | msg=${sanitize(msg)}"
    return if (extra.isEmpty()) head else "$head | $extra"
}

fun sanitize(value: String): String =
    value.replace('|', '/')
        .replace('\n', ' ')
        .replace('\r', ' ')
        .trim()

fun renderValue(value: Any?): String {
    val text = sanitize(value.toString())
    return if (text.contains(' ')) "\"$text\"" else text
}

fun maskEmail(email: String?): String? {
    if (email.isNullOrBlank()) return email
    val at = email.indexOf('@')
    if (at <= 0) return "***"
    return "${email.first()}***${email.substring(at)}"
}

fun maskPhone(phone: String?): String? {
    if (phone.isNullOrBlank()) return phone
    if (phone.length <= 4) return "****"
    return "****${phone.takeLast(4)}"
}

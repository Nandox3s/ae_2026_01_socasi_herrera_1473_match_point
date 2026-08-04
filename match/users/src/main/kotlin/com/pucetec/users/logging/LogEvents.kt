package com.pucetec.users.logging

/**
 * Estandar de logging (Criterio 2).
 *
 * El framework pone `timestamp | LEVEL | servicio | sub= | logger` desde el patron de
 * `logging.pattern.console`. Aqui solo se arma la ultima parte del mensaje:
 *
 *     event=<recurso.accion> | msg=<mensaje corto en ingles> | <clave=valor ...>
 *
 * Reglas que respeta este helper:
 *  - Ni el mensaje ni los valores llevan saltos de linea.
 *  - El caracter `|` esta prohibido dentro de un valor: se reemplaza por `/`.
 *  - Un valor con espacios va entre comillas.
 *  - Los campos nulos no se imprimen (nunca se omite `event` ni `msg`).
 */
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

/** `juan.perez@puce.edu.ec` -> `j***@puce.edu.ec`. Nunca se loguea un correo completo. */
fun maskEmail(email: String?): String? {
    if (email.isNullOrBlank()) return email
    val at = email.indexOf('@')
    if (at <= 0) return "***"
    return "${email.first()}***${email.substring(at)}"
}

/** `0999123456` -> `****3456`. Nunca se loguea un telefono completo. */
fun maskPhone(phone: String?): String? {
    if (phone.isNullOrBlank()) return phone
    if (phone.length <= 4) return "****"
    return "****${phone.takeLast(4)}"
}

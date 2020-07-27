package com.henrycourse

import com.fasterxml.jackson.module.kotlin.readValue

inline fun <reified T> String.readConfig(): T? = try {
    configMap()[this] as T
} catch (e: Exception) { null }

fun Any.writeConfig(key: String) {
    objectMapper.writeValueAsString(configMap() + (key to this)).writeToFile("config")
}

fun configMap() = try {
    objectMapper.readValue<Map<String, Any>>("config".readFile())
} catch (e: Exception) {
    emptyMap<String, Any>()
}

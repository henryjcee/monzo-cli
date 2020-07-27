package com.henrycourse

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


const val monzoUri = "https://api.monzo.com"
const val authUrl = "https://auth.monzo.com"
const val authListenerPort = 8050

val objectMapper = ObjectMapper().apply {
    registerModules(KotlinModule(), JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
}

// Files
fun dataDir(): String = "${System.getProperty("user.home")}/.monzo".also {
    File(it).let { file ->
        if (!file.exists()) file.mkdir()
    }
}

fun String.writeToFile(path: String, append: Boolean = false) {
    return Paths.get(dataDir(), path).let {
        Files.write(
            it,
            this.toByteArray(),
            when {
                !it.toFile().exists() -> StandardOpenOption.CREATE
                append -> StandardOpenOption.APPEND
                else -> StandardOpenOption.WRITE
            }
        )
    }
}

fun String.readFile(): String = String(Files.readAllBytes(Paths.get(dataDir(), this)))

// Strings
private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance()

fun Long.asMoney(): String {
    return if (this >= 0) {
        Font.GREEN { currencyFormatter.format(this / 100.0) }
    } else {
        Font.RED { currencyFormatter.format(Math.abs(this) / 100.0) }
    }
}

private val rfc3339Formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.of("UTC"))

fun Instant.toRfc3339(): String = rfc3339Formatter.format(this)
fun Instant.toShortDate(): String = shortDateFormatter.format(this)

fun String.mask(firstN: Int = 4) = "${"*".repeat(firstN)}${this.substring(Math.max(0, this.length - firstN))}"

enum class Font(private val ansiCode: String) {
    BLACK("\u001B[30m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    PURPLE("\u001B[35m"),
    CYAN("\u001B[36m"),
    WHITE("\u001B[37m"),
    BOLD("\u001B[1;37m");

    operator fun invoke(stringProvider: () -> String) = "${this.ansiCode}${stringProvider()}\u001B[0m" // ANSI reset
}

val asciiRegex = Regex("\u001B\\[\\d?;?\\d+m")

fun <T> Collection<T>.renderTable(headers: List<String>, mapper: (T) -> List<String?>): String {

    val headersAndData = (listOf(headers) + this.map(mapper))
    val maxColumnWidths = headersAndData.fold(List(headers.size) { 0 }) { a, n ->
        a.mapIndexed { index, i -> if (i >= n[index]?.replace(asciiRegex, "")?.length ?: 0) i else n[index]!!.replace(asciiRegex, "").length }
    }
    val tabSize = 4

    val rows = headersAndData.map {
        it.mapIndexed { i, n ->
            (n ?: "") + " ".repeat(tabSize + maxColumnWidths[i] - (n?.replace(asciiRegex, "")?.length ?: 0))
        }.joinToString("")
    }

    return (listOf(Font.BOLD { rows[0] }) + rows.takeLast(this.size)).joinToString("\n")
}

fun <T> T.render(headers: List<String>, mapper: (T) -> List<String?>): String = listOf(this).renderTable(headers, mapper)

// General
fun <T> Array<T>.tail() = this.takeLast(this.size - 1)

suspend fun <T, R> Collection<T>.mapAsync(f: suspend (T) -> R) = coroutineScope {
    map { async { f(it) } }
}

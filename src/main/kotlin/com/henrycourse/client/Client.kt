package com.henrycourse.client

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.henrycourse.Account
import com.henrycourse.Balance
import com.henrycourse.Pot
import com.henrycourse.Transaction
import com.henrycourse.monzoUri
import com.henrycourse.toRfc3339
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.URLProtocol
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit


private val monzoClient =
    httpClient(monzoUri) {
        install(Auth) {
            oauth2 {
                clientId = System.getenv("MONZO_CLIENT_ID")
                    ?: throw IllegalStateException("MONZO_CLIENT_ID not found in env vars")
                clientSecret = System.getenv("MONZO_CLIENT_SECRET")
                    ?: throw IllegalStateException("MONZO_CLIENT_SECRET not found in env vars")
            }
        }
    }

suspend fun accountsCall(): List<Account> =
    monzoClient.get<Map<String, List<Account>>>("/accounts").getValue("accounts")

suspend fun balanceCall(accountId: String): Balance =
    monzoClient.get("/balance") { params("account_id" to accountId) }

suspend fun transactionsCall(
    accountId: String,
    from: Instant? = Instant.now().minus(24, ChronoUnit.HOURS),
    to: Instant = Instant.now()
): List<Transaction> {
    return monzoClient.get<Map<String, List<Transaction>>>("/transactions") {
        params("account_id" to accountId)
        between(from, to)
    }.getValue("transactions")
}

suspend fun listPotsCall(accountId: String): List<Pot> =
    monzoClient.get<Map<String, List<Pot>>>("/pots") {
        params("current_account_id" to accountId)
    }.getValue("pots")

fun httpClient(hostAndProtocol: String, additionalConfig: HttpClientConfig<*>.() -> Unit = {}): HttpClient {
    return HttpClient {

        hostAndProtocol.let {
            val url = URL(it)
            val protocol = URLProtocol.createOrDefault(url.protocol)
            this.defaultRequest {
                url {
                    this.host = url.host
                    this.protocol = protocol
                    if (url.port != -1) this.port = url.port
                }
            }
        }

        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
                propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
            }
        }
        additionalConfig()
    }
}

private fun HttpRequestBuilder.between(from: Instant?, to: Instant?) {
    from?.let { params("since" to it.toRfc3339()) }
    to?.let { params("before" to it.toRfc3339()) }
}

fun HttpRequestBuilder.params(vararg params: Pair<String, String>) = params.forEach { parameter(it.first, it.second) }

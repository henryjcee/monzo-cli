package com.henrycourse.client

import com.fasterxml.jackson.module.kotlin.readValue
import com.henrycourse.OauthToken
import com.henrycourse.authListener
import com.henrycourse.authUrl
import com.henrycourse.loginUri
import com.henrycourse.objectMapper
import com.henrycourse.readFile
import com.henrycourse.writeToFile
import io.ktor.client.HttpClient
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.AuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.parametersOf
import java.time.Instant
import kotlin.random.Random

fun Auth.oauth2(block: OAuth2AuthConfig.() -> Unit) {
    with(OAuth2AuthConfig().apply(block)) {
        providers.add(OAuth2AuthProvider(this))
    }
}

class OAuth2AuthConfig {
    lateinit var domain: String
    lateinit var clientId: String
    lateinit var clientSecret: String
    var audience: String? = null
}

class OAuth2AuthProvider(private val config: OAuth2AuthConfig) : AuthProvider {

    private val client: HttpClient = httpClient(authUrl)

    override fun isApplicable(auth: HttpAuthHeader): Boolean = true
    override val sendWithoutRequest = true

    override suspend fun addRequestHeaders(request: HttpRequestBuilder) {
        request.headers[HttpHeaders.Authorization] = "Bearer ${validToken().accessToken}"
    }

    private suspend fun validToken(): OauthToken = runCatching {
        val token = readToken()
        when {
            Instant.now().isBefore(token.expiry) -> token
            else -> refreshToken(token.refreshToken!!)
        }
    }.recoverCatching {
        val state = Random.nextInt()
        println("Click here to authorise monzo-cli: ${loginUri(state)}")
        exchangeCode(authListener(state))
    }.getOrThrow()
     .also { writeAccessToken(it) }

    private suspend fun refreshToken(refreshToken: String) =
        client.post<OauthToken>("/oauth2/token") {
            params(
                "grant_type" to "refresh_token",
                "client_id" to config.clientId,
                "client_secret" to config.clientSecret,
                "refresh_token" to refreshToken
            )
        }

    private suspend fun exchangeCode(code: String) =
        httpClient("https://api.monzo.com").post<OauthToken>("/oauth2/token") {
            body = FormDataContent(
                parametersOf(
                    "grant_type" to listOf("authorization_code"),
                    "client_id" to listOf(config.clientId),
                    "client_secret" to listOf(config.clientSecret),
                    "redirect_uri" to listOf("http://localhost:8050"),
                    "code" to listOf(code)
                )
            )
        }

    private fun readToken() = objectMapper.readValue<OauthToken>("token".readFile())

    private fun writeAccessToken(accessToken: OauthToken) =
        objectMapper.writeValueAsString(accessToken).writeToFile("token")
}

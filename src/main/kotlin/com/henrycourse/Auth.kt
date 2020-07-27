package com.henrycourse

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

val clientId = System.getenv("MONZO_CLIENT_ID")
    ?: throw IllegalStateException("MONZO_CLIENT_ID not found in env vars")

fun loginUri(state: Int) = "$authUrl/?client_id=$clientId&redirect_uri=http://localhost:$authListenerPort&response_type=code&state=$state"

fun authListener(state: Int): String {

    ServerSocket(authListenerPort).use { serverSocket ->
        serverSocket.accept().use { socket ->
            BufferedReader(InputStreamReader(socket.getInputStream())).use { input ->
                socket.getOutputStream().use { output ->

                    val urlParams = input.getLines()[0].parseUrlParams()

                    if (urlParams["state"]?.toInt() != state) throw IllegalStateException("Authentication state check failed")
                    val code = urlParams["code"] ?: throw IllegalStateException("Unable to obtain authentication code")

                    "monzo-cli has authenticated, return to your terminal 🚀".let {
                        output.write(
                            "HTTP/1.1 200 OK\r\nContent-Length: ${it.toByteArray().size}\r\nContent-Type: text/plain; charset=utf-8\n\r\n$it".toByteArray()
                        )
                    }

                    output.flush()

                    return code
                }
            }
        }
    }
}

fun BufferedReader.getLines(): List<String> {
    return this.readLine().let {
        when {
            it.isNullOrBlank() -> emptyList()
            else -> listOf(it) + this.getLines()
        }
    }
}

fun String.parseUrlParams(): Map<String, String> {
    return this
        .substringAfter("?")
        .substringBefore(" ")
        .split("&")
        .mapNotNull { it.split("=").let { if (it.size == 2) it else null } }
        .associate { it[0] to it[1] }
}

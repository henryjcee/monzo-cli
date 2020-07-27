package com.henrycourse

import java.time.Instant

class OauthToken(
    expiresIn: Long,
    val accessToken: String,
    val clientId: String,
    val refreshToken: String?,
    val tokenType: String,
    val scope: String,
    val userId: String,
    val expiry: Instant = Instant.now().plusSeconds(expiresIn)
)

data class Account(
    val id: String,
    val created: Instant,
    val type: String,
    val accountNumber: String,
    val sortCode: String
)

data class Balance(
    val balance: Long,
    val currency: String,
    val spend_today: Long
) {
    override fun toString(): String = balance.asMoney()
}

data class Transaction(
    val created: Instant,
    val amount: Long,
    val description: String?,
    val notes: String?,
    val id: String,
    val category: String,
    val includeInSpending: Boolean
) {
    override fun toString() = "${created.toShortDate()}\t${amount.asMoney()}\t\t$description\t$id"
}

data class Pot(
    val id: String,
    val name: String,
    val balance: Long,
    val deleted: Boolean
)

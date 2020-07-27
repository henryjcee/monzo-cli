package com.henrycourse

import com.henrycourse.client.accountsCall
import com.henrycourse.client.balanceCall
import com.henrycourse.client.listPotsCall
import com.henrycourse.client.transactionsCall
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit


fun main(args: Array<String>) = runBlocking {

    if (args.isEmpty()) {
        balances()
    } else {
        when (args[0]) {
            "balance" -> balances()
            "tx" -> transactions(args.tail())
            "stats" -> stats()
            "pot" -> pots()
            else -> throw IllegalStateException("Unable to parse args, please see https://github.com/henryjcee/monzo-cli for docs")
        }
    }
}

suspend fun balances() {

    coroutineScope {
        val accountInfo = accountsCall()
            .mapAsync {
                val balances = async { balanceCall(it.id) }
                val pots = async { listPotsCall(it.id)  }
                Triple(
                    it,
                    balances.await(),
                    pots.await().filter { !it.deleted }
                )
            }.awaitAll()

        println(Font.BOLD { "Accounts:" })
        accountInfo.map {
            println(
                listOf(it).renderTable(listOf("Acc No.", "Balance", "Type", "Id")) {
                    listOf(it.first.accountNumber.mask(), it.second.balance.asMoney(), it.first.type, it.first.id)
                }
            )
            println(Font.BOLD { "\nPots:" })
            println(
                it.third.renderTable(listOf("Name", "Balance", "Id")) {
                    listOf(it.name, it.balance.asMoney(), it.id)
                }
            )
            println()
        }

        accountInfo.map {
            val total = it.second.balance + it.third.sumBy { it.balance.toInt() }
            println(Font.BOLD { "\nTotal: ${total.asMoney()}" })
        }
    }
}

suspend fun transactions(args: List<String> = emptyList()) {

    val searchString = if (args.size == 1) args[0] else null

    coroutineScope {
        accountsCall().forEach { account ->
            launch {
                transactionsCall(account.id, Instant.now().minus(30, ChronoUnit.DAYS))
                    .filter { searchString?.let { s -> it.description?.contains(s, true) ?: false } ?: true }
                    .let {
                        println("Transactions for ${Font.BOLD { account.accountNumber.mask() }}:")
                        println(
                            it.renderTable(listOf("Date", "Amount", "Description", "Notes", "Category", "Id")) { tx ->
                                listOf(tx.created.toShortDate(), tx.amount.asMoney(), tx.description, tx.notes, tx.category, tx.id)
                            }
                        )
                    }
            }
        }
    }
}

suspend fun stats() {
    coroutineScope {
        accountsCall().forEach { account ->
            launch {
                val transactions = transactionsCall(
                    account.id,
                    LocalDate
                        .now()
                        .with(ChronoField.DAY_OF_MONTH, 1)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                ).filter { it.amount < 0 }
                    .filter { it.includeInSpending }

                val totalSpend = transactions.sumBy { it.amount.toInt() }.toLong()
                val averageDailySpend = (totalSpend / LocalDate.now().dayOfMonth)

                println("Stats for ${Font.BOLD { account.accountNumber.mask() }}:")
                println(
                    (totalSpend to averageDailySpend).render(
                        listOf("Spend This Month", "Avg. Daily Spend")) { listOf(it.first.asMoney(), it.second.asMoney()) }
                )
            }
        }
    }
}

suspend fun pots() {
    coroutineScope {
        accountsCall().forEach {
            launch {
                listPotsCall(it.id).renderTable(listOf("Name", "Balance", "Deleted", "Id")) {
                    listOf(it.name, it.balance.asMoney(), it.deleted.toString(), it.id)
                }
            }
        }
    }
}

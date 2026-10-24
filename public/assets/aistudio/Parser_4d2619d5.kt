package com.pocketanalyst.app

object SmsParser {
    private val balanceWords = listOf("bal", "avl", "avail")
    private val limitWords = listOf("limit", "lmt")
    private val stopRe = Regex(
        "(?:\\s+(?:on|ref|avl|bal|via|using|upi|dated|at)\\b|\\.\\s).*$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val badRe = Regex("^(?:a/c|ac|acct|account|x+\\d*|your|rs|inr|the|card)\\b", RegexOption.IGNORE_CASE)

    private fun firstIndex(text: String, words: List<String>): Int? =
        words.mapNotNull { w ->
            Regex("(?<![a-z0-9])" + Regex.escape(w)).find(text)?.range?.first
        }.minOrNull()

    fun parse(address: String, body: String, ts: Long, k: Kw, ignoreSender: Boolean = false): Txn? {
        val text = body.lowercase()
        if (!ignoreSender && k.senders.isNotEmpty() && k.senders.none { address.lowercase().contains(it) }) return null
        if (firstIndex(text, k.ignore) != null) return null

        val d = firstIndex(text, k.debit)
        val c = firstIndex(text, k.credit)
        val type = when {
            d == null && c == null -> return null
            d == null -> TxType.CREDIT
            c == null -> TxType.DEBIT
            c < d -> TxType.CREDIT
            else -> TxType.DEBIT
        }

        val markers = k.currency.ifEmpty { KeywordGroup.CURRENCY.defaults.map { it.lowercase() } }
        val cur = markers.joinToString("|") { Regex.escape(it) }
        val amtRe = Regex("(?<![a-z])(?:$cur)\\.?\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)")

        var amount: Double? = null
        var balance: Double? = null
        for (m in amtRe.findAll(text)) {
            val v = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
            val before = text.substring(maxOf(0, m.range.first - 20), m.range.first)
            if (limitWords.any { before.contains(it) }) continue
            if (balanceWords.any { before.contains(it) }) {
                if (balance == null) balance = v
            } else if (amount == null) {
                amount = v
            }
        }
        if (amount == null) return null

        return Txn(0, type, amount, merchant(body, type), balance, ts, address, body)
    }

    private fun merchant(body: String, type: TxType): String {
        val prepositions = if (type == TxType.CREDIT) "from|by" else "at|to|towards|info[:\\-]?|vpa"
        val re = Regex("(?<![A-Za-z])(?:$prepositions)\\s+([A-Za-z0-9@._&/* -]{2,40})", RegexOption.IGNORE_CASE)
        for (m in re.findAll(body)) {
            val cand = m.groupValues[1].replace(stopRe, "").trim().trimEnd('.', '-', ' ')
            if (cand.length >= 2 && !badRe.containsMatchIn(cand)) return cand
        }
        return if (type == TxType.CREDIT) "Deposit" else "Unknown"
    }
}

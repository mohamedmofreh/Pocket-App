package com.example.parser

import com.example.data.model.BankSenderEntity
import com.example.data.model.KeywordEntity
import com.example.data.model.TransactionType
import java.util.regex.Pattern

data class ParsedSmsResult(
    val isValid: Boolean,
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.EXPENSE,
    val merchant: String = "", // Represents Sender / Receiver
    val categoryName: String = "General",
    val matchedKeyword: String = "",
    val notes: String = "",
    val rawText: String = "",
    val sender: String = "",
    val isSenderAllowed: Boolean = true
)

object SmsParser {

    private val AMOUNT_PATTERNS = listOf(
        // Symbols followed by number: $50.00, USD 45.20, Rs 500, etc.
        Pattern.compile("""(?i)(?:[$€£₹]|USD|EUR|GBP|INR|Rs\.?|CAD|AUD|EGP|AED|SGD)\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)"""),
        // Number followed by currency code: 50.00 USD
        Pattern.compile("""(?i)([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*(?:USD|EUR|GBP|INR|CAD|AUD|EGP|AED|SGD)"""),
        // Action words followed by number: debited by 45.00, spent 120, etc.
        Pattern.compile("""(?i)(?:debited|spent|purchased|credited|withdrawn|paid|amount of|txn of|charged|sum of)\s*(?:for|by|of)?\s*(?:[$€£₹]|USD|Rs\.?)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""")
    )

    private val stopRe = Regex(
        "(?:\\s+(?:on|ref|avl|bal|via|using|upi|dated|at)\\b|\\.\\s).*$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val badRe = Regex("^(?:a/c|ac|acct|account|x+\\d*|your|rs|inr|the|card)\\b", RegexOption.IGNORE_CASE)

    fun merchant(body: String, type: TransactionType): String {
        val prepositions = if (type == TransactionType.INCOME) "from|by" else "at|to|towards|info[:\\-]??|vpa"
        val re = Regex("(?<![A-Za-z])(?:$prepositions)\\s+([A-Za-z0-9@._&'/*# -]{2,40})", RegexOption.IGNORE_CASE)
        for (m in re.findAll(body)) {
            val cand = m.groupValues[1].replace(stopRe, "").trim().trimEnd('.', '-', ' ')
            if (cand.length >= 2 && !badRe.containsMatchIn(cand)) return cand
        }
        return if (type == TransactionType.INCOME) "Deposit" else "Unknown"
    }

    fun parse(
        body: String,
        sender: String = "",
        keywords: List<KeywordEntity>,
        allowedSenders: List<BankSenderEntity> = emptyList()
    ): ParsedSmsResult {
        if (body.isBlank()) {
            return ParsedSmsResult(isValid = false, rawText = body, sender = sender)
        }

        // 1. Verify Allowed Bank Sender (only read my bank sms messages and not something else)
        val activeBankSenders = allowedSenders.filter { it.isEnabled }
        val isSenderAllowed = if (activeBankSenders.isNotEmpty()) {
            activeBankSenders.any { allowed ->
                sender.contains(allowed.senderKeyword.trim(), ignoreCase = true)
            }
        } else {
            true // If no filter is set yet, allow parsing
        }

        if (!isSenderAllowed) {
            return ParsedSmsResult(
                isValid = false,
                rawText = body,
                sender = sender,
                isSenderAllowed = false,
                notes = "Sender '$sender' does not match your allowed bank sender keywords"
            )
        }

        // 2. Detect matching keyword (debited or credited)
        val lowerBody = body.lowercase()
        val matchedKeyword = keywords.filter { it.isEnabled }.firstOrNull { kw ->
            val kwPattern = "\\b${Pattern.quote(kw.keyword.lowercase())}\\b".toRegex()
            kwPattern.containsMatchIn(lowerBody)
        } ?: run {
            when {
                lowerBody.contains("debited") || lowerBody.contains("spent") || lowerBody.contains("charged") ||
                        lowerBody.contains("paid") || lowerBody.contains("withdrawn") || lowerBody.contains("purchase") ->
                    KeywordEntity(keyword = "debited", type = TransactionType.EXPENSE)
                lowerBody.contains("credited") || lowerBody.contains("deposited") || lowerBody.contains("refund") ||
                        lowerBody.contains("cashback") || lowerBody.contains("salary") ->
                    KeywordEntity(keyword = "credited", type = TransactionType.INCOME)
                else -> null
            }
        }

        if (matchedKeyword == null) {
            return ParsedSmsResult(isValid = false, rawText = body, sender = sender, isSenderAllowed = true)
        }

        // 3. Extract Amount
        var extractedAmount: Double? = null
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val rawNumberStr = matcher.group(1)?.replace(",", "")
                val parsed = rawNumberStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    extractedAmount = parsed
                    break
                }
            }
        }

        if (extractedAmount == null) {
            return ParsedSmsResult(isValid = false, rawText = body, sender = sender, isSenderAllowed = true)
        }

        // 4. Extract Sender / Receiver Counterparty via merchant() logic
        val extractedCounterparty = merchant(body, matchedKeyword.type)

        // 5. Categorize (categorize any new read transaction to General until categorized manually)
        val category = "General"

        val note = when {
            matchedKeyword.type == TransactionType.INCOME -> "Credited inward from $extractedCounterparty"
            else -> "Debited outward to $extractedCounterparty"
        }

        return ParsedSmsResult(
            isValid = true,
            amount = extractedAmount,
            type = matchedKeyword.type,
            merchant = extractedCounterparty,
            categoryName = category,
            matchedKeyword = matchedKeyword.keyword,
            notes = note,
            rawText = body,
            sender = sender,
            isSenderAllowed = true
        )
    }
}

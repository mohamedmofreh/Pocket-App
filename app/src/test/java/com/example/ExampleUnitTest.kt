package com.example

import com.example.data.model.BankSenderEntity
import com.example.data.model.KeywordEntity
import com.example.data.model.TransactionType
import com.example.parser.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val keywords = listOf(
        KeywordEntity(keyword = "debited", type = TransactionType.EXPENSE),
        KeywordEntity(keyword = "spent", type = TransactionType.EXPENSE),
        KeywordEntity(keyword = "purchased", type = TransactionType.EXPENSE, defaultCategoryName = "Shopping"),
        KeywordEntity(keyword = "paid", type = TransactionType.EXPENSE),
        KeywordEntity(keyword = "charged", type = TransactionType.EXPENSE),
        KeywordEntity(keyword = "credited", type = TransactionType.INCOME, defaultCategoryName = "Salary & Income")
    )

    private val allowedBankSenders = listOf(
        BankSenderEntity(senderKeyword = "CHASE"),
        BankSenderEntity(senderKeyword = "CITI"),
        BankSenderEntity(senderKeyword = "BOFA")
    )

    @Test
    fun testChaseDebitSmsParsing() {
        val sms = "ALERT: Your card ending in 4920 was debited $64.50 at TRADER JOE'S on Oct 24. Bal: $2,840.10"
        val result = SmsParser.parse(sms, "CHASE-BANK", keywords, allowedBankSenders)

        assertTrue(result.isValid)
        assertEquals(64.50, result.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("General", result.categoryName)
        assertEquals("TRADER JOE'S", result.merchant)
    }

    @Test
    fun testCitiCreditCardSmsParsing() {
        val sms = "Your Citi card was charged USD 14.25 at STARBUCKS #1049. Available credit: $8,420.00"
        val result = SmsParser.parse(sms, "CITI-CARD", keywords, allowedBankSenders)

        assertTrue(result.isValid)
        assertEquals(14.25, result.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("General", result.categoryName)
        assertEquals("STARBUCKS #1049", result.merchant)
    }

    @Test
    fun testDebitPrepositions() {
        // "paid to Amazon"
        val toResult = SmsParser.merchant("Paid $45.00 to Amazon on Tuesday", TransactionType.EXPENSE)
        assertEquals("Amazon", toResult)

        // "spent at Starbucks"
        val atResult = SmsParser.merchant("Spent $5.50 at Starbucks Coffee. Bal: $200", TransactionType.EXPENSE)
        assertEquals("Starbucks Coffee", atResult)

        // "towards Utilities"
        val towardsResult = SmsParser.merchant("debited $120.00 towards Electric Bill ref 9283", TransactionType.EXPENSE)
        assertEquals("Electric Bill", towardsResult)
    }

    @Test
    fun testCreditPrepositionsAndDefaults() {
        // "from John Doe"
        val fromResult = SmsParser.merchant("Account credited with $1,000.00 from John Doe on 20/10", TransactionType.INCOME)
        assertEquals("John Doe", fromResult)

        // "by Target"
        val byResult = SmsParser.merchant("Refund credited with $35.00 by Target Store ref #4920", TransactionType.INCOME)
        assertEquals("Target Store", byResult)

        // Credit fallback defaults to "Deposit"
        val creditFallback = SmsParser.merchant("Account credited with $500.00 via ACH. Bal: $1,200", TransactionType.INCOME)
        assertEquals("Deposit", creditFallback)

        // Debit fallback defaults to "Unknown"
        val debitFallback = SmsParser.merchant("Card was debited $50.00 for a/c 4920", TransactionType.EXPENSE)
        assertEquals("Unknown", debitFallback)
    }

    @Test
    fun testNonBankSenderIgnored() {
        val sms = "Hey, you spent $50.00 for the concert ticket!"
        val result = SmsParser.parse(sms, "FRIEND-ALEX", keywords, allowedBankSenders)

        // Non-bank sender should be rejected
        assertFalse(result.isValid)
        assertFalse(result.isSenderAllowed)
    }

    @Test
    fun testPayrollIncomeParsing() {
        val sms = "Acct *9012 credited with $3,200.00 from APEX CORP PAYROLL on 20/10. Avail bal: $4,500.00"
        val result = SmsParser.parse(sms, "BOFA-ALERTS", keywords, allowedBankSenders)

        assertTrue(result.isValid)
        assertEquals(3200.00, result.amount, 0.001)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals("General", result.categoryName)
        assertEquals("APEX CORP PAYROLL", result.merchant)
    }
}

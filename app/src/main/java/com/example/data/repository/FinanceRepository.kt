package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.BankSenderEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.KeywordEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.parser.SmsParser
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val database: AppDatabase) {

    val allTransactions: Flow<List<TransactionEntity>> = database.transactionDao().getAllTransactions()
    val allCategories: Flow<List<CategoryEntity>> = database.categoryDao().getAllCategories()
    val allKeywords: Flow<List<KeywordEntity>> = database.keywordDao().getAllKeywords()
    val allBankSenders: Flow<List<BankSenderEntity>> = database.bankSenderDao().getAllSenders()

    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return database.transactionDao().getTransactionsInRange(startTime, endTime)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return database.transactionDao().insert(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        database.transactionDao().update(transaction)
    }

    suspend fun deleteTransaction(id: Long) {
        database.transactionDao().deleteById(id)
    }

    suspend fun clearAllTransactions() {
        database.transactionDao().clearAll()
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return database.categoryDao().insert(category)
    }

    suspend fun deleteCategory(id: Long) {
        database.categoryDao().deleteById(id)
    }

    suspend fun insertKeyword(keyword: KeywordEntity): Long {
        return database.keywordDao().insert(keyword)
    }

    suspend fun updateKeyword(keyword: KeywordEntity) {
        database.keywordDao().update(keyword)
    }

    suspend fun deleteKeyword(id: Long) {
        database.keywordDao().deleteById(id)
    }

    suspend fun resetKeywordsToDefault() {
        database.keywordDao().clearAll()
        val defaultKeywords = listOf(
            KeywordEntity(keyword = "debited", type = TransactionType.EXPENSE),
            KeywordEntity(keyword = "spent", type = TransactionType.EXPENSE),
            KeywordEntity(keyword = "purchased", type = TransactionType.EXPENSE, defaultCategoryName = "Shopping"),
            KeywordEntity(keyword = "paid", type = TransactionType.EXPENSE),
            KeywordEntity(keyword = "withdrawn", type = TransactionType.EXPENSE, defaultCategoryName = "General"),
            KeywordEntity(keyword = "deducted", type = TransactionType.EXPENSE),
            KeywordEntity(keyword = "charged", type = TransactionType.EXPENSE),
            KeywordEntity(keyword = "transferred to", type = TransactionType.EXPENSE),
            KeywordEntity(keyword = "pos purchase", type = TransactionType.EXPENSE),
            KeywordEntity(keyword = "credited", type = TransactionType.INCOME, defaultCategoryName = "Salary & Income"),
            KeywordEntity(keyword = "deposited", type = TransactionType.INCOME, defaultCategoryName = "Salary & Income"),
            KeywordEntity(keyword = "refunded", type = TransactionType.INCOME),
            KeywordEntity(keyword = "cashback", type = TransactionType.INCOME, defaultCategoryName = "Salary & Income"),
            KeywordEntity(keyword = "salary", type = TransactionType.INCOME, defaultCategoryName = "Salary & Income")
        )
        database.keywordDao().insertAll(defaultKeywords)
    }

    suspend fun insertBankSender(sender: BankSenderEntity): Long {
        return database.bankSenderDao().insert(sender)
    }

    suspend fun updateBankSender(sender: BankSenderEntity) {
        database.bankSenderDao().update(sender)
    }

    suspend fun deleteBankSender(id: Long) {
        database.bankSenderDao().deleteById(id)
    }

    suspend fun processIncomingSms(body: String, sender: String, timestamp: Long): TransactionEntity? {
        val keywords = database.keywordDao().getActiveKeywords()
        val allowedSenders = database.bankSenderDao().getActiveSenders()
        val parsed = SmsParser.parse(body, sender, keywords, allowedSenders)
        if (!parsed.isValid) return null

        val entity = TransactionEntity(
            amount = parsed.amount,
            type = parsed.type,
            categoryName = parsed.categoryName,
            timestamp = timestamp,
            merchant = parsed.merchant,
            notes = parsed.notes,
            source = TransactionSource.SMS,
            rawSmsText = body,
            smsSender = sender
        )
        val id = database.transactionDao().insert(entity)
        return entity.copy(id = id)
    }

    suspend fun seedSampleData() {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val sample = listOf(
            TransactionEntity(
                amount = 64.50,
                type = TransactionType.EXPENSE,
                categoryName = "Groceries",
                timestamp = now - (2 * 3600 * 1000),
                merchant = "Trader Joe's",
                notes = "Debited: Fresh produce & organic groceries",
                source = TransactionSource.SMS,
                rawSmsText = "ALERT: Your card ending in 4920 was debited $64.50 at TRADER JOE'S on Oct 24. Bal: $2,840.10",
                smsSender = "CHASE-BANK"
            ),
            TransactionEntity(
                amount = 14.25,
                type = TransactionType.EXPENSE,
                categoryName = "Dining Out",
                timestamp = now - (5 * 3600 * 1000),
                merchant = "Starbucks Coffee",
                notes = "Debited: Cold brew & pastry",
                source = TransactionSource.SMS,
                rawSmsText = "Your Citi card was charged USD 14.25 at STARBUCKS #1049. Available credit: $8,420.00",
                smsSender = "CITI-CARD"
            ),
            TransactionEntity(
                amount = 45.00,
                type = TransactionType.EXPENSE,
                categoryName = "Transport",
                timestamp = now - dayMs + (3600 * 1000),
                merchant = "Shell Gas Station",
                notes = "Debited: Regular unleaded fuel fill-up",
                source = TransactionSource.MANUAL
            ),
            TransactionEntity(
                amount = 89.99,
                type = TransactionType.EXPENSE,
                categoryName = "Shopping",
                timestamp = now - (2 * dayMs),
                merchant = "Target Store",
                notes = "Debited: Home essentials & stationery",
                source = TransactionSource.SMS,
                rawSmsText = "Debit alert: $89.99 spent at TARGET STORE on 22/10. Available bal: $2,950.00",
                smsSender = "WELLS-FARGO"
            ),
            TransactionEntity(
                amount = 120.00,
                type = TransactionType.EXPENSE,
                categoryName = "Bills & Utilities",
                timestamp = now - (3 * dayMs),
                merchant = "Metro Electric Utility",
                notes = "Debited: Monthly electricity statement",
                source = TransactionSource.MANUAL
            ),
            TransactionEntity(
                amount = 3200.00,
                type = TransactionType.INCOME,
                categoryName = "Salary & Income",
                timestamp = now - (4 * dayMs),
                merchant = "Apex Corp Payroll",
                notes = "Credited: Bi-weekly direct deposit salary",
                source = TransactionSource.SMS,
                rawSmsText = "Acct *9012 credited with $3,200.00 on 20/10 via ACH APEX CORP PAYROLL. Avail bal: $4,500.00",
                smsSender = "BANK-OF-AMERICA"
            ),
            TransactionEntity(
                amount = 24.50,
                type = TransactionType.EXPENSE,
                categoryName = "Dining Out",
                timestamp = now - (5 * dayMs),
                merchant = "Chipotle Mexican Grill",
                notes = "Debited: Lunch burrito bowl",
                source = TransactionSource.SMS,
                rawSmsText = "Paid $24.50 to CHIPOTLE GRILL via Apple Pay.",
                smsSender = "CHASE-BANK"
            ),
            TransactionEntity(
                amount = 19.99,
                type = TransactionType.EXPENSE,
                categoryName = "Entertainment",
                timestamp = now - (6 * dayMs),
                merchant = "Netflix Subscription",
                notes = "Debited: Monthly 4K streaming plan",
                source = TransactionSource.MANUAL
            )
        )
        database.transactionDao().insertAll(sample)
    }

    companion object {
        @Volatile
        private var INSTANCE: FinanceRepository? = null

        fun getInstance(context: Context): FinanceRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val repo = FinanceRepository(db)
                INSTANCE = repo
                repo
            }
        }
    }
}

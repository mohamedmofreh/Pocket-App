package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BankSenderEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.KeywordEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, KeywordEntity::class, BankSenderEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun keywordDao(): KeywordDao
    abstract fun bankSenderDao(): BankSenderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocket_analyst_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDefaults(database)
                }
            }
        }

        private suspend fun populateDefaults(database: AppDatabase) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Groceries", iconKey = "shopping_cart", colorHex = "#10B981", isDefault = true),
                CategoryEntity(name = "Dining Out", iconKey = "restaurant", colorHex = "#F97316", isDefault = true),
                CategoryEntity(name = "Transport", iconKey = "directions_car", colorHex = "#3B82F6", isDefault = true),
                CategoryEntity(name = "Shopping", iconKey = "shopping_bag", colorHex = "#8B5CF6", isDefault = true),
                CategoryEntity(name = "Bills & Utilities", iconKey = "receipt_long", colorHex = "#EAB308", isDefault = true),
                CategoryEntity(name = "Entertainment", iconKey = "movie", colorHex = "#EC4899", isDefault = true),
                CategoryEntity(name = "Health", iconKey = "medical_services", colorHex = "#EF4444", isDefault = true),
                CategoryEntity(name = "Salary & Income", iconKey = "payments", colorHex = "#0D9488", isDefault = true),
                CategoryEntity(name = "General", iconKey = "category", colorHex = "#64748B", isDefault = true)
            )
            database.categoryDao().insertAll(defaultCategories)

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

            val defaultBankSenders = listOf(
                BankSenderEntity(senderKeyword = "CHASE"),
                BankSenderEntity(senderKeyword = "CITI"),
                BankSenderEntity(senderKeyword = "BOFA"),
                BankSenderEntity(senderKeyword = "WELLS"),
                BankSenderEntity(senderKeyword = "BANK")
            )
            database.bankSenderDao().insertAll(defaultBankSenders)

            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000
            val initialTransactions = listOf(
                TransactionEntity(
                    amount = 64.50,
                    type = TransactionType.EXPENSE,
                    categoryName = "Groceries",
                    timestamp = now - (2 * 3600 * 1000),
                    merchant = "Trader Joe's",
                    notes = "Fresh produce & organic groceries",
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
                    notes = "Cold brew & pastry",
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
                    notes = "Regular unleaded fuel fill-up",
                    source = TransactionSource.MANUAL
                ),
                TransactionEntity(
                    amount = 89.99,
                    type = TransactionType.EXPENSE,
                    categoryName = "Shopping",
                    timestamp = now - (2 * dayMs),
                    merchant = "Target Store",
                    notes = "Home essentials & stationery",
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
                    notes = "Monthly electricity statement",
                    source = TransactionSource.MANUAL
                ),
                TransactionEntity(
                    amount = 3200.00,
                    type = TransactionType.INCOME,
                    categoryName = "Salary & Income",
                    timestamp = now - (4 * dayMs),
                    merchant = "Apex Corp Payroll",
                    notes = "Bi-weekly direct deposit salary",
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
                    notes = "Lunch burrito bowl",
                    source = TransactionSource.SMS,
                    rawSmsText = "Paid $24.50 to CHIPOTLE GRILL via Apple Pay.",
                    smsSender = "APPLE-CARD"
                ),
                TransactionEntity(
                    amount = 19.99,
                    type = TransactionType.EXPENSE,
                    categoryName = "Entertainment",
                    timestamp = now - (6 * dayMs),
                    merchant = "Netflix Subscription",
                    notes = "Monthly 4K streaming plan",
                    source = TransactionSource.MANUAL
                )
            )
            database.transactionDao().insertAll(initialTransactions)
        }
    }
}

package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BankSenderEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.KeywordEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.parser.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class TimeframeFilter(val label: String) {
    object Last24Hours : TimeframeFilter("24 Hours")
    object Last7Days : TimeframeFilter("7 Days")
    object Last30Days : TimeframeFilter("30 Days")
    data class SingleDate(val dateMillis: Long) : TimeframeFilter("Single Date")
    data class CustomRange(val startMillis: Long, val endMillis: Long) : TimeframeFilter("Custom Range")
    object AllTime : TimeframeFilter("All Time")
}

data class AnalystSummary(
    val totalTransactionsCount: Int = 0, // Count all in specified timeframe
    val inwardCount: Int = 0,            // Total number of inwards (credited)
    val outwardCount: Int = 0,           // Total number of outwards (debited)
    val totalCredited: Double = 0.0,     // Total inward money
    val totalDebited: Double = 0.0,      // Total outward money
    val netBalance: Double = 0.0,
    val smsTransactionCount: Int = 0,
    val manualTransactionCount: Int = 0
)

data class FeedFilter(
    val searchQuery: String = "",
    val typeFilter: TransactionType? = null,
    val categoryFilter: String? = null,
    val sourceFilter: TransactionSource? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinanceRepository.getInstance(application)

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKeywords: StateFlow<List<KeywordEntity>> = repository.allKeywords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBankSenders: StateFlow<List<BankSenderEntity>> = repository.allBankSenders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val prefs = application.getSharedPreferences("pocket_analyst_prefs", Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    private val _selectedTimeframe = MutableStateFlow<TimeframeFilter>(TimeframeFilter.Last7Days)
    val selectedTimeframe: StateFlow<TimeframeFilter> = _selectedTimeframe

    private val _feedFilter = MutableStateFlow(FeedFilter())
    val feedFilter: StateFlow<FeedFilter> = _feedFilter

    // Filtered transactions for the selected timeframe
    val timeframeTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedTimeframe
    ) { transactions, timeframe ->
        filterTransactionsByTimeframe(transactions, timeframe)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered transactions for the Feed
    val feedTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _feedFilter
    ) { transactions, filter ->
        transactions.filter { tx ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                    tx.merchant.contains(filter.searchQuery, ignoreCase = true) ||
                    tx.notes.contains(filter.searchQuery, ignoreCase = true) ||
                    tx.categoryName.contains(filter.searchQuery, ignoreCase = true) ||
                    String.format("%.2f", tx.amount).contains(filter.searchQuery)

            val matchesType = filter.typeFilter == null || tx.type == filter.typeFilter
            val matchesCategory = filter.categoryFilter == null || tx.categoryName.equals(filter.categoryFilter, ignoreCase = true)
            val matchesSource = filter.sourceFilter == null || tx.source == filter.sourceFilter

            matchesQuery && matchesType && matchesCategory && matchesSource
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analyst summary computed dynamically for the selected timeframe
    val analystSummary: StateFlow<AnalystSummary> = timeframeTransactions.combine(
        _selectedTimeframe
    ) { transactions, _ ->
        computeAnalystSummary(transactions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalystSummary())

    fun setTimeframe(timeframe: TimeframeFilter) {
        _selectedTimeframe.value = timeframe
    }

    fun setSearchQuery(query: String) {
        _feedFilter.value = _feedFilter.value.copy(searchQuery = query)
    }

    fun setTypeFilter(type: TransactionType?) {
        _feedFilter.value = _feedFilter.value.copy(typeFilter = type)
    }

    fun setCategoryFilter(category: String?) {
        _feedFilter.value = _feedFilter.value.copy(categoryFilter = category)
    }

    fun setSourceFilter(source: TransactionSource?) {
        _feedFilter.value = _feedFilter.value.copy(sourceFilter = source)
    }

    fun clearFeedFilters() {
        _feedFilter.value = FeedFilter()
    }

    fun addManualTransaction(
        amount: Double,
        type: TransactionType,
        categoryName: String,
        timestamp: Long,
        merchant: String,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = TransactionEntity(
                amount = amount,
                type = type,
                categoryName = categoryName.trim().ifBlank { "General" },
                timestamp = timestamp,
                merchant = merchant.trim().ifBlank { if (type == TransactionType.EXPENSE) "Receiver: Direct Payment" else "Sender: Direct Deposit" },
                notes = notes.trim(),
                source = TransactionSource.MANUAL
            )
            repository.insertTransaction(entity)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(id)
        }
    }

    fun addCategory(name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(
                CategoryEntity(
                    name = name.trim(),
                    iconKey = iconKey,
                    colorHex = colorHex,
                    isDefault = false
                )
            )
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(id)
        }
    }

    fun addKeyword(keyword: String, type: TransactionType, defaultCategoryName: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertKeyword(
                KeywordEntity(
                    keyword = keyword.trim().lowercase(),
                    type = type,
                    defaultCategoryName = defaultCategoryName?.trim()?.ifBlank { null },
                    isEnabled = true
                )
            )
        }
    }

    fun toggleKeyword(keyword: KeywordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateKeyword(keyword.copy(isEnabled = !keyword.isEnabled))
        }
    }

    fun deleteKeyword(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteKeyword(id)
        }
    }

    fun resetKeywords() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetKeywordsToDefault()
        }
    }

    fun addBankSender(senderKeyword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val clean = senderKeyword.trim().uppercase()
            if (clean.isNotBlank()) {
                repository.insertBankSender(
                    BankSenderEntity(senderKeyword = clean, isEnabled = true)
                )
            }
        }
    }

    fun toggleBankSender(sender: BankSenderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBankSender(sender.copy(isEnabled = !sender.isEnabled))
        }
    }

    fun deleteBankSender(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBankSender(id)
        }
    }

    fun simulateSms(body: String, sender: String): com.example.parser.ParsedSmsResult {
        val keywords = allKeywords.value
        val allowedSenders = allBankSenders.value
        return SmsParser.parse(body, sender, keywords, allowedSenders)
    }

    fun scanInboxSms(context: Context, onComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var importedCount = 0
            try {
                val cursor = context.contentResolver.query(
                    Uri.parse("content://sms/inbox"),
                    arrayOf("address", "body", "date"),
                    null,
                    null,
                    "date DESC LIMIT 50"
                )
                val keywords = allKeywords.value
                val allowedSenders = allBankSenders.value
                val existing = allTransactions.value

                cursor?.use {
                    val addressIdx = it.getColumnIndex("address")
                    val bodyIdx = it.getColumnIndex("body")
                    val dateIdx = it.getColumnIndex("date")

                    while (it.moveToNext()) {
                        val sender = if (addressIdx != -1) it.getString(addressIdx) ?: "" else ""
                        val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                        val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()

                        val alreadyExists = existing.any { tx ->
                            tx.source == TransactionSource.SMS && tx.rawSmsText == body
                        }

                        if (!alreadyExists) {
                            val parsed = SmsParser.parse(body, sender, keywords, allowedSenders)
                            if (parsed.isValid) {
                                repository.insertTransaction(
                                    TransactionEntity(
                                        amount = parsed.amount,
                                        type = parsed.type,
                                        categoryName = parsed.categoryName, // "General" per requirement 2
                                        timestamp = date,
                                        merchant = parsed.merchant,
                                        notes = parsed.notes,
                                        source = TransactionSource.SMS,
                                        rawSmsText = body,
                                        smsSender = sender
                                    )
                                )
                                importedCount++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onComplete(importedCount)
        }
    }

    fun seedDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedSampleData()
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllTransactions()
        }
    }

    private fun filterTransactionsByTimeframe(
        transactions: List<TransactionEntity>,
        timeframe: TimeframeFilter
    ): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        return when (timeframe) {
            is TimeframeFilter.Last24Hours -> {
                val threshold = now - (24L * 60 * 60 * 1000)
                transactions.filter { it.timestamp >= threshold }
            }
            is TimeframeFilter.Last7Days -> {
                val threshold = now - (7L * 24 * 60 * 60 * 1000)
                transactions.filter { it.timestamp >= threshold }
            }
            is TimeframeFilter.Last30Days -> {
                val threshold = now - (30L * 24 * 60 * 60 * 1000)
                transactions.filter { it.timestamp >= threshold }
            }
            is TimeframeFilter.SingleDate -> {
                calendar.timeInMillis = timeframe.dateMillis
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                val endOfDay = startOfDay + (24L * 60 * 60 * 1000) - 1
                transactions.filter { it.timestamp in startOfDay..endOfDay }
            }
            is TimeframeFilter.CustomRange -> {
                transactions.filter { it.timestamp in timeframe.startMillis..timeframe.endMillis }
            }
            is TimeframeFilter.AllTime -> transactions
        }
    }

    private fun computeAnalystSummary(
        transactions: List<TransactionEntity>
    ): AnalystSummary {
        var totalDebited = 0.0
        var totalCredited = 0.0
        var debitedCount = 0
        var creditedCount = 0
        var smsCount = 0
        var manualCount = 0

        for (tx in transactions) {
            if (tx.source == TransactionSource.SMS) smsCount++ else manualCount++

            if (tx.type == TransactionType.EXPENSE) {
                totalDebited += tx.amount
                debitedCount++
            } else {
                totalCredited += tx.amount
                creditedCount++
            }
        }

        val totalCount = transactions.size
        val netBalance = totalCredited - totalDebited

        return AnalystSummary(
            totalTransactionsCount = totalCount,
            inwardCount = creditedCount,
            outwardCount = debitedCount,
            totalCredited = totalCredited,
            totalDebited = totalDebited,
            netBalance = netBalance,
            smsTransactionCount = smsCount,
            manualTransactionCount = manualCount
        )
    }
}

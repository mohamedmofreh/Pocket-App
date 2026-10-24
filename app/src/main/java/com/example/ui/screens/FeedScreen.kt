package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.TxSortOption
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FeedScreen(
    viewModel: FinanceViewModel,
    onOpenAddTransaction: () -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val txns by viewModel.txns.collectAsStateWithLifecycle()
    val typeFilter by viewModel.txnTypeFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.txnSortOption.collectAsStateWithLifecycle()
    val filter by viewModel.feedFilter.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // Pipeline: 2. Type Filtering & search/category/source filtering
    val filtered = remember(txns, typeFilter, filter) {
        txns.filter { tx ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                    tx.merchant.contains(filter.searchQuery, ignoreCase = true) ||
                    tx.notes.contains(filter.searchQuery, ignoreCase = true) ||
                    tx.categoryName.contains(filter.searchQuery, ignoreCase = true) ||
                    String.format("%.2f", tx.amount).contains(filter.searchQuery)

            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesCategory = filter.categoryFilter == null || tx.categoryName.equals(filter.categoryFilter, ignoreCase = true)
            val matchesSource = filter.sourceFilter == null || tx.source == filter.sourceFilter

            matchesQuery && matchesType && matchesCategory && matchesSource
        }
    }

    // Pipeline: 3. Sorting & 4. Efficient Compose Caching (remember)
    val sortedTransactions = remember(filtered, sortOption) {
        when (sortOption) {
            TxSortOption.DATE_DESC -> filtered.sortedByDescending { it.timestamp }
            TxSortOption.DATE_ASC -> filtered.sortedBy { it.timestamp }
            TxSortOption.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            TxSortOption.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header & Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Transaction Feed",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${sortedTransactions.size} records found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sort button & menu
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { showSortMenu = true }
                                .testTag("feed_sort_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sortOption.label.split(":")[0], // Short label e.g. "Date" or "Amount"
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        androidx.compose.material3.DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            TxSortOption.entries.forEach { option ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        viewModel.setTxnSortOption(option)
                                        showSortMenu = false
                                    },
                                    modifier = Modifier.testTag("sort_option_${option.name}")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = filter.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search merchant, notes, amount...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (filter.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feed_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterTagChip(
                            label = "All",
                            isSelected = typeFilter == null && filter.sourceFilter == null && filter.categoryFilter == null,
                            onClick = {
                                viewModel.setTxnTypeFilter(null)
                                viewModel.clearFeedFilters()
                            }
                        )
                    }
                    item {
                        FilterTagChip(
                            label = "Debited",
                            isSelected = typeFilter == TransactionType.EXPENSE,
                            onClick = {
                                viewModel.setTxnTypeFilter(
                                    if (typeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                                )
                            }
                        )
                    }
                    item {
                        FilterTagChip(
                            label = "Credited",
                            isSelected = typeFilter == TransactionType.INCOME,
                            onClick = {
                                viewModel.setTxnTypeFilter(
                                    if (typeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                                )
                            }
                        )
                    }
                    item {
                        FilterTagChip(
                            label = "Bank SMS Only",
                            isSelected = filter.sourceFilter == TransactionSource.SMS,
                            onClick = {
                                viewModel.setSourceFilter(
                                    if (filter.sourceFilter == TransactionSource.SMS) null else TransactionSource.SMS
                                )
                            }
                        )
                    }
                    item {
                        FilterTagChip(
                            label = "Manual Only",
                            isSelected = filter.sourceFilter == TransactionSource.MANUAL,
                            onClick = {
                                viewModel.setSourceFilter(
                                    if (filter.sourceFilter == TransactionSource.MANUAL) null else TransactionSource.MANUAL
                                )
                            }
                        )
                    }
                    // Categories
                    items(categories) { cat ->
                        FilterTagChip(
                            label = cat.name,
                            isSelected = filter.categoryFilter.equals(cat.name, ignoreCase = true),
                            onClick = {
                                viewModel.setCategoryFilter(
                                    if (filter.categoryFilter.equals(cat.name, ignoreCase = true)) null else cat.name
                                )
                            }
                        )
                    }
                }
            }

            // Grouped Timeline List
            if (sortedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching transactions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing filters or search terms, or log a new transaction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = {
                            viewModel.setTxnTypeFilter(null)
                            viewModel.clearFeedFilters()
                        }) {
                            Text("Reset Filters")
                        }
                    }
                }
            } else {
                val grouped = remember(sortedTransactions) { groupTransactionsByDate(sortedTransactions) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    grouped.forEach { (dateHeader, dayTransactions) ->
                        item {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                        }

                        items(dayTransactions, key = { it.id }) { tx ->
                            TransactionFeedItem(
                                transaction = tx,
                                timeFormatted = timeFormat.format(Date(tx.timestamp)),
                                onClick = { onSelectTransaction(tx) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onOpenAddTransaction,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 20.dp)
                .testTag("fab_feed_add_transaction")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }
}

@Composable
fun FilterTagChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clickable { onClick() }
            .testTag("feed_filter_$label")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun TransactionFeedItem(
    transaction: TransactionEntity,
    timeFormatted: String,
    onClick: () -> Unit
) {
    val isDebited = transaction.type == TransactionType.EXPENSE
    val isSms = transaction.source == TransactionSource.SMS

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDebited) Rose500.copy(alpha = 0.14f) else Emerald500.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDebited) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isDebited) Rose500 else Emerald500,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isDebited) "Debited" else "Credited",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDebited) Rose500 else Emerald500
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = transaction.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (transaction.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isDebited) "-" else "+"}$${String.format(Locale.US, "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDebited) Rose500 else Emerald500
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Source pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSms) Cyan500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSms) {
                            Icon(
                                Icons.Default.Sms,
                                contentDescription = null,
                                tint = Cyan500,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = if (isSms) "SMS" else "Manual",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isSms) Cyan500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun groupTransactionsByDate(transactions: List<TransactionEntity>): Map<String, List<TransactionEntity>> {
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    val dateCal = Calendar.getInstance()
    val fullDateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())

    return transactions.groupBy { tx ->
        dateCal.timeInMillis = tx.timestamp
        when {
            dateCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    dateCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) -> "Today"

            dateCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                    dateCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) -> "Yesterday"

            else -> fullDateFormat.format(Date(tx.timestamp))
        }
    }
}

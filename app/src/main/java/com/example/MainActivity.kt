package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TransactionEntity
import com.example.ui.dialogs.AddEditTransactionDialog
import com.example.ui.dialogs.TransactionDetailDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsStudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

enum class NavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    ANALYST("Analyst", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_tab_analyst"),
    FEED("Feed", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "nav_tab_feed"),
    SMS("SMS Studio", Icons.Filled.Sms, Icons.Outlined.Sms, "nav_tab_sms"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_tab_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FinanceViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkMode) {
                PocketAnalystApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PocketAnalystApp(
    viewModel: FinanceViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(NavTab.ANALYST) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedDetailTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    // Handle back button: return to ANALYST if on another tab
    if (currentTab != NavTab.ANALYST) {
        BackHandler {
            currentTab = NavTab.ANALYST
        }
    }

    // Add or Edit Transaction Dialog
    if (showAddDialog) {
        AddEditTransactionDialog(
            existingTransaction = editingTransaction,
            categories = categories,
            onAddCategory = { name, icon, color ->
                viewModel.addCategory(name, icon, color)
            },
            onSave = { amount, type, category, timestamp, merchant, notes ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(
                        editingTransaction!!.copy(
                            amount = amount,
                            type = type,
                            categoryName = category,
                            timestamp = timestamp,
                            merchant = merchant,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.addManualTransaction(
                        amount = amount,
                        type = type,
                        categoryName = category,
                        timestamp = timestamp,
                        merchant = merchant,
                        notes = notes
                    )
                }
                editingTransaction = null
                showAddDialog = false
            },
            onDismiss = {
                editingTransaction = null
                showAddDialog = false
            }
        )
    }

    // Detailed View Dialog
    selectedDetailTransaction?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            onEdit = {
                editingTransaction = tx
                selectedDetailTransaction = null
                showAddDialog = true
            },
            onDelete = {
                viewModel.deleteTransaction(tx.id)
                selectedDetailTransaction = null
            },
            onDismiss = {
                selectedDetailTransaction = null
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.ANALYST -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToFeed = { currentTab = NavTab.FEED },
                    onNavigateToSms = { currentTab = NavTab.SMS },
                    onOpenAddTransaction = {
                        editingTransaction = null
                        showAddDialog = true
                    },
                    onSelectTransaction = { selectedDetailTransaction = it }
                )
                NavTab.FEED -> FeedScreen(
                    viewModel = viewModel,
                    onOpenAddTransaction = {
                        editingTransaction = null
                        showAddDialog = true
                    },
                    onSelectTransaction = { selectedDetailTransaction = it }
                )
                NavTab.SMS -> SmsStudioScreen(
                    viewModel = viewModel,
                    onNavigateToFeed = { currentTab = NavTab.FEED }
                )
                NavTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

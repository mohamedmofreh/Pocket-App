@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.pocketanalyst.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val Ink = Color(0xFF1B2A41)
private val Paper = Color(0xFFF5F6F8)
private val Spend = Color(0xFFC8433B)
private val Deposit = Color(0xFF14877A)
private val Mint = Color(0xFF7FE0CF)
private val Amber = Color(0xFFF2C46D)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isUnlocked by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                authenticateBiometric { isUnlocked = true }
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Ink,
                    background = Paper,
                    surface = Paper,
                    secondaryContainer = Color(0xFFDDE6F0)
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = Paper) {
                    if (!isUnlocked) {
                        Box(
                            Modifier.fillMaxSize().background(Ink).padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Amber, modifier = Modifier.size(64.dp))
                                Text("Pocket Analyst is Locked", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text("Please authenticate with your fingerprint or screen lock to open the app.", color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { authenticateBiometric { isUnlocked = true } },
                                    colors = ButtonDefaults.buttonColors(containerColor = Mint)
                                ) {
                                    Text("Unlock App", color = Ink, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        App(onAuthenticate = { onSuccess ->
                            authenticateBiometric(onSuccess)
                        })
                    }
                }
            }
        }
    }

    private fun authenticateBiometric(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Pocket Analyst")
            .setSubtitle("Authenticate using your fingerprint or screen lock PIN to access your financial analytics")
            .setDeviceCredentialAllowed(true)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}

private fun hasSmsPermission(ctx: Context) =
    listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS).all {
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
    }

private fun money(sym: String, v: Double) = (if (v < 0) "-" else "") + sym + "%,.2f".format(abs(v))

private fun cleanBody(body: String): String {
    var text = body
    val balancePatterns = listOf(
        Regex("(?i)(?:available\\s+balance\\s+(?:is)?|avail\\.?\\s*bal|avl\\s*bal|a/c\\s*bal|balance)\\s*(?:is)?[:\\s\\-]*([a-zA-Z]{3}\\s*)?([\\d,]+\\.?\\d*|\\*+)", RegexOption.IGNORE_CASE)
    )
    for (p in balancePatterns) {
        text = text.replace(p) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val matchedText = matchResult.value
            matchedText.substringBefore(prefix) + prefix + "****"
        }
    }
    return text
}

@Composable
fun App(vm: MainViewModel = viewModel(), onAuthenticate: (() -> Unit) -> Unit) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasSmsPermission(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasSmsPermission(ctx)
    }
    LaunchedEffect(granted) { if (granted) vm.scan(false) }

    if (!granted) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Pocket Analyst", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(12.dp))
            Text("Allow access to your text messages so the app can find bank alerts and total up what you spend and receive. Everything stays on this phone.")
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                launcher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
            }) { Text("Allow SMS access") }
            Spacer(Modifier.height(12.dp))
            Text(
                "If the permission is greyed out: Settings > Apps > Pocket Analyst > ⋮ > Allow restricted settings.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = Paper,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Overview") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text("Transactions") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Keywords") })
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> Dashboard(vm, onAuthenticate)
                1 -> TransactionsScreen(vm)
                else -> KeywordsScreen(vm)
            }
        }
    }
}

@Composable
fun Dashboard(vm: MainViewModel, onAuthenticate: (() -> Unit) -> Unit) {
    val txns by vm.txns.collectAsState()
    val draftStart by vm.draftStartDate.collectAsState()
    val draftEnd by vm.draftEndDate.collectAsState()
    val appliedStart by vm.appliedStartDate.collectAsState()
    val appliedEnd by vm.appliedEndDate.collectAsState()
    val sym by vm.symbol.collectAsState()

    val s = remember(txns, appliedStart, appliedEnd) { Analytics.summarizeRange(txns, appliedStart, appliedEnd) }
    val rangeTxns = remember(txns, appliedStart, appliedEnd) { Analytics.getRangeTransactions(txns, appliedStart, appliedEnd) }

    val dateTimeFmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val ctx = LocalContext.current
    var showPickerInfo by remember { mutableStateOf(false) }
    var showRangeTxnsDialog by remember { mutableStateOf(false) }

    var selectedPreset by remember { mutableStateOf("1d") }
    var balanceUnlocked by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { showPickerInfo = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date & Time", tint = Ink)
                            }
                            Column {
                                Text("Date & Time Filter", fontWeight = FontWeight.SemiBold)
                                Text("Draft: ${if (draftStart <= 0L) "Beginning" else dateTimeFmt.format(Date(draftStart))}\nto\n${if (draftEnd == Long.MAX_VALUE) "Present" else dateTimeFmt.format(Date(draftEnd))}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        Button(onClick = { showPickerInfo = true }) {
                            Text("Configure")
                        }
                    }

                    // Preset buttons (1d, 7d, 30d, All - 90d removed)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (selectedPreset == "1d") {
                            Button(onClick = {}) { Text("1d") }
                        } else {
                            OutlinedButton(onClick = {
                                val now = System.currentTimeMillis()
                                vm.draftEndDate.value = now
                                vm.draftStartDate.value = now - 1L * 86_400_000L
                                selectedPreset = "1d"
                            }) { Text("1d") }
                        }

                        if (selectedPreset == "7d") {
                            Button(onClick = {}) { Text("7d") }
                        } else {
                            OutlinedButton(onClick = {
                                val now = System.currentTimeMillis()
                                vm.draftEndDate.value = now
                                vm.draftStartDate.value = now - 7L * 86_400_000L
                                selectedPreset = "7d"
                            }) { Text("7d") }
                        }

                        if (selectedPreset == "30d") {
                            Button(onClick = {}) { Text("30d") }
                        } else {
                            OutlinedButton(onClick = {
                                val now = System.currentTimeMillis()
                                vm.draftEndDate.value = now
                                vm.draftStartDate.value = now - 30L * 86_400_000L
                                selectedPreset = "30d"
                            }) { Text("30d") }
                        }

                        if (selectedPreset == "All") {
                            Button(onClick = {}) { Text("All") }
                        } else {
                            OutlinedButton(onClick = {
                                vm.draftEndDate.value = Long.MAX_VALUE
                                vm.draftStartDate.value = 0L
                                selectedPreset = "All"
                            }) { Text("All") }
                        }
                    }

                    Button(
                        onClick = { vm.applyDateFilter() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink)
                    ) {
                        Text("Apply Date & Time Filter", color = Color.White)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Financial Summary (Applied Range)", color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Total Deposited", fontSize = 11.sp, color = Mint)
                            Text(money(sym, s.deposited), color = Mint, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Total Debited (Spent)", fontSize = 11.sp, color = Spend)
                            Text(money(sym, s.spent), color = Spend, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Mini("Net Flow", money(sym, s.deposited - s.spent), Color.White, Modifier.weight(1f))
                        Box(Modifier.weight(1f).clickable { showRangeTxnsDialog = true }) {
                            Mini("Transactions", "${s.count} 🔍", Amber, Modifier.fillMaxWidth())
                        }
                        // Secured Balance with Biometric / PIN unlock & toggle to re-hide
                        Column(Modifier.weight(1f).clickable {
                            if (balanceUnlocked) {
                                balanceUnlocked = false
                            } else {
                                onAuthenticate { balanceUnlocked = true }
                            }
                        }) {
                            Text("Balance", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (balanceUnlocked) (s.latestBalance?.let { money(sym, it) } ?: "n/a") else "••••••",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    if (balanceUnlocked) Icons.Default.Close else Icons.Default.Lock,
                                    contentDescription = if (balanceUnlocked) "Hide Balance" else "Unlock Balance",
                                    tint = Amber,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (txns.isEmpty()) {
            item {
                Text(
                    "No transactions yet. Open the Keywords tab, check the words match your bank's messages, then tap Re-scan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (showPickerInfo) {
        AlertDialog(
            onDismissRequest = { showPickerInfo = false },
            title = { Text("Select Date & Time Range") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Specify exact start and end date & time for draft filter:")
                    TextButton(onClick = {
                        selectedPreset = "Custom"
                        val c = Calendar.getInstance().apply { timeInMillis = if (draftStart <= 0L) System.currentTimeMillis() else draftStart }
                        android.app.DatePickerDialog(ctx, { _, yr, mo, da ->
                            val startC = Calendar.getInstance().apply { set(yr, mo, da, 0, 0, 0) }
                            android.app.TimePickerDialog(ctx, { _, hr, min ->
                                startC.set(Calendar.HOUR_OF_DAY, hr)
                                startC.set(Calendar.MINUTE, min)
                                vm.draftStartDate.value = startC.timeInMillis
                            }, startC.get(Calendar.HOUR_OF_DAY), startC.get(Calendar.MINUTE), true).show()
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text("Pick Start Date & Time") }

                    TextButton(onClick = {
                        selectedPreset = "Custom"
                        val c = Calendar.getInstance().apply { timeInMillis = if (draftEnd == Long.MAX_VALUE) System.currentTimeMillis() else draftEnd }
                        android.app.DatePickerDialog(ctx, { _, yr, mo, da ->
                            val endC = Calendar.getInstance().apply { set(yr, mo, da, 23, 59, 59) }
                            android.app.TimePickerDialog(ctx, { _, hr, min ->
                                endC.set(Calendar.HOUR_OF_DAY, hr)
                                endC.set(Calendar.MINUTE, min)
                                vm.draftEndDate.value = endC.timeInMillis
                            }, endC.get(Calendar.HOUR_OF_DAY), endC.get(Calendar.MINUTE), true).show()
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text("Pick End Date & Time") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickerInfo = false }) { Text("Done") }
            }
        )
    }

    if (showRangeTxnsDialog) {
        AlertDialog(
            onDismissRequest = { showRangeTxnsDialog = false },
            title = { Text("Transactions in Range (${rangeTxns.size})") },
            text = {
                if (rangeTxns.isEmpty()) {
                    Text("No transactions found in this specified date & time range.")
                } else {
                    LazyColumn(Modifier.height(350.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rangeTxns) { t ->
                            val debit = t.type == TxType.DEBIT
                            val party = t.merchant.ifEmpty { t.sender }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(party, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink, maxLines = 1)
                                        Text(dateTimeFmt.format(Date(t.ts)), style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        (if (debit) "-" else "+") + money(sym, t.amount),
                                        color = if (debit) Spend else Deposit,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRangeTxnsDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun Mini(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
    }
}

@Composable
fun TransactionsScreen(vm: MainViewModel) {
    val txns by vm.txns.collectAsState()
    val sym by vm.symbol.collectAsState()
    val sortOption by vm.txnSortOption.collectAsState()
    val typeFilter by vm.txnTypeFilter.collectAsState()
    val fmt = remember { SimpleDateFormat("dd MMM yy, HH:mm", Locale.getDefault()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedTxn by remember { mutableStateOf<Txn?>(null) }

    val filtered = remember(txns, typeFilter) {
        when (typeFilter) {
            null -> txns
            TxType.CREDIT -> txns.filter { it.type == TxType.CREDIT }
            TxType.DEBIT -> txns.filter { it.type == TxType.DEBIT }
        }
    }

    val sorted = remember(filtered, sortOption) {
        when (sortOption) {
            TxnSortOption.DATE_DESC -> filtered.sortedByDescending { it.ts }
            TxnSortOption.DATE_ASC -> filtered.sortedBy { it.ts }
            TxnSortOption.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            TxnSortOption.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Transactions (${sorted.size})", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Ink)

        // Cool filtration / sort menu placed right under the title
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Sort", tint = Ink)
                        Text("Sort by: ${sortOption.label}", fontWeight = FontWeight.Medium, color = Ink)
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Ink)
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                TxnSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            vm.txnSortOption.value = option
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        // Type filter: All, Credited, Debited
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = typeFilter == null,
                onClick = { vm.txnTypeFilter.value = null },
                label = { Text("All") }
            )
            FilterChip(
                selected = typeFilter == TxType.CREDIT,
                onClick = { vm.txnTypeFilter.value = TxType.CREDIT },
                label = { Text("Credited") }
            )
            FilterChip(
                selected = typeFilter == TxType.DEBIT,
                onClick = { vm.txnTypeFilter.value = TxType.DEBIT },
                label = { Text("Debited") }
            )
        }

        if (txns.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing tracked yet. Adjust your keywords and re-scan.")
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.fillMaxSize()) {
                    // Table Header
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFFEAEFF5)).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date", Modifier.weight(1.1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Amount", Modifier.weight(1.0f), fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End)
                        Text("Sender / Receiver", Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End)
                    }
                    HorizontalDivider()
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(sorted, key = { it.id }) { t ->
                            val debit = t.type == TxType.DEBIT
                            val party = t.merchant.ifEmpty { t.sender }
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { selectedTxn = t }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(fmt.format(Date(t.ts)), Modifier.weight(1.1f), fontSize = 12.sp)
                                Text(
                                    (if (debit) "-" else "+") + money(sym, t.amount),
                                    Modifier.weight(1.0f),
                                    color = if (debit) Spend else Deposit,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.End,
                                    maxLines = 1
                                )
                                Text(
                                    party,
                                    Modifier.weight(1.3f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.End,
                                    maxLines = 1
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF0F3F7))
                        }
                    }
                }
            }
        }
    }

    selectedTxn?.let { t ->
        AlertDialog(
            onDismissRequest = { selectedTxn = null },
            title = { Text(if (t.type == TxType.DEBIT) "Spent Details" else "Deposit Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Amount: ${money(sym, t.amount)}", fontWeight = FontWeight.Bold)
                    Text("Date: ${fmt.format(Date(t.ts))}")
                    Text("Sender / Receiver: ${t.merchant.ifEmpty { t.sender }}")
                    Text("Bank Sender: ${t.sender}")
                    Spacer(Modifier.height(4.dp))
                    Text("SMS Body:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(cleanBody(t.body), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTxn = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun KeywordsScreen(vm: MainViewModel) {
    val kws by vm.keywords.collectAsState()
    val sym by vm.symbol.collectAsState()
    val status by vm.status.collectAsState()
    val busy by vm.busy.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Match these to the wording of your bank's messages. Tap a keyword to remove it. Separate several new ones with commas.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item { TesterCard(vm) }
        items(KeywordGroup.entries.toList()) { g -> GroupCard(g, kws[g].orEmpty(), vm) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Currency symbol", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = sym,
                        onValueChange = { vm.setSymbol(it) },
                        singleLine = true,
                        label = { Text("Shown before amounts, e.g. ₹ or $") }
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.scan(true) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Scanning…" else "Re-scan messages")
                }
                if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun GroupCard(g: KeywordGroup, words: List<String>, vm: MainViewModel) {
    var input by remember { mutableStateOf("") }
    val add = {
        vm.addKeyword(g, input)
        input = ""
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(g.title, fontWeight = FontWeight.SemiBold)
            Text(g.help, style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                words.forEach { w ->
                    InputChip(
                        selected = false,
                        onClick = { vm.removeKeyword(g, w) },
                        label = { Text(w) },
                        trailingIcon = { Icon(Icons.Default.Close, "Remove $w", Modifier.size(16.dp)) }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Add keyword") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() })
                )
                Button(onClick = { add() }) { Text("Add") }
            }
            TextButton(onClick = { vm.resetGroup(g) }) { Text("Reset to defaults") }
        }
    }
}

@Composable
private fun TesterCard(vm: MainViewModel) {
    var sms by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Try a message", fontWeight = FontWeight.SemiBold)
            Text("Paste a bank SMS to see how your keywords read it.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = sms,
                onValueChange = { sms = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("Paste SMS text") }
            )
            Button(onClick = { result = vm.test(sms) }, enabled = sms.isNotBlank()) { Text("Test message") }
            if (result.isNotEmpty()) Text(result, fontWeight = FontWeight.Medium)
        }
    }
}

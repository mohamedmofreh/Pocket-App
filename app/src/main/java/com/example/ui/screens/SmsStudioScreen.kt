package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.parser.SmsParser
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmsStudioScreen(
    viewModel: FinanceViewModel,
    onNavigateToFeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keywords by viewModel.allKeywords.collectAsStateWithLifecycle()
    val allowedSenders by viewModel.allBankSenders.collectAsStateWithLifecycle()

    var testSmsBody by remember {
        mutableStateOf("ALERT: Your card ending in 4920 was debited $78.40 at WHOLE FOODS MARKET on Oct 24. Bal: $2,760.10")
    }
    var testSmsSender by remember { mutableStateOf("CHASE-BANK") }
    var isScanningInbox by remember { mutableStateOf(false) }
    var newSenderInput by remember { mutableStateOf("") }
    var simulationTestMessage by remember { mutableStateOf<String?>(null) }

    // Check permissions
    var hasReceiveSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasReadSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasReceiveSmsPermission = permissions[Manifest.permission.RECEIVE_SMS] == true
        hasReadSmsPermission = permissions[Manifest.permission.READ_SMS] == true
        if (hasReceiveSmsPermission) {
            Toast.makeText(context, "SMS monitoring permissions granted!", Toast.LENGTH_SHORT).show()
        }
    }

    // Live parsing preview enforcing active allowed senders
    val parsedPreview = remember(testSmsBody, testSmsSender, keywords, allowedSenders) {
        SmsParser.parse(testSmsBody, testSmsSender, keywords, allowedSenders)
    }

    val samplePresets = listOf(
        "Chase Debited" to ("ALERT: Your card ending in 4920 was debited $78.40 at WHOLE FOODS MARKET on Oct 24. Bal: $2,760.10" to "CHASE-BANK"),
        "Citi Debited" to ("Your Citi card was charged USD 28.50 at CHIPOTLE MEXICAN GRILL. Avail credit: $8,400.00" to "CITI-CARD"),
        "Payroll Credited" to ("Acct *9012 credited with $2,850.00 on 24/10 from APEX CORP PAYROLL. Avail bal: $5,400.00" to "BOFA-ALERT"),
        "Gas Debited" to ("Debit alert: $42.00 spent at SHELL OIL 1042 on 24/10. Available bal: $1,420.00" to "WELLS-FARGO"),
        "Coffee Debited" to ("Paid $14.75 to STARBUCKS COFFEE via debit card." to "CHASE-BANK"),
        "Amazon Debited" to ("Your Visa debit was charged $59.99 towards AMAZON RETAIL. Bal: $2,310.00" to "BANK-ALERT"),
        "Refund Credited" to ("Your account was credited $39.99 by TARGET STORE. Available: $2,800.09" to "CHASE-BANK")
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "Automated SMS Tracking",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Bank SMS sender filter, debited/credited detection, and test simulator",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Monitoring Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Cyan500.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, tint = Cyan500, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Bank SMS Background Monitor",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasReceiveSmsPermission) "Active • Capturing Allowed Bank Alerts" else "Requires SMS Permission",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasReceiveSmsPermission) Emerald500 else Rose500
                                )
                            }
                        }

                        if (hasReceiveSmsPermission) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Emerald500, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "The app reads incoming SMS alerts, verifies if the sender matches your saved bank sender keywords, and automatically logs debited and credited amounts into your private database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!hasReceiveSmsPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_sms_permission_button")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable SMS Background Tracking")
                        }
                    }
                }
            }
        }

        // Allowed Bank SMS Senders Section (Requirement 1)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Emerald500, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Allowed Bank SMS Senders",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${allowedSenders.filter { it.isEnabled }.size} Active",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Emerald500
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "The app ONLY processes messages whose sender ID contains one of these keywords (e.g. your bank's name or shortcode). All non-bank personal SMS messages are strictly ignored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Senders list as chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allowedSenders.forEach { sender ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (sender.isEnabled) Cyan500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (sender.isEnabled) Cyan500.copy(alpha = 0.4f) else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sender.senderKeyword,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (sender.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteBankSender(sender.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add new bank sender input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSenderInput,
                            onValueChange = { newSenderInput = it },
                            placeholder = { Text("e.g. CHASE, CITI, HDFC, BOFA") },
                            label = { Text("Add Bank Sender ID") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_bank_sender_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newSenderInput.isNotBlank()) {
                                    viewModel.addBankSender(newSenderInput)
                                    newSenderInput = ""
                                    Toast.makeText(context, "Bank sender keyword added!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("add_bank_sender_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }

        // Live SMS Simulator Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = SkyAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live SMS Simulator & Tester",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Test how incoming bank text messages are extracted into debited or credited records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Presets
                    Text(
                        text = "Bank Alert Presets:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        samplePresets.forEach { (label, data) ->
                            val isSelected = testSmsBody == data.first
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        testSmsBody = data.first
                                        testSmsSender = data.second
                                    }
                                    .testTag("preset_$label")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SMS Sender Input
                    OutlinedTextField(
                        value = testSmsSender,
                        onValueChange = { testSmsSender = it },
                        label = { Text("SMS Sender / Bank Header") },
                        placeholder = { Text("e.g. CHASE-BANK, CITI-CARD") },
                        singleLine = true,
                        supportingText = {
                            val matches = allowedSenders.filter { it.isEnabled }.any {
                                testSmsSender.contains(it.senderKeyword, ignoreCase = true)
                            }
                            if (matches) {
                                Text("Matches allowed bank sender filter", color = Emerald500)
                            } else {
                                Text("Will be blocked unless you add '$testSmsSender' to allowed senders", color = Rose500)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_sms_sender_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // SMS Body Input
                    OutlinedTextField(
                        value = testSmsBody,
                        onValueChange = { testSmsBody = it },
                        label = { Text("SMS Message Body") },
                        placeholder = { Text("Paste or type bank SMS message...") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_sms_body_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Extraction Preview Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (parsedPreview.isValid) Emerald500.copy(alpha = 0.5f) else Rose500.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Live Extraction Preview",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (parsedPreview.isValid) "Valid Bank Alert" else "Invalid / Blocked",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (parsedPreview.isValid) Emerald500 else Rose500
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (parsedPreview.isValid) {
                                val isDebited = parsedPreview.type == TransactionType.EXPENSE
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isDebited) "Debited (Outward)" else "Credited (Inward)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDebited) Rose500 else Emerald500
                                            )
                                        )
                                        Text(
                                            text = "${if (isDebited) "-" else "+"}$${String.format(Locale.US, "%.2f", parsedPreview.amount)}",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDebited) Rose500 else Emerald500
                                            )
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isDebited) "Receiver (Payee)" else "Sender (Payer)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = parsedPreview.merchant,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Category: ${parsedPreview.categoryName} (Default)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Trigger: \"${parsedPreview.matchedKeyword}\"",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = SkyAccent
                                    )
                                }
                            } else {
                                Text(
                                    text = if (!parsedPreview.isSenderAllowed) {
                                        "Sender '$testSmsSender' is NOT in your allowed bank senders list. Add it above to allow reading messages from this bank."
                                    } else {
                                        "Could not identify an enabled bank keyword (e.g. debited, credited, spent) or currency amount."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Rose500
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val result = viewModel.simulateSms(testSmsBody, testSmsSender)
                                if (result.isValid) {
                                    val action = if (result.type == TransactionType.EXPENSE) "Debited outward" else "Credited inward"
                                    simulationTestMessage = "Simulation Succeeded: $action $${String.format(Locale.US, "%.2f", result.amount)} • Counterparty: \"${result.merchant}\" • Category: \"${result.categoryName}\"\n(Test mode only — no transaction was logged to your database)"
                                    Toast.makeText(context, "Simulation test passed (No transaction logged)", Toast.LENGTH_SHORT).show()
                                } else {
                                    simulationTestMessage = "Simulation Failed: Could not extract valid transaction. Please verify your allowed bank sender keywords and trigger terms."
                                    Toast.makeText(context, "Simulation test failed: verify sender & keywords", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = parsedPreview.isValid,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_and_save_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate & Log")
                        }

                        OutlinedButton(
                            onClick = onNavigateToFeed,
                            modifier = Modifier.testTag("view_feed_from_sms")
                        ) {
                            Text("View Feed")
                        }
                    }

                    simulationTestMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // SMS Inbox Scanner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Phone SMS Inbox",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Scans inbox for messages matching your allowed bank sender keywords to import past transactions without adding duplicates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (!hasReadSmsPermission) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                            } else {
                                isScanningInbox = true
                                viewModel.scanInboxSms(context) { count ->
                                    isScanningInbox = false
                                    Toast.makeText(context, "Imported $count new bank transactions from SMS!", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_inbox_button")
                    ) {
                        if (isScanningInbox) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning Inbox...")
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Recent SMS Inbox")
                        }
                    }
                }
            }
        }
    }
}

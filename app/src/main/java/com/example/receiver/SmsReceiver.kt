package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullBody = StringBuilder()
        var sender = ""
        var timestamp = System.currentTimeMillis()

        for (msg in messages) {
            fullBody.append(msg.messageBody)
            if (sender.isEmpty()) {
                sender = msg.originatingAddress ?: ""
            }
            timestamp = msg.timestampMillis
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = FinanceRepository.getInstance(appContext)
                val loggedTransaction = repository.processIncomingSms(
                    body = fullBody.toString(),
                    sender = sender,
                    timestamp = timestamp
                )
                if (loggedTransaction != null) {
                    showNotification(appContext, loggedTransaction)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, tx: TransactionEntity) {
        try {
            val channelId = "pocket_analyst_sms"
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Bank SMS Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts for automatically logged bank SMS transactions"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val isDebited = tx.type == TransactionType.EXPENSE
            val sign = if (isDebited) "-" else "+"
            val typeLabel = if (isDebited) "Debited" else "Credited"
            val content = "$sign$${String.format("%.2f", tx.amount)} • $typeLabel • ${tx.merchant} (${tx.categoryName})"

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("New Bank Transaction Logged")
                .setContentText(content)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$content\nAuto-captured by Pocket Financial Analyst.")
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            notificationManager.notify(tx.id.toInt().takeIf { it != 0 } ?: (System.currentTimeMillis() % 10000).toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

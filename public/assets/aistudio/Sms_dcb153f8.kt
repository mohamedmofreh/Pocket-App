package com.pocketanalyst.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.update

object SmsImport {
    /** Reads the inbox and stores every message that matches the keywords. Returns how many new ones were added. */
    fun scanInbox(ctx: Context, days: Int = 400): Int {
        val kw = KeywordStore(ctx).snapshot()
        val db = Db.get(ctx)
        val since = System.currentTimeMillis() - days * 86_400_000L
        var added = 0
        val cols = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
        ctx.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            cols,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(since.toString()),
            "${Telephony.Sms.DATE} DESC"
        )?.use { c ->
            while (c.moveToNext()) {
                val t = SmsParser.parse(c.getString(0) ?: "", c.getString(1) ?: "", c.getLong(2), kw) ?: continue
                if (db.insert(t)) added++
            }
        }
        return added
    }

    fun handleIncoming(ctx: Context, address: String, body: String, ts: Long) {
        val t = SmsParser.parse(address, body, ts, KeywordStore(ctx).snapshot()) ?: return
        if (Db.get(ctx).insert(t)) Events.tick.update { it + 1 }
    }
}

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val address = msgs.firstOrNull()?.originatingAddress ?: return
        val body = msgs.joinToString("") { it.messageBody ?: "" }
        val pending = goAsync()
        thread {
            try {
                SmsImport.handleIncoming(context.applicationContext, address, body, System.currentTimeMillis())
            } finally {
                pending.finish()
            }
        }
    }
}

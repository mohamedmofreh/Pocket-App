package com.pocketanalyst.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Db private constructor(ctx: Context) : SQLiteOpenHelper(ctx, "pocket.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE txn (id INTEGER PRIMARY KEY AUTOINCREMENT, dedup TEXT UNIQUE NOT NULL, " +
                "type TEXT NOT NULL, amount REAL NOT NULL, merchant TEXT, balance REAL, " +
                "ts INTEGER NOT NULL, sender TEXT, body TEXT)"
        )
        db.execSQL("CREATE INDEX idx_ts ON txn(ts)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    @Synchronized
    fun insert(t: Txn): Boolean {
        val dedup = "${t.sender}|${t.body.hashCode()}|${t.ts / 86_400_000L}"
        val cv = ContentValues().apply {
            put("dedup", dedup)
            put("type", t.type.name)
            put("amount", t.amount)
            put("merchant", t.merchant)
            val b = t.balance
            if (b != null) put("balance", b) else putNull("balance")
            put("ts", t.ts)
            put("sender", t.sender)
            put("body", t.body)
        }
        return writableDatabase.insertWithOnConflict("txn", null, cv, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    @Synchronized
    fun clear() {
        writableDatabase.delete("txn", null, null)
    }

    @Synchronized
    fun all(): List<Txn> {
        val out = ArrayList<Txn>()
        readableDatabase.rawQuery(
            "SELECT id,type,amount,merchant,balance,ts,sender,body FROM txn ORDER BY ts DESC", null
        ).use { c ->
            while (c.moveToNext()) {
                out += Txn(
                    c.getLong(0),
                    TxType.valueOf(c.getString(1)),
                    c.getDouble(2),
                    c.getString(3) ?: "",
                    if (c.isNull(4)) null else c.getDouble(4),
                    c.getLong(5),
                    c.getString(6) ?: "",
                    c.getString(7) ?: ""
                )
            }
        }
        return out
    }

    companion object {
        @Volatile
        private var inst: Db? = null

        fun get(ctx: Context): Db =
            inst ?: synchronized(this) { inst ?: Db(ctx.applicationContext).also { inst = it } }
    }
}

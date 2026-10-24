package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryName: String,
    val timestamp: Long,
    val merchant: String,
    val notes: String = "",
    val source: TransactionSource,
    val rawSmsText: String? = null,
    val smsSender: String? = null
)

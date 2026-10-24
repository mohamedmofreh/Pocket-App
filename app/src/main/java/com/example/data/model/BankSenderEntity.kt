package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_senders")
data class BankSenderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderKeyword: String, // e.g. "CHASE", "CITI", "BOFA", "WELLS", "HDFC"
    val isEnabled: Boolean = true
)

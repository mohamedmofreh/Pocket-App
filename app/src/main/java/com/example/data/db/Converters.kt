package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: Exception) {
        TransactionType.EXPENSE
    }

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource): String = value.name

    @TypeConverter
    fun toTransactionSource(value: String): TransactionSource = try {
        TransactionSource.valueOf(value)
    } catch (e: Exception) {
        TransactionSource.MANUAL
    }
}

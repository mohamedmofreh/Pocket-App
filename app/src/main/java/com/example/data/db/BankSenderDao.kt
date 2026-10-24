package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankSenderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankSenderDao {
    @Query("SELECT * FROM bank_senders ORDER BY senderKeyword ASC")
    fun getAllSenders(): Flow<List<BankSenderEntity>>

    @Query("SELECT * FROM bank_senders WHERE isEnabled = 1")
    suspend fun getActiveSenders(): List<BankSenderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sender: BankSenderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(senders: List<BankSenderEntity>)

    @Update
    suspend fun update(sender: BankSenderEntity)

    @Query("DELETE FROM bank_senders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bank_senders")
    suspend fun clearAll()
}

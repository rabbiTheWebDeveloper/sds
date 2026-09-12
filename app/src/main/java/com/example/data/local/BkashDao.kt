package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BkashTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BkashDao {

    @Query("SELECT * FROM bkash_transactions ORDER BY smsTimestamp DESC")
    fun getAllTransactions(): Flow<List<BkashTransaction>>

    @Query("SELECT * FROM bkash_transactions WHERE apiSyncStatus = 'PENDING' ORDER BY smsTimestamp DESC")
    suspend fun getPendingSyncTransactions(): List<BkashTransaction>

    @Query("SELECT * FROM bkash_transactions WHERE trxId = :trxId LIMIT 1")
    suspend fun getTransactionByTrxId(trxId: String): BkashTransaction?

    @Query("SELECT * FROM bkash_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): BkashTransaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: BkashTransaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<BkashTransaction>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: BkashTransaction)

    @Query("UPDATE bkash_transactions SET apiSyncStatus = :status, apiSyncedAt = :syncedAt, apiResponseMessage = :responseMsg WHERE id = :id")
    suspend fun updateApiSyncStatus(id: Long, status: String, syncedAt: Long?, responseMsg: String?)

    @Delete
    suspend fun deleteTransaction(transaction: BkashTransaction)

    @Query("DELETE FROM bkash_transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bkash_transactions")
    fun getTransactionCount(): Flow<Int>

    @Query("SELECT SUM(amount) FROM bkash_transactions WHERE type IN ('CASH_IN', 'RECEIVED_MONEY')")
    fun getTotalReceived(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bkash_transactions WHERE type IN ('PAYMENT', 'CASH_OUT', 'SEND_MONEY', 'RECHARGE')")
    fun getTotalSpent(): Flow<Double?>

    @Query("SELECT * FROM bkash_transactions ORDER BY smsTimestamp DESC LIMIT 1")
    fun getLatestTransaction(): Flow<BkashTransaction?>
}

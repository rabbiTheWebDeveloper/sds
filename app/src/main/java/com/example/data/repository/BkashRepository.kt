package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.BkashDao
import com.example.data.model.BkashTransaction
import com.example.data.remote.BkashApiSyncManager
import com.example.data.sms.SmsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BkashRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao: BkashDao = db.bkashDao()
    val apiManager = BkashApiSyncManager(context)

    companion object {
        private const val TAG = "BkashRepository"
    }

    val allTransactions: Flow<List<BkashTransaction>> = dao.getAllTransactions()
    val transactionCount: Flow<Int> = dao.getTransactionCount()
    val totalReceived: Flow<Double?> = dao.getTotalReceived()
    val totalSpent: Flow<Double?> = dao.getTotalSpent()
    val latestTransaction: Flow<BkashTransaction?> = dao.getLatestTransaction()

    /**
     * Scans device SMS inbox, extracts bKash transactions, stores in database,
     * and optionally triggers API sync if auto-sync is enabled.
     */
    suspend fun scanAndImportSmsInbox(): Int = withContext(Dispatchers.IO) {
        val extractedList = SmsReader.readBkashMessages(context)
        var newCount = 0

        for (transaction in extractedList) {
            val existing = dao.getTransactionByTrxId(transaction.trxId)
            if (existing == null) {
                val rowId = dao.insertTransaction(transaction)
                if (rowId > 0) {
                    newCount++
                }
            }
        }

        if (newCount > 0 && apiManager.autoSyncEnabled) {
            syncPendingTransactions()
        }

        return@withContext newCount
    }

    /**
     * Saves an incoming bKash transaction (e.g. from SMS receiver or simulator)
     */
    suspend fun saveTransaction(transaction: BkashTransaction): Long = withContext(Dispatchers.IO) {
        val existing = dao.getTransactionByTrxId(transaction.trxId)
        if (existing != null) {
            return@withContext existing.id
        }
        val id = dao.insertTransaction(transaction)
        if (id > 0 && apiManager.autoSyncEnabled) {
            val inserted = transaction.copy(id = id)
            syncSingleTransaction(inserted)
        }
        return@withContext id
    }

    /**
     * Sends all pending transactions to the remote API.
     */
    suspend fun syncPendingTransactions(): Int = withContext(Dispatchers.IO) {
        val pendingList = dao.getPendingSyncTransactions()
        var successCount = 0

        for (item in pendingList) {
            val result = apiManager.sendTransactionToApi(item)
            val status = if (result.isSuccess) BkashTransaction.SYNC_STATUS_SYNCED else BkashTransaction.SYNC_STATUS_FAILED
            val syncedAt = if (result.isSuccess) System.currentTimeMillis() else item.apiSyncedAt

            dao.updateApiSyncStatus(
                id = item.id,
                status = status,
                syncedAt = syncedAt,
                responseMsg = result.message
            )

            if (result.isSuccess) {
                successCount++
            }
        }

        return@withContext successCount
    }

    /**
     * Sends a specific transaction to the API.
     */
    suspend fun syncSingleTransaction(transaction: BkashTransaction): BkashApiSyncManager.SyncResult =
        withContext(Dispatchers.IO) {
            val result = apiManager.sendTransactionToApi(transaction)
            val status = if (result.isSuccess) BkashTransaction.SYNC_STATUS_SYNCED else BkashTransaction.SYNC_STATUS_FAILED
            val syncedAt = if (result.isSuccess) System.currentTimeMillis() else transaction.apiSyncedAt

            dao.updateApiSyncStatus(
                id = transaction.id,
                status = status,
                syncedAt = syncedAt,
                responseMsg = result.message
            )

            return@withContext result
        }

    suspend fun deleteTransaction(transaction: BkashTransaction) = withContext(Dispatchers.IO) {
        dao.deleteTransaction(transaction)
    }

    suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }
}

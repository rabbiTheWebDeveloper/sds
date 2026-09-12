package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BkashTransaction
import com.example.data.parser.BkashSmsParser
import com.example.data.repository.BkashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class TransactionFilter {
    ALL,
    INCOMING,
    OUTGOING,
    PENDING_API
}

data class BkashUiState(
    val transactions: List<BkashTransaction> = emptyList(),
    val filter: TransactionFilter = TransactionFilter.ALL,
    val searchQuery: String = "",
    val isScanningSms: Boolean = false,
    val isSyncingApi: Boolean = false,
    val totalReceived: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val apiUrl: String = "",
    val apiKey: String = "",
    val autoSyncEnabled: Boolean = true,
    val userFeedbackMessage: String? = null,
    val testPingStatus: String? = null,
    val isTestingPing: Boolean = false
)

class BkashViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BkashRepository(application)

    private val _filter = MutableStateFlow(TransactionFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _isScanning = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)
    private val _feedback = MutableStateFlow<String?>(null)
    private val _testPingStatus = MutableStateFlow<String?>(null)
    private val _isTestingPing = MutableStateFlow(false)

    private val _apiUrl = MutableStateFlow(repository.apiManager.apiUrl)
    private val _apiKey = MutableStateFlow(repository.apiManager.apiKey)
    private val _autoSync = MutableStateFlow(repository.apiManager.autoSyncEnabled)

    val uiState: StateFlow<BkashUiState> = combine(
        repository.allTransactions,
        _filter,
        _searchQuery,
        _isScanning,
        _isSyncing,
        _feedback,
        _testPingStatus,
        _isTestingPing,
        _apiUrl,
        _apiKey,
        _autoSync
    ) { params ->
        val rawList = params[0] as List<BkashTransaction>
        val filter = params[1] as TransactionFilter
        val query = (params[2] as String).trim().lowercase()
        val isScanning = params[3] as Boolean
        val isSyncing = params[4] as Boolean
        val feedback = params[5] as? String
        val pingStatus = params[6] as? String
        val isTestingPing = params[7] as Boolean
        val apiUrl = params[8] as String
        val apiKey = params[9] as String
        val autoSync = params[10] as Boolean

        val filtered = rawList.filter { item ->
            val matchesFilter = when (filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.INCOMING -> item.isIncoming
                TransactionFilter.OUTGOING -> !item.isIncoming
                TransactionFilter.PENDING_API -> item.apiSyncStatus == BkashTransaction.SYNC_STATUS_PENDING
            }

            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                item.trxId.lowercase().contains(query) ||
                        item.counterparty.lowercase().contains(query) ||
                        item.displayType.lowercase().contains(query) ||
                        item.amount.toString().contains(query) ||
                        (item.reference?.lowercase()?.contains(query) == true)
            }

            matchesFilter && matchesQuery
        }

        var totalRecv = 0.0
        var totalSp = 0.0
        var pendingCnt = 0

        for (item in rawList) {
            if (item.isIncoming) {
                totalRecv += item.amount
            } else {
                totalSp += item.amount
            }
            if (item.apiSyncStatus == BkashTransaction.SYNC_STATUS_PENDING) {
                pendingCnt++
            }
        }

        BkashUiState(
            transactions = filtered,
            filter = filter,
            searchQuery = query,
            isScanningSms = isScanning,
            isSyncingApi = isSyncing,
            totalReceived = totalRecv,
            totalSpent = totalSp,
            totalCount = rawList.size,
            pendingCount = pendingCnt,
            apiUrl = apiUrl,
            apiKey = apiKey,
            autoSyncEnabled = autoSync,
            userFeedbackMessage = feedback,
            testPingStatus = pingStatus,
            isTestingPing = isTestingPing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BkashUiState(
            apiUrl = repository.apiManager.apiUrl,
            apiKey = repository.apiManager.apiKey,
            autoSyncEnabled = repository.apiManager.autoSyncEnabled
        )
    )

    fun setFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    fun scanSmsInbox() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val newCount = repository.scanAndImportSmsInbox()
                _feedback.value = if (newCount > 0) {
                    "Scan complete: Found $newCount new bKash message(s)!"
                } else {
                    "Scan complete: No new bKash messages found."
                }
            } catch (e: Exception) {
                _feedback.value = "SMS scan failed: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun syncPendingToApi() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val count = repository.syncPendingTransactions()
                _feedback.value = if (count > 0) {
                    "Successfully sent $count transaction(s) to API!"
                } else {
                    "No pending transactions synced. Check API URL or connection."
                }
            } catch (e: Exception) {
                _feedback.value = "API sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncSingleTransaction(transaction: BkashTransaction) {
        viewModelScope.launch {
            try {
                val result = repository.syncSingleTransaction(transaction)
                _feedback.value = if (result.isSuccess) {
                    "TrxID ${transaction.trxId} sent to API: ${result.message}"
                } else {
                    "Sync failed: ${result.message}"
                }
            } catch (e: Exception) {
                _feedback.value = "Sync error: ${e.message}"
            }
        }
    }

    fun deleteTransaction(transaction: BkashTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _feedback.value = "Transaction ${transaction.trxId} deleted"
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            _feedback.value = "All stored transactions cleared"
        }
    }

    fun saveApiSettings(newUrl: String, newApiKey: String, autoSync: Boolean) {
        repository.apiManager.apiUrl = newUrl
        repository.apiManager.apiKey = newApiKey
        repository.apiManager.autoSyncEnabled = autoSync

        _apiUrl.value = newUrl
        _apiKey.value = newApiKey
        _autoSync.value = autoSync

        _feedback.value = "API settings updated"
    }

    fun testApiPing(targetUrl: String) {
        viewModelScope.launch {
            _isTestingPing.value = true
            _testPingStatus.value = "Testing connection to $targetUrl..."
            try {
                val result = repository.apiManager.testEndpoint(targetUrl)
                _testPingStatus.value = if (result.isSuccess) {
                    "✅ ${result.message}"
                } else {
                    "❌ ${result.message}"
                }
            } catch (e: Exception) {
                _testPingStatus.value = "❌ Test failed: ${e.message}"
            } finally {
                _isTestingPing.value = false
            }
        }
    }

    /**
     * Simulates receiving realistic bKash SMS messages.
     * Essential for emulators, browser previews, or devices without real bKash SMS.
     */
    fun simulateBkashSms(type: String) {
        viewModelScope.launch {
            val randomNum = 10000000 + Random.nextInt(90000000)
            val charPool = ('A'..'Z') + ('0'..'9')
            val randomTrx = (1..10).map { charPool[Random.nextInt(charPool.size)] }.joinToString("")
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH)
            val dateStr = sdf.format(Date())

            val rawSms = when (type) {
                BkashTransaction.TYPE_CASH_IN -> {
                    val amount = (listOf(500, 1000, 1500, 2000, 5000).random()).toDouble()
                    val balance = 3000.0 + amount
                    "You have received Tk $amount from 017$randomNum. Fee Tk 0.00. Balance Tk $balance. TrxID $randomTrx at $dateStr"
                }
                BkashTransaction.TYPE_RECEIVED_MONEY -> {
                    val amount = (listOf(300, 750, 1200, 2500).random()).toDouble()
                    val balance = 4000.0 + amount
                    "You have received Tk $amount from 018$randomNum. Ref Family. Fee Tk 0.00. Balance Tk $balance. TrxID $randomTrx at $dateStr"
                }
                BkashTransaction.TYPE_PAYMENT -> {
                    val amount = (listOf(250, 680, 1450, 2100).random()).toDouble()
                    val balance = 2000.0
                    "Payment Tk $amount to Shwapno Superstore (019$randomNum) successful. Ref Grocery. Fee Tk 0.00. Balance Tk $balance. TrxID $randomTrx at $dateStr"
                }
                BkashTransaction.TYPE_CASH_OUT -> {
                    val amount = (listOf(1000, 2000, 3000).random()).toDouble()
                    val fee = amount * 0.0149
                    val balance = 1500.0
                    "Cash Out Tk $amount to 017$randomNum fee Tk ${String.format(Locale.US, "%.2f", fee)}. Balance Tk $balance. TrxID $randomTrx at $dateStr"
                }
                else -> {
                    val amount = 100.0
                    "Mobile Recharge of Tk $amount to 016$randomNum successful. Balance Tk 850.00. TrxID $randomTrx at $dateStr"
                }
            }

            val parsed = BkashSmsParser.parse("bKash", rawSms, System.currentTimeMillis())
            if (parsed != null) {
                repository.saveTransaction(parsed)
                _feedback.value = "New ${parsed.displayType} bKash SMS received & saved! (TrxID: ${parsed.trxId})"
            }
        }
    }
}

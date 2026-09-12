package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "bkash_transactions",
    indices = [Index(value = ["trxId"], unique = true)]
)
data class BkashTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trxId: String,
    val type: String, // CASH_IN, RECEIVED_MONEY, SEND_MONEY, PAYMENT, CASH_OUT, RECHARGE, UNKNOWN
    val amount: Double,
    val counterparty: String,
    val fee: Double = 0.0,
    val balance: Double = 0.0,
    val reference: String? = null,
    val smsSender: String = "bKash",
    val smsTimestamp: Long = System.currentTimeMillis(),
    val rawBody: String,
    val apiSyncStatus: String = SYNC_STATUS_PENDING, // PENDING, SYNCED, FAILED
    val apiSyncedAt: Long? = null,
    val apiResponseMessage: String? = null
) {
    companion object {
        const val TYPE_CASH_IN = "CASH_IN"
        const val TYPE_RECEIVED_MONEY = "RECEIVED_MONEY"
        const val TYPE_SEND_MONEY = "SEND_MONEY"
        const val TYPE_PAYMENT = "PAYMENT"
        const val TYPE_CASH_OUT = "CASH_OUT"
        const val TYPE_RECHARGE = "RECHARGE"
        const val TYPE_UNKNOWN = "UNKNOWN"

        const val SYNC_STATUS_PENDING = "PENDING"
        const val SYNC_STATUS_SYNCED = "SYNCED"
        const val SYNC_STATUS_FAILED = "FAILED"
    }

    val isIncoming: Boolean
        get() = type == TYPE_CASH_IN || type == TYPE_RECEIVED_MONEY

    val formattedAmount: String
        get() = String.format(Locale.US, "৳ %,.2f", amount)

    val formattedBalance: String
        get() = String.format(Locale.US, "৳ %,.2f", balance)

    val formattedFee: String
        get() = String.format(Locale.US, "৳ %,.2f", fee)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            return sdf.format(Date(smsTimestamp))
        }

    val displayType: String
        get() = when (type) {
            TYPE_CASH_IN -> "Cash In"
            TYPE_RECEIVED_MONEY -> "Received Money"
            TYPE_SEND_MONEY -> "Send Money"
            TYPE_PAYMENT -> "Payment"
            TYPE_CASH_OUT -> "Cash Out"
            TYPE_RECHARGE -> "Mobile Recharge"
            else -> "Transaction"
        }
}

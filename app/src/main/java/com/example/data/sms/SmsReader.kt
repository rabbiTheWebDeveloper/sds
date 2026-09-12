package com.example.data.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.example.data.model.BkashTransaction
import com.example.data.parser.BkashSmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsReader {

    private const val TAG = "SmsReader"

    /**
     * Reads SMS from device inbox and extracts all bKash transactions.
     */
    suspend fun readBkashMessages(context: Context): List<BkashTransaction> = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<BkashTransaction>()
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            if (cursor != null && cursor.moveToFirst()) {
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

                do {
                    val address = if (addressIdx != -1) cursor.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) cursor.getLong(dateIdx) else System.currentTimeMillis()

                    if (BkashSmsParser.isBkashMessage(address, body)) {
                        val parsed = BkashSmsParser.parse(address, body, date)
                        if (parsed != null) {
                            transactions.add(parsed)
                        }
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "READ_SMS permission not granted: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SMS inbox: ${e.message}", e)
        } finally {
            cursor?.close()
        }

        return@withContext transactions
    }
}

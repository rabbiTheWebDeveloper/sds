package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.parser.BkashSmsParser
import com.example.data.repository.BkashRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BkashSmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BkashSmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val repo = BkashRepository(context)

            // Group by sender in case of multi-part messages
            val fullBodyBuilder = StringBuilder()
            var sender = ""
            var timestamp = System.currentTimeMillis()

            for (sms in messages) {
                if (sms != null) {
                    sender = sms.displayOriginatingAddress ?: ""
                    fullBodyBuilder.append(sms.displayMessageBody ?: "")
                    timestamp = sms.timestampMillis
                }
            }

            val body = fullBodyBuilder.toString()
            if (BkashSmsParser.isBkashMessage(sender, body)) {
                Log.d(TAG, "bKash SMS detected from $sender: $body")
                val parsed = BkashSmsParser.parse(sender, body, timestamp)
                if (parsed != null) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repo.saveTransaction(parsed)
                            Log.d(TAG, "Saved incoming bKash transaction ${parsed.trxId} to DB")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving incoming bKash SMS: ${e.message}", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in SMS receiver: ${e.message}", e)
        }
    }
}

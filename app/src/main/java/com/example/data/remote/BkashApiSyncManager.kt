package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.BkashTransaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class BkashApiSyncManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bkash_sync_prefs", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "BkashApiSyncManager"
        const val PREF_API_URL = "api_endpoint_url"
        const val PREF_AUTO_SYNC = "auto_sync_enabled"
        const val PREF_API_KEY = "api_auth_token"
        const val DEFAULT_API_URL = "https://httpbin.org/post"
    }

    var apiUrl: String
        get() = prefs.getString(PREF_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
        set(value) = prefs.edit().putString(PREF_API_URL, value.trim()).apply()

    var autoSyncEnabled: Boolean
        get() = prefs.getBoolean(PREF_AUTO_SYNC, true)
        set(value) = prefs.edit().putBoolean(PREF_AUTO_SYNC, value).apply()

    var apiKey: String
        get() = prefs.getString(PREF_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(PREF_API_KEY, value.trim()).apply()

    data class SyncResult(
        val isSuccess: Boolean,
        val statusCode: Int = 0,
        val message: String
    )

    /**
     * Sends a parsed bKash transaction payload to the configured API endpoint.
     */
    suspend fun sendTransactionToApi(
        transaction: BkashTransaction,
        targetUrl: String = apiUrl
    ): SyncResult = withContext(Dispatchers.IO) {
        if (targetUrl.isBlank()) {
            return@withContext SyncResult(false, 0, "API URL is empty")
        }

        try {
            val payload = mapOf(
                "event" to "BKASH_SMS_TRANSACTION",
                "trxId" to transaction.trxId,
                "type" to transaction.type,
                "displayType" to transaction.displayType,
                "amount" to transaction.amount,
                "counterparty" to transaction.counterparty,
                "fee" to transaction.fee,
                "balance" to transaction.balance,
                "reference" to (transaction.reference ?: ""),
                "smsSender" to transaction.smsSender,
                "smsTimestamp" to transaction.smsTimestamp,
                "formattedDate" to transaction.formattedDate,
                "rawMessage" to transaction.rawBody
            )

            val adapter = moshi.adapter(Map::class.java)
            val jsonString = adapter.toJson(payload)

            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .post(jsonString.toRequestBody(jsonMediaType))
                .addHeader("User-Agent", "bKash-SMS-Sync-Android/1.0")
                .addHeader("Content-Type", "application/json")

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                requestBuilder.addHeader("X-API-KEY", apiKey)
            }

            val request = requestBuilder.build()
            val response = okHttpClient.newCall(request).execute()
            val responseCode = response.code
            val isSuccess = response.isSuccessful

            response.close()

            if (isSuccess) {
                Log.d(TAG, "Successfully synced TrxID ${transaction.trxId} to API (HTTP $responseCode)")
                SyncResult(true, responseCode, "HTTP $responseCode Success")
            } else {
                Log.w(TAG, "Failed syncing TrxID ${transaction.trxId} to API (HTTP $responseCode)")
                SyncResult(false, responseCode, "HTTP $responseCode error")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during API sync: ${e.message}", e)
            SyncResult(false, 0, "Network error: ${e.localizedMessage ?: "Connection failed"}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during API sync: ${e.message}", e)
            SyncResult(false, 0, "Error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Tests the connectivity to the configured API endpoint with a test ping.
     */
    suspend fun testEndpoint(targetUrl: String): SyncResult = withContext(Dispatchers.IO) {
        if (targetUrl.isBlank()) {
            return@withContext SyncResult(false, 0, "API URL is required")
        }

        try {
            val pingPayload = mapOf(
                "event" to "PING_TEST",
                "message" to "bKash SMS Sync connection test",
                "timestamp" to System.currentTimeMillis()
            )
            val jsonString = moshi.adapter(Map::class.java).toJson(pingPayload)

            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .post(jsonString.toRequestBody(jsonMediaType))
                .addHeader("User-Agent", "bKash-SMS-Sync-Android/1.0")

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val code = response.code
            val isSuccess = response.isSuccessful
            response.close()

            if (isSuccess) {
                SyncResult(true, code, "Connected! Server returned HTTP $code")
            } else {
                SyncResult(false, code, "Server responded with HTTP $code")
            }
        } catch (e: Exception) {
            SyncResult(false, 0, "Connection failed: ${e.message}")
        }
    }
}

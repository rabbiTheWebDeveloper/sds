package com.example.data.parser

import com.example.data.model.BkashTransaction
import java.util.regex.Pattern

object BkashSmsParser {

    /**
     * Checks if the SMS message originates from or pertains to bKash.
     */
    fun isBkashMessage(sender: String?, body: String?): Boolean {
        if (body.isNullOrBlank()) return false
        val s = sender?.lowercase() ?: ""
        val b = body.lowercase()

        val isSenderBkash = s.contains("bkash") || s.contains("b-kash") || s.contains("16247")
        val hasBkashContent = b.contains("bkash") ||
                (b.contains("trxid") && (b.contains("balance tk") || b.contains("fee tk") || b.contains("received tk") || b.contains("cash in tk") || b.contains("cash out tk")))

        return isSenderBkash || hasBkashContent
    }

    /**
     * Parses the SMS message into a BkashTransaction if valid, or returns null.
     */
    fun parse(sender: String, body: String, timestamp: Long): BkashTransaction? {
        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) return null

        // Must at least extract a TrxID or have bKash patterns
        val trxId = extractTrxId(cleanBody) ?: return null

        val type = determineType(cleanBody)
        val amount = extractAmount(cleanBody, type)
        val counterparty = extractCounterparty(cleanBody, type)
        val fee = extractFee(cleanBody)
        val balance = extractBalance(cleanBody)
        val reference = extractReference(cleanBody)

        return BkashTransaction(
            trxId = trxId,
            type = type,
            amount = amount,
            counterparty = counterparty,
            fee = fee,
            balance = balance,
            reference = reference,
            smsSender = if (sender.isBlank()) "bKash" else sender,
            smsTimestamp = timestamp,
            rawBody = cleanBody,
            apiSyncStatus = BkashTransaction.SYNC_STATUS_PENDING
        )
    }

    private fun extractTrxId(body: String): String? {
        val patterns = listOf(
            Pattern.compile("""(?i)TrxID\s*[:\s]?\s*([A-Za-z0-9]+)"""),
            Pattern.compile("""(?i)TxnId\s*[:\s]?\s*([A-Za-z0-9]+)"""),
            Pattern.compile("""(?i)Txn\s*ID\s*[:\s]?\s*([A-Za-z0-9]+)"""),
            Pattern.compile("""(?i)Transaction\s*ID\s*[:\s]?\s*([A-Za-z0-9]+)""")
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val found = matcher.group(1)?.trim()
                if (!found.isNullOrEmpty()) return found
            }
        }
        return null
    }

    private fun determineType(body: String): String {
        val b = body.lowercase()
        return when {
            b.contains("you have received") || b.contains("received tk") -> BkashTransaction.TYPE_RECEIVED_MONEY
            b.contains("cash in") -> BkashTransaction.TYPE_CASH_IN
            b.contains("payment") -> BkashTransaction.TYPE_PAYMENT
            b.contains("cash out") -> BkashTransaction.TYPE_CASH_OUT
            b.contains("send money") -> BkashTransaction.TYPE_SEND_MONEY
            b.contains("mobile recharge") || b.contains("recharge") -> BkashTransaction.TYPE_RECHARGE
            else -> BkashTransaction.TYPE_UNKNOWN
        }
    }

    private fun extractAmount(body: String, type: String): Double {
        // Targeted patterns first based on type
        val patterns = when (type) {
            BkashTransaction.TYPE_RECEIVED_MONEY -> listOf(
                Pattern.compile("""(?i)received\s+Tk\.?\s*([0-9,]+\.?[0-9]*)"""),
                Pattern.compile("""(?i)Tk\.?\s*([0-9,]+\.?[0-9]*)\s+from""")
            )
            BkashTransaction.TYPE_CASH_IN -> listOf(
                Pattern.compile("""(?i)Cash\s*In\s+Tk\.?\s*([0-9,]+\.?[0-9]*)"""),
                Pattern.compile("""(?i)Tk\.?\s*([0-9,]+\.?[0-9]*)\s+from""")
            )
            BkashTransaction.TYPE_PAYMENT -> listOf(
                Pattern.compile("""(?i)Payment\s+Tk\.?\s*([0-9,]+\.?[0-9]*)"""),
                Pattern.compile("""(?i)Payment\s+of\s+Tk\.?\s*([0-9,]+\.?[0-9]*)""")
            )
            BkashTransaction.TYPE_CASH_OUT -> listOf(
                Pattern.compile("""(?i)Cash\s*Out\s+Tk\.?\s*([0-9,]+\.?[0-9]*)"""),
                Pattern.compile("""(?i)Cash\s*Out\s+of\s+Tk\.?\s*([0-9,]+\.?[0-9]*)""")
            )
            BkashTransaction.TYPE_SEND_MONEY -> listOf(
                Pattern.compile("""(?i)amount\s+Tk\.?\s*([0-9,]+\.?[0-9]*)"""),
                Pattern.compile("""(?i)Send\s*Money\s+Tk\.?\s*([0-9,]+\.?[0-9]*)""")
            )
            BkashTransaction.TYPE_RECHARGE -> listOf(
                Pattern.compile("""(?i)(?:of\s+)?Tk\.?\s*([0-9,]+\.?[0-9]*)\s+to"""),
                Pattern.compile("""(?i)Recharge\s+Tk\.?\s*([0-9,]+\.?[0-9]*)""")
            )
            else -> emptyList()
        }

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val numStr = matcher.group(1)?.replace(",", "")?.trim()
                val parsed = numStr?.toDoubleOrNull()
                if (parsed != null) return parsed
            }
        }

        // Generic fallback: first "Tk X" that isn't Balance or Fee
        val genericPattern = Pattern.compile("""(?i)(?:Tk|Tk\.|BDT)\s*([0-9,]+\.?[0-9]*)""")
        val matcher = genericPattern.matcher(body)
        while (matcher.find()) {
            val start = matcher.start()
            val prefix = if (start > 10) body.substring(start - 10, start).lowercase() else body.substring(0, start).lowercase()
            if (!prefix.contains("balance") && !prefix.contains("fee")) {
                val numStr = matcher.group(1)?.replace(",", "")?.trim()
                val parsed = numStr?.toDoubleOrNull()
                if (parsed != null) return parsed
            }
        }

        return 0.0
    }

    private fun extractCounterparty(body: String, type: String): String {
        val patterns = when (type) {
            BkashTransaction.TYPE_RECEIVED_MONEY, BkashTransaction.TYPE_CASH_IN -> listOf(
                Pattern.compile("""(?i)from\s+([0-9A-Za-z\+\-\s\(\)]+?)(?:\.|\s+Ref|\s+Fee|\s+Balance|$)"""),
                Pattern.compile("""(?i)from\s+([0-9\+]{10,14})""")
            )
            BkashTransaction.TYPE_PAYMENT, BkashTransaction.TYPE_CASH_OUT -> listOf(
                Pattern.compile("""(?i)to\s+([0-9A-Za-z\+\-\s\(\)]+?)(?:\s+successful|\.|\s+fee|\s+Ref|\s+Balance|$)"""),
                Pattern.compile("""(?i)to\s+([0-9\+]{10,14})""")
            )
            BkashTransaction.TYPE_SEND_MONEY -> listOf(
                Pattern.compile("""(?i)to\s+([0-9A-Za-z\+\-\s\(\)]+?)(?:\s+amount|\.|\s+fee|$)"""),
                Pattern.compile("""(?i)to\s+([0-9\+]{10,14})""")
            )
            BkashTransaction.TYPE_RECHARGE -> listOf(
                Pattern.compile("""(?i)to\s+([0-9\+]{10,14})"""),
                Pattern.compile("""(?i)to\s+([0-9A-Za-z\+\-\s]+?)(?:\s+successful|\.|$)""")
            )
            else -> listOf(
                Pattern.compile("""(?i)(?:from|to)\s+([0-9A-Za-z\+\-\s\(\)]+?)(?:\.|\s+Ref|\s+Fee|\s+Balance|$)""")
            )
        }

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val name = matcher.group(1)?.trim()
                if (!name.isNullOrEmpty()) return name
            }
        }

        return "Unknown"
    }

    private fun extractFee(body: String): Double {
        val pattern = Pattern.compile("""(?i)fee\s*(?:Tk\.?)?\s*([0-9,]+\.?[0-9]*)""")
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "")?.trim()
            return numStr?.toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun extractBalance(body: String): Double {
        val pattern = Pattern.compile("""(?i)Balance\s*(?:Tk\.?)?\s*([0-9,]+\.?[0-9]*)""")
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "")?.trim()
            return numStr?.toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun extractReference(body: String): String? {
        val pattern = Pattern.compile("""(?i)Ref\s+([^\.]+?)(?:\.|\s+Fee|\s+Balance|\s+TrxID|$)""")
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            val ref = matcher.group(1)?.trim()
            if (!ref.isNullOrEmpty() && !ref.equals("none", ignoreCase = true)) {
                return ref
            }
        }
        return null
    }
}

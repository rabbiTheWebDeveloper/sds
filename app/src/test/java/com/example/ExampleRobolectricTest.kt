package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BkashTransaction
import com.example.data.parser.BkashSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("bKash SMS Sync", appName)
  }

  @Test
  fun `parse received money sms correctly`() {
    val sms = "You have received Tk 1,500.00 from 01712345678. Fee Tk 0.00. Balance Tk 5,230.50. TrxID 9K382JDL8A at 12/09/2026 14:35"
    val parsed = BkashSmsParser.parse("bKash", sms, 1726153000000L)

    assertNotNull(parsed)
    assertEquals("9K382JDL8A", parsed?.trxId)
    assertEquals(1500.0, parsed?.amount ?: 0.0, 0.01)
    assertEquals(BkashTransaction.TYPE_RECEIVED_MONEY, parsed?.type)
    assertEquals(5230.50, parsed?.balance ?: 0.0, 0.01)
    assertTrue(parsed?.isIncoming == true)
  }

  @Test
  fun `parse payment sms correctly`() {
    val sms = "Payment Tk 450.00 to Shwapno Superstore (01912345678) successful. Ref Grocery. Fee Tk 0.00. Balance Tk 3,250.00. TrxID 7N291KLS92 at 12/09/2026 12:10"
    val parsed = BkashSmsParser.parse("bKash", sms, 1726153000000L)

    assertNotNull(parsed)
    assertEquals("7N291KLS92", parsed?.trxId)
    assertEquals(450.0, parsed?.amount ?: 0.0, 0.01)
    assertEquals(BkashTransaction.TYPE_PAYMENT, parsed?.type)
    assertEquals("Grocery", parsed?.reference)
    assertEquals(3250.0, parsed?.balance ?: 0.0, 0.01)
  }
}

package com.example.finalytics

import com.example.finalytics.util.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun testIsTransactionSms() {
        val debitSms = "Your A/C XX1234 has been debited for Rs 500.00 to VPA amazon@apl on 22-06-26."
        val creditSms = "Amt Credited: Rs 1,000.00 from HDFC Bank on 22-06-26."
        val spentSms = "Rs. 150.00 spent on HDFC Bank Credit Card at Amazon."
        val nonTransactionSms = "Hey, are we still meeting today at 5 PM?"

        assertTrue(SmsParser.isTransactionSms(debitSms))
        assertTrue(SmsParser.isTransactionSms(creditSms))
        assertTrue(SmsParser.isTransactionSms(spentSms))
        assertFalse(SmsParser.isTransactionSms(nonTransactionSms))
    }

    @Test
    fun testParseAmount() {
        assertEquals(500.0, SmsParser.parseAmount("debited for Rs 500.00 to")!!, 0.001)
        assertEquals(1234.50, SmsParser.parseAmount("Rs. 1,234.50 spent at")!!, 0.001)
        assertEquals(1500.0, SmsParser.parseAmount("Amt Credited: INR 1500.00 from")!!, 0.001)
        assertNull(SmsParser.parseAmount("No money mentioned here."))
    }

    @Test
    fun testParseMerchant() {
        val defaultSender = "AD-HDFCBK"
        
        assertEquals("Amazon", SmsParser.parseMerchant("Rs. 150.00 spent at Amazon.", defaultSender))
        assertEquals("amazon@apl", SmsParser.parseMerchant("debited to vpa amazon@apl on 22-06-26", defaultSender))
        assertEquals("John Doe", SmsParser.parseMerchant("Received from John Doe on UPI Ref 12345", defaultSender))
        assertEquals(defaultSender, SmsParser.parseMerchant("debited for Rs. 500", defaultSender))
    }

    @Test
    fun testDetectCategory() {
        // 1. Mark a transaction as an expense if it only contains the term "debited".
        assertEquals("Expense", SmsParser.detectCategory("Your account has been debited for Rs 100."))
        
        // 2. Mark a transaction as income if it only contains the term "credited".
        assertEquals("Income", SmsParser.detectCategory("Rs 500.00 credited to your account."))

        // 3. If the message contains the term 'debited' before 'credited', then mark the transaction as an expense.
        assertEquals("Expense", SmsParser.detectCategory("Your account has been debited for Rs 100. Credited to receiver."))

        // 4. If the message contains 'credited' before 'debited', mark as income.
        assertEquals("Income", SmsParser.detectCategory("Amount credited to account, debited from source."))
        
        // 5. Only "credited" or "Credited" should mark it as income. "credit" or "Credit" alone in text should NOT.
        assertEquals("General", SmsParser.detectCategory("Payment made on Credit Card."))
        
        // Standard category checks
        assertEquals("Shopping", SmsParser.detectCategory("Spent on Credit Card at Amazon."))
        assertEquals("Food", SmsParser.detectCategory("Order debited at Zomato Restaurant."))
    }
}

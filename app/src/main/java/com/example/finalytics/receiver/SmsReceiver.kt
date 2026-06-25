package com.example.finalytics.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.finalytics.data.AppDatabase
import com.example.finalytics.data.entity.Transaction
import com.example.finalytics.util.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val pendingResult = goAsync()

            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    for (sms in messages) {
                        val body = sms.messageBody ?: continue
                        val sender = sms.originatingAddress ?: "Unknown"
                        val timestamp = sms.timestampMillis

                        if (SmsParser.isTransactionSms(body)) {
                            val amount = SmsParser.parseAmount(body) ?: 0.0
                            val merchant = SmsParser.parseMerchant(body, sender)
                            
                            val category = SmsParser.detectCategory(body)
                            val transaction = Transaction(
                                timestamp = timestamp,
                                sender = merchant,
                                amount = amount,
                                category = category,
                                rawText = body
                            )
                            val transactionId = db.transactionDao().insertTransaction(transaction)
                            com.example.finalytics.util.NotificationHelper.sendNotification(
                                context,
                                transactionId,
                                merchant,
                                amount
                            )
                            Log.d("SmsReceiver", "Successfully saved parsed transaction and triggered notification: $transaction")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error saving transaction from SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

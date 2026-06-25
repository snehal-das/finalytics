package com.example.finalytics.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finalytics.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

class PriceSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("PriceSyncWorker", "Starting background price sync...")
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.investmentBalanceDao()

        val prices = try {
            fetchPricesFromApi()
        } catch (e: Exception) {
            Log.w("PriceSyncWorker", "API fetch failed (${e.message}). Falling back to simulated prices.")
            getSimulatedPrices()
        }

        try {
            val balances = dao.getAllBalances().first()
            if (balances.isEmpty()) {
                Log.d("PriceSyncWorker", "No investment balances in database to sync.")
                return@withContext Result.success()
            }

            for (balance in balances) {
                val assetName = balance.assetName
                // Attempt to match by name (case-insensitive)
                val price = prices[assetName] ?: prices.entries.find { it.key.equals(assetName, ignoreCase = true) }?.value
                if (price != null) {
                    val newValue = balance.quantity * price
                    val updatedBalance = balance.copy(
                        currentValue = newValue,
                        lastUpdated = System.currentTimeMillis()
                    )
                    dao.updateBalance(updatedBalance)
                    Log.d("PriceSyncWorker", "Synced $assetName: quantity ${balance.quantity} * price $price = $newValue")
                } else {
                    Log.w("PriceSyncWorker", "No price found for asset: $assetName")
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("PriceSyncWorker", "Error executing database sync operations", e)
            Result.failure()
        }
    }

    private fun fetchPricesFromApi(): Map<String, Double> {
        val url = URL("https://api.placeholderfinancial.com/prices")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            return parsePricesJson(response.toString())
        } else {
            throw Exception("HTTP error code: $responseCode")
        }
    }

    private fun parsePricesJson(jsonString: String): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        val jsonObject = JSONObject(jsonString)
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = jsonObject.getDouble(key)
        }
        return map
    }

    private fun getSimulatedPrices(): Map<String, Double> {
        val random = Random()
        val randomFactor = { min: Double, max: Double -> min + (max - min) * random.nextDouble() }
        
        return mapOf(
            "AAPL" to randomFactor(175.0, 185.0),
            "Bitcoin" to randomFactor(63000.0, 67000.0),
            "Ethereum" to randomFactor(3300.0, 3600.0),
            "HDFC Bank" to randomFactor(1500.0, 1600.0),
            "Mutual Fund A" to randomFactor(45.0, 52.0),
            "Cash" to 1.0
        )
    }
}

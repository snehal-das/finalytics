package com.example.finalytics.util

import java.util.regex.Pattern

object SmsParser {
    // Matches common amount formats: e.g. Rs 100, Rs. 100.50, INR 5,000, Amt: 45.00
    private val AMOUNT_PATTERN = Pattern.compile("(?i)(?:rs\\.?|inr|amt\\.?)\\s*([\\d,]+(?:\\.\\d{1,2})?)")
    
    // Extracts merchant/recipient after keywords like "at", "to", "from", "vpa", optionally skipping a "vpa" prefix
    private val MERCHANT_PATTERN = Pattern.compile("(?i)(?:at|to|from|vpa)\\s+(?:vpa\\s+)?([A-Za-z0-9\\s@\\.\\-_]{2,30})")

    fun isTransactionSms(body: String): Boolean {
        val lower = body.lowercase()
        return lower.contains("debited") || 
               lower.contains("credited") || 
               lower.contains("spent") || 
               lower.contains("vpa")
    }

    fun parseAmount(body: String): Double? {
        val matcher = AMOUNT_PATTERN.matcher(body)
        if (matcher.find()) {
            val cleanAmount = matcher.group(1)?.replace(",", "")
            return cleanAmount?.toDoubleOrNull()
        }
        return null
    }

    fun parseMerchant(body: String, defaultSender: String): String {
        val matcher = MERCHANT_PATTERN.matcher(body)
        if (matcher.find()) {
            val merchant = matcher.group(1)?.trim()
            if (!merchant.isNullOrBlank()) {
                // Split at line breaks, punctuation, or transition keywords like "on", "via", "using", "ref", "for"
                val cleaned = merchant.split(Regex("(?i)[\\n\\r,\\.]|\\s+(?:on|via|using|for|ref|through|to|at|with|by|from)\\b"))[0].trim()
                if (cleaned.isNotEmpty()) {
                    return cleaned
                }
            }
        }
        return defaultSender
    }

    fun detectCategory(body: String): String {
        val lower = body.lowercase()
        
        val hasDebited = lower.contains("debited")
        val hasCredited = lower.contains("credited")
        
        val isExpense = when {
            hasDebited && !hasCredited -> true
            hasCredited && !hasDebited -> false
            hasDebited && hasCredited -> {
                val debitedIndex = lower.indexOf("debited")
                val creditedIndex = lower.indexOf("credited")
                debitedIndex < creditedIndex
            }
            else -> null
        }

        // If isExpense is explicitly determined, use that. Otherwise, check other keywords.
        val classifyAsExpense = if (isExpense != null) {
            isExpense
        } else {
            lower.contains("spent") || lower.contains("vpa")
        }

        val classifyAsIncome = if (isExpense != null) {
            !isExpense
        } else {
            lower.contains("credited")
        }

        return when {
            classifyAsExpense -> {
                when {
                    lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("shopping") -> "Shopping"
                    lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("restaurant") || lower.contains("cafe") -> "Food"
                    lower.contains("uber") || lower.contains("ola") || lower.contains("cab") || lower.contains("travel") || lower.contains("metro") || lower.contains("irctc") -> "Travel"
                    lower.contains("electricity") || lower.contains("water") || lower.contains("bill") || lower.contains("recharge") || lower.contains("airtel") || lower.contains("jio") -> "Utilities"
                    else -> "Expense"
                }
            }
            classifyAsIncome -> "Income"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("shopping") -> "Shopping"
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("restaurant") || lower.contains("cafe") -> "Food"
            lower.contains("uber") || lower.contains("ola") || lower.contains("cab") || lower.contains("travel") || lower.contains("metro") || lower.contains("irctc") -> "Travel"
            lower.contains("electricity") || lower.contains("water") || lower.contains("bill") || lower.contains("recharge") || lower.contains("airtel") || lower.contains("jio") -> "Utilities"
            else -> "General"
        }
    }
}

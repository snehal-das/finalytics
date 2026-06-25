package com.example.finalytics.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finalytics.data.AppDatabase
import com.example.finalytics.data.dao.CategoryDao
import com.example.finalytics.data.entity.Category
import com.example.finalytics.data.entity.InvestmentBalance
import com.example.finalytics.data.entity.Transaction
import com.example.finalytics.worker.PriceSyncWorker
import kotlinx.coroutines.Dispatchers
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.finalytics.util.SmsParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.finalytics.util.PreferenceHelper
import com.example.finalytics.util.NotificationHelper

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()
    private val investmentDao = database.investmentBalanceDao()
    private val categoryDao = database.categoryDao()
    private val workManager = WorkManager.getInstance(application)

    // Flow of all categories
    val categories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Flow of all transactions
    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expenseTransactions: StateFlow<List<Transaction>> = transactions.map { list ->
        list.filter { !it.category.equals("Income", ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val incomeTransactions: StateFlow<List<Transaction>> = transactions.map { list ->
        list.filter { it.category.equals("Income", ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Flow of unique merchants user has interacted with
    val uniqueMerchants: StateFlow<List<String>> = transactions.map { list ->
        list.map { it.sender }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filter configuration state (defaults to current calendar month)
    val filterState = MutableStateFlow(
        FilterState(
            startDate = getStartOfCurrentMonth(),
            endDate = getEndOfCurrentMonth(),
            selectedCategory = "All",
            selectedMerchant = "All"
        )
    )

    private val _editingTransactionFromIntent = MutableStateFlow<Transaction?>(null)
    val editingTransactionFromIntent: StateFlow<Transaction?> = _editingTransactionFromIntent

    private val _isNewTxNotificationsEnabled = MutableStateFlow(PreferenceHelper.isNewTxNotificationsEnabled(application))
    val isNewTxNotificationsEnabled: StateFlow<Boolean> = _isNewTxNotificationsEnabled

    private val _isAuditNotificationsEnabled = MutableStateFlow(PreferenceHelper.isAuditNotificationsEnabled(application))
    val isAuditNotificationsEnabled: StateFlow<Boolean> = _isAuditNotificationsEnabled

    private val _auditNotificationTime = MutableStateFlow(PreferenceHelper.getAuditNotificationTime(application))
    val auditNotificationTime: StateFlow<String> = _auditNotificationTime

    private val _auditTransactionsFromIntent = MutableStateFlow(false)
    val auditTransactionsFromIntent: StateFlow<Boolean> = _auditTransactionsFromIntent

    private val _isMonthlyNotificationsEnabled = MutableStateFlow(PreferenceHelper.isMonthlyNotificationsEnabled(application))
    val isMonthlyNotificationsEnabled: StateFlow<Boolean> = _isMonthlyNotificationsEnabled

    private val _monthlySummaryFromIntent = MutableStateFlow(false)
    val monthlySummaryFromIntent: StateFlow<Boolean> = _monthlySummaryFromIntent

    // Flow of transactions filtered by date range, category, and merchant
    val filteredTransactions: StateFlow<List<Transaction>> = combine(transactions, filterState) { txList, filter ->
        txList.filter { tx ->
            val matchesTime = tx.timestamp in filter.startDate..filter.endDate
            val matchesCategory = filter.selectedCategory == "All" || tx.category.equals(filter.selectedCategory, ignoreCase = true)
            val matchesMerchant = filter.selectedMerchant == "All" || tx.sender.equals(filter.selectedMerchant, ignoreCase = true)
            matchesTime && matchesCategory && matchesMerchant
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setEditingTransactionFromIntent(transactionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val tx = transactionDao.getTransactionById(transactionId)
            _editingTransactionFromIntent.value = tx
        }
    }

    fun clearEditingTransactionFromIntent() {
        _editingTransactionFromIntent.value = null
    }

    fun setNewTxNotificationsEnabled(enabled: Boolean) {
        PreferenceHelper.setNewTxNotificationsEnabled(getApplication(), enabled)
        _isNewTxNotificationsEnabled.value = enabled
    }

    fun setAuditNotificationsEnabled(enabled: Boolean) {
        PreferenceHelper.setAuditNotificationsEnabled(getApplication(), enabled)
        _isAuditNotificationsEnabled.value = enabled
        NotificationHelper.scheduleAuditAlarm(getApplication())
    }

    fun setAuditNotificationTime(time: String) {
        PreferenceHelper.setAuditNotificationTime(getApplication(), time)
        _auditNotificationTime.value = time
        if (_isAuditNotificationsEnabled.value) {
            NotificationHelper.scheduleAuditAlarm(getApplication())
        }
    }

    fun triggerAuditTransactionsFromIntent() {
        _auditTransactionsFromIntent.value = true
    }

    fun clearAuditTransactionsFromIntent() {
        _auditTransactionsFromIntent.value = false
    }

    fun setMonthlyNotificationsEnabled(enabled: Boolean) {
        PreferenceHelper.setMonthlyNotificationsEnabled(getApplication(), enabled)
        _isMonthlyNotificationsEnabled.value = enabled
        NotificationHelper.scheduleMonthlyAlarm(getApplication())
    }

    fun triggerMonthlySummaryFromIntent() {
        _monthlySummaryFromIntent.value = true
    }

    fun clearMonthlySummaryFromIntent() {
        _monthlySummaryFromIntent.value = false
    }

    fun setFiltersToPreviousMonth() {
        val calendar = Calendar.getInstance()
        // Move back 1 month
        calendar.add(Calendar.MONTH, -1)
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfPrevMonth = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfPrevMonth = calendar.timeInMillis

        filterState.value = FilterState(
            startDate = startOfPrevMonth,
            endDate = endOfPrevMonth,
            selectedCategory = "All",
            selectedMerchant = "All"
        )
    }

    fun setFiltersToToday() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfToday = calendar.timeInMillis

        filterState.value = FilterState(
            startDate = startOfToday,
            endDate = endOfToday,
            selectedCategory = "All",
            selectedMerchant = "All"
        )
    }

    fun updateFilters(startDate: Long, endDate: Long, category: String, merchant: String) {
        filterState.value = FilterState(startDate, endDate, category, merchant)
    }

    fun resetFilters() {
        filterState.value = FilterState(
            startDate = getStartOfCurrentMonth(),
            endDate = getEndOfCurrentMonth(),
            selectedCategory = "All",
            selectedMerchant = "All"
        )
    }

    private fun getStartOfCurrentMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfCurrentMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    // Flow of all investments
    val investments: StateFlow<List<InvestmentBalance>> = investmentDao.getAllBalances()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Total net worth: sum of all current values of investment assets
    val netWorth: StateFlow<Double> = investments.map { balances ->
        balances.sumOf { it.currentValue }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Expenses grouped by category (current month)
    val categoryExpenses: StateFlow<Map<String, Double>> = expenseTransactions.map { list ->
        val start = getStartOfCurrentMonth()
        val end = getEndOfCurrentMonth()
        list.filter { it.isEnabled && it.timestamp in start..end }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Total expenses (current month)
    val totalExpenses: StateFlow<Double> = categoryExpenses.map { map ->
        map.values.sum()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Total income (current month)
    val totalIncome: StateFlow<Double> = incomeTransactions.map { list ->
        val start = getStartOfCurrentMonth()
        val end = getEndOfCurrentMonth()
        list.filter { it.isEnabled && it.timestamp in start..end }
            .sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    init {
        // Prepulate database with sample data if it is empty, for display purposes
        if (!isRunningTests()) {
            viewModelScope.launch(Dispatchers.IO) {
                prepopulateIfEmpty()
            }
        }
        NotificationHelper.scheduleAuditAlarm(application)
        NotificationHelper.scheduleMonthlyAlarm(application)
    }

    private fun isRunningTests(): Boolean {
        try {
            for (element in Thread.currentThread().stackTrace) {
                val className = element.className
                if (className.contains("org.junit.") || 
                    className.contains("androidx.test.") || 
                    className.contains("robolectric") || 
                    className.contains("runner.AndroidJUnitRunner")) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return false
    }

    fun addTransaction(sender: String, amount: Double, category: String, timestamp: Long, isEnabled: Boolean, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.insertTransaction(
                Transaction(
                    sender = sender,
                    amount = amount,
                    category = category,
                    timestamp = timestamp,
                    rawText = "Manually entered transaction",
                    isEnabled = isEnabled,
                    note = note
                )
            )
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.deleteTransaction(transaction)
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryDao.insertCategory(Category(name = name))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryDao.deleteCategory(category)
            transactionDao.updateCategoryName(category.name, "General")
        }
    }

    fun updateCategory(category: Category, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryDao.updateCategory(category.copy(name = newName))
            transactionDao.updateCategoryName(category.name, newName)
        }
    }

    fun syncPrices() {
        val syncRequest = OneTimeWorkRequestBuilder<PriceSyncWorker>().build()
        workManager.enqueue(syncRequest)
    }

    fun scanSmsInbox(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(
                    Uri.parse("content://sms/inbox"),
                    arrayOf("body", "address", "date"),
                    null,
                    null,
                    "date DESC"
                )

                cursor?.use { c ->
                    val bodyIndex = c.getColumnIndexOrThrow("body")
                    val addressIndex = c.getColumnIndexOrThrow("address")
                    val dateIndex = c.getColumnIndexOrThrow("date")

                    val existingTxs = transactionDao.getAllTransactions().first()
                    val existingKeys = existingTxs.map { 
                        Triple(it.timestamp, it.amount, it.sender.lowercase().trim()) 
                    }.toSet()

                    val transactionsToInsert = mutableListOf<Transaction>()

                    while (c.moveToNext()) {
                        val body = c.getString(bodyIndex) ?: continue
                        val address = c.getString(addressIndex) ?: "Unknown"
                        val date = c.getLong(dateIndex)

                        if (SmsParser.isTransactionSms(body)) {
                            val amount = SmsParser.parseAmount(body) ?: 0.0
                            val merchant = SmsParser.parseMerchant(body, address)
                            val category = SmsParser.detectCategory(body)

                            val key = Triple(date, amount, merchant.lowercase().trim())
                            if (!existingKeys.contains(key)) {
                                transactionsToInsert.add(
                                    Transaction(
                                        timestamp = date,
                                        sender = merchant,
                                        amount = amount,
                                        category = category,
                                        rawText = body
                                    )
                                )
                            }
                        }
                    }

                    if (transactionsToInsert.isNotEmpty()) {
                        transactionDao.insertAll(transactionsToInsert)
                        Log.d("DashboardViewModel", "Scanned and imported ${transactionsToInsert.size} new transactions from inbox SMS.")
                    } else {
                        Log.d("DashboardViewModel", "SMS Inbox scanned. No new transaction messages found.")
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error scanning SMS inbox", e)
            }
        }
    }

    private suspend fun prepopulateIfEmpty() {
        // Ensure default categories exist
        val currentCategories = categoryDao.getAllCategories().stateIn(viewModelScope).value
        if (currentCategories.isEmpty()) {
            categoryDao.insertAll(
                listOf(
                    Category(name = "Food"),
                    Category(name = "Shopping"),
                    Category(name = "Utilities"),
                    Category(name = "Travel"),
                    Category(name = "Income"),
                    Category(name = "General")
                )
            )
        }

        val currentBalances = investmentDao.getAllBalances().stateIn(viewModelScope).value
        val currentTransactions = transactionDao.getAllTransactions().stateIn(viewModelScope).value

        if (currentBalances.isEmpty() && currentTransactions.isEmpty()) {
            val now = System.currentTimeMillis()
            
            // Insert Sample Transactions
            transactionDao.insertAll(
                listOf(
                    Transaction(
                        timestamp = now - 3600000 * 2, // 2 hours ago
                        sender = "Sample Transaction",
                        amount = 1.00,
                        category = "Food",
                        rawText = "Your A/C XX1234 has been debited for Rs 1.00",
                        note = "Click on the transaction to see more details..."
                    )
                )
            )
        }
    }
}

data class FilterState(
    val startDate: Long,
    val endDate: Long,
    val selectedCategory: String = "All",
    val selectedMerchant: String = "All"
)

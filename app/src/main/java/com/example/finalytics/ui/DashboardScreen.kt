package com.example.finalytics.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.finalytics.data.entity.InvestmentBalance
import com.example.finalytics.data.entity.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    Dashboard,
    Filters,
    Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var permissionsGranted by remember { mutableStateOf(hasSmsPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val receive = permissions[android.Manifest.permission.RECEIVE_SMS] ?: false
        val read = permissions[android.Manifest.permission.READ_SMS] ?: false
        permissionsGranted = receive && read
        if (permissionsGranted) {
            viewModel.scanSmsInbox(context)
        }
    }

    var notificationPermissionGranted by remember { 
        mutableStateOf(hasNotificationPermission(context)) 
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
    }

    val expenseTransactions by viewModel.expenseTransactions.collectAsState()
    val incomeTransactions by viewModel.incomeTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()

    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val uniqueMerchants by viewModel.uniqueMerchants.collectAsState()

    var showAddAssetDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var showConfigureFiltersDialog by remember { mutableStateOf(false) }

    val editingTransactionFromIntent by viewModel.editingTransactionFromIntent.collectAsState()
    LaunchedEffect(editingTransactionFromIntent) {
        editingTransactionFromIntent?.let { tx ->
            editingTransaction = tx
            viewModel.clearEditingTransactionFromIntent()
        }
    }

    val auditTransactionsFromIntent by viewModel.auditTransactionsFromIntent.collectAsState()
    LaunchedEffect(auditTransactionsFromIntent) {
        if (auditTransactionsFromIntent) {
            viewModel.setFiltersToToday()
            currentScreen = AppScreen.Filters
            viewModel.clearAuditTransactionsFromIntent()
            android.widget.Toast.makeText(context, "Audit Mode: Reviewing today's transactions", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    when (currentScreen) {
        AppScreen.Dashboard -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                text = "Finalytics", 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        actions = {
                            IconButton(onClick = { currentScreen = AppScreen.Filters }) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = "Filters",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { currentScreen = AppScreen.Settings }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { 
                            showAddTransactionDialog = true 
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add, 
                            contentDescription = "Add Transaction"
                        )
                    }
                },
                bottomBar = {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Expenses", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Expenses") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Income", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Star, contentDescription = "Income") }
                        )
                    }
                },
                modifier = modifier
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (selectedTab) {
                        0 -> ExpensesTab(
                            transactions = expenseTransactions,
                            totalExpenses = totalExpenses,
                            permissionsGranted = permissionsGranted,
                            notificationPermissionGranted = notificationPermissionGranted,
                            onGrantPermissionsClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.RECEIVE_SMS,
                                        android.Manifest.permission.READ_SMS
                                    )
                                )
                            },
                            onGrantNotificationPermissionClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onScanClick = { viewModel.scanSmsInbox(context) },
                            onTransactionClick = { editingTransaction = it }
                        )
                        1 -> IncomeTab(
                            totalIncome = totalIncome,
                            transactions = incomeTransactions,
                            onTransactionClick = { editingTransaction = it }
                        )
                    }
                }
            }
        }
        AppScreen.Filters -> {
            BackHandler {
                currentScreen = AppScreen.Dashboard
            }
            FiltersScreen(
                filteredTransactions = filteredTransactions,
                filterState = filterState,
                onBackClick = { currentScreen = AppScreen.Dashboard },
                onConfigureClick = { showConfigureFiltersDialog = true },
                onTransactionClick = { editingTransaction = it }
            )
        }
        AppScreen.Settings -> {
            BackHandler {
                currentScreen = AppScreen.Dashboard
            }
            SettingsScreen(
                categories = categories,
                viewModel = viewModel,
                onBackClick = { currentScreen = AppScreen.Dashboard },
                onAddCategory = { viewModel.addCategory(it) },
                onDeleteCategory = { viewModel.deleteCategory(it) },
                onUpdateCategory = { cat, name -> viewModel.updateCategory(cat, name) }
            )
        }
    }

    // Add Transaction Dialog
    if (showAddTransactionDialog) {
        AddEditTransactionDialog(
            transaction = null,
            categories = categories,
            onDismiss = { showAddTransactionDialog = false },
            onSave = { sender, amount, category, timestamp, enabled, note ->
                viewModel.addTransaction(sender, amount, category, timestamp, enabled, note)
                showAddTransactionDialog = false
            }
        )
    }

    // Edit Transaction Dialog
    if (editingTransaction != null) {
        AddEditTransactionDialog(
            transaction = editingTransaction,
            categories = categories,
            onDismiss = { editingTransaction = null },
            onSave = { sender, amount, category, timestamp, enabled, note ->
                editingTransaction?.let { old ->
                    viewModel.updateTransaction(
                        old.copy(
                            sender = sender,
                            amount = amount,
                            category = category,
                            timestamp = timestamp,
                            isEnabled = enabled,
                            note = note
                        )
                    )
                }
                editingTransaction = null
            },
            onDelete = {
                editingTransaction?.let { viewModel.deleteTransaction(it) }
                editingTransaction = null
            }
        )
    }

    // Manage Categories Dialog
    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            categories = categories,
            onDismiss = { showManageCategoriesDialog = false },
            onAddCategory = { viewModel.addCategory(it) },
            onDeleteCategory = { viewModel.deleteCategory(it) }
        )
    }

    // Configure Filters Dialog
    if (showConfigureFiltersDialog) {
        ConfigureFiltersDialog(
            filterState = filterState,
            categories = categories,
            uniqueMerchants = uniqueMerchants,
            onDismiss = { showConfigureFiltersDialog = false },
            onReset = {
                viewModel.resetFilters()
                showConfigureFiltersDialog = false
            },
            onApply = { start, end, category, merchant ->
                viewModel.updateFilters(start, end, category, merchant)
                showConfigureFiltersDialog = false
            }
        )
    }

    // Add Asset Simulation Dialog
    if (showAddAssetDialog) {
        Dialog(onDismissRequest = { showAddAssetDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Add New Asset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Add investments here. They will automatically be synced by the WorkManager scheduler.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showAddAssetDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
fun ExpensesTab(
    transactions: List<Transaction>,
    totalExpenses: Double,
    permissionsGranted: Boolean,
    notificationPermissionGranted: Boolean,
    onGrantPermissionsClick: () -> Unit,
    onGrantNotificationPermissionClick: () -> Unit,
    onScanClick: () -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SMS Permission Prompt Banner
        if (!permissionsGranted) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Auto-Track SMS Expenses",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Grant SMS permissions to scan your inbox for historical transactions and track incoming alerts in real-time.",
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onGrantPermissionsClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permissions & Scan Inbox")
                        }
                    }
                }
            }
        }

        // Notification Permission Prompt Banner
        if (!notificationPermissionGranted) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Enable Notifications",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Grant notification permissions to receive live transaction alerts and daily audit prompts.",
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onGrantNotificationPermissionClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Notifications")
                        }
                    }
                }
            }
        }

        // Total Expense Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Total Expenses (Current Month)",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(totalExpenses),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }



        // Recent Transactions Title with Manage Categories & Scan Inbox triggers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (permissionsGranted) {
                        TextButton(onClick = onScanClick) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan SMS Inbox",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Inbox", fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Transactions List
        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions detected yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(transactions) { transaction ->
                TransactionRow(transaction, onClick = { onTransactionClick(transaction) })
            }
        }
    }
}

@Composable
fun IncomeTab(
    totalIncome: Double,
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Total Income Card (light green Container with dark green text)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD1FAE5)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Total Income (Current Month)",
                        color = Color(0xFF065F46),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(totalIncome),
                        color = Color(0xFF047857),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Text(
                text = "Income Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No income transactions recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(transactions) { transaction ->
                TransactionRow(transaction, onClick = { onTransactionClick(transaction) })
            }
        }
    }
}

@Composable
fun ExpensesDonutChart(
    categoryExpenses: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val total = categoryExpenses.values.sum()
    val categories = categoryExpenses.keys.toList()
    val amounts = categoryExpenses.values.toList()

    val colors = listOf(
        Color(0xFF8B5CF6),
        Color(0xFF06B6D4),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF3B82F6),
        Color(0xFF10B981)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                amounts.forEachIndexed { index, amount ->
                    val sweepAngle = (amount / total * 360f).toFloat()
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrencyCompact(total), 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEachIndexed { index, category ->
                val percentage = (amounts[index] / total * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[index % colors.size], shape = CircleShape)
                    )
                    Text(
                        text = "$category ($percentage%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

fun getCategoryIcon(categoryName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (categoryName.lowercase()) {
        "food" -> Icons.Default.Restaurant
        "shopping" -> Icons.Default.ShoppingCart
        "utilities" -> Icons.Default.Build
        "travel" -> Icons.Default.DirectionsCar
        "income" -> Icons.Default.TrendingUp
        else -> Icons.Default.Category
    }
}

@Composable
fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    val alpha = if (transaction.isEnabled) 1.0f else 0.5f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .alpha(alpha),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isIncome = transaction.category.equals("Income", ignoreCase = true)
            val iconBackground = if (isIncome) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
            val iconTint = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626)
            val icon = getCategoryIcon(transaction.category)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBackground, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.category,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.sender,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${transaction.category} • ${formatDate(transaction.timestamp)}" + if (transaction.isEnabled) "" else " • Disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.note.isNotBlank()) {
                    val firstLine = transaction.note.lineSequence().firstOrNull() ?: ""
                    if (firstLine.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = firstLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = (if (isIncome) "+" else "-") + formatCurrency(transaction.amount),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isIncome) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AssetRow(asset: InvestmentBalance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.assetName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${asset.assetType} • ${asset.quantity} units",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(asset.currentValue),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Updated: ${formatRelativeTime(asset.lastUpdated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Dialog for Adding or Editing transactions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    transaction: Transaction?,
    categories: List<com.example.finalytics.data.entity.Category>,
    onDismiss: () -> Unit,
    onSave: (sender: String, amount: Double, category: String, timestamp: Long, isEnabled: Boolean, note: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var merchant by remember { mutableStateOf(transaction?.sender ?: "") }
    var amountStr by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(transaction?.category ?: if (categories.isNotEmpty()) categories[0].name else "General") }
    var dateStr by remember { mutableStateOf(formatDateTime(transaction?.timestamp ?: System.currentTimeMillis())) }
    var isEnabled by remember { mutableStateOf(transaction?.isEnabled ?: true) }
    var note by remember { mutableStateOf(transaction?.note ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "Add Transaction" else "Edit Transaction") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Sender") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selection dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category.name
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Date (YYYY-MM-DD HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Toggle for enable/disable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mark as Expense",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }

                if (transaction != null && transaction.rawText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Original SMS:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = transaction.rawText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val timestamp = parseDateTime(dateStr)
                    onSave(merchant, amount, selectedCategory, timestamp, isEnabled, note)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (transaction != null && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// Dialog for Managing Categories
@Composable
fun ManageCategoriesDialog(
    categories: List<com.example.finalytics.data.entity.Category>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (com.example.finalytics.data.entity.Category) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Categories") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("New Category") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCategory(newCategoryName.trim())
                                newCategoryName = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }

                Text(
                    text = "Existing Categories", 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.bodyMedium
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.name, fontWeight = FontWeight.Medium)
                            
                            val isProtected = category.name.equals("Income", ignoreCase = true) || 
                                              category.name.equals("General", ignoreCase = true) ||
                                              category.name.equals("Food", ignoreCase = true) ||
                                              category.name.equals("Shopping", ignoreCase = true)
                            
                            if (!isProtected) {
                                IconButton(
                                    onClick = { onDeleteCategory(category) }, 
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Category",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

enum class SettingsSubScreen {
    Main,
    ManageCategories,
    Notifications
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    categories: List<com.example.finalytics.data.entity.Category>,
    viewModel: DashboardViewModel,
    onBackClick: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (com.example.finalytics.data.entity.Category) -> Unit,
    onUpdateCategory: (com.example.finalytics.data.entity.Category, String) -> Unit
) {
    var subScreen by remember { mutableStateOf(SettingsSubScreen.Main) }

    BackHandler(enabled = subScreen != SettingsSubScreen.Main) {
        subScreen = SettingsSubScreen.Main
    }

    when (subScreen) {
        SettingsSubScreen.Main -> {
            MainSettingsView(
                onBackClick = onBackClick,
                onManageCategoriesClick = { subScreen = SettingsSubScreen.ManageCategories },
                onNotificationsClick = { subScreen = SettingsSubScreen.Notifications }
            )
        }
        SettingsSubScreen.ManageCategories -> {
            ManageCategoriesView(
                categories = categories,
                onBackClick = { subScreen = SettingsSubScreen.Main },
                onAddCategory = onAddCategory,
                onDeleteCategory = onDeleteCategory,
                onUpdateCategory = onUpdateCategory
            )
        }
        SettingsSubScreen.Notifications -> {
            NotificationsSettingsView(
                viewModel = viewModel,
                onBackClick = { subScreen = SettingsSubScreen.Main }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsView(
    onBackClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onManageCategoriesClick() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Manage Categories",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Add, rename, or delete categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Manage Categories",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNotificationsClick() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Notifications",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Configure alerts and daily audit times",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesView(
    categories: List<com.example.finalytics.data.entity.Category>,
    onBackClick: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (com.example.finalytics.data.entity.Category) -> Unit,
    onUpdateCategory: (com.example.finalytics.data.entity.Category, String) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategoryId by remember { mutableStateOf<Long?>(null) }
    var editingName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add New Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("New Category") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName.trim())
                            newCategoryName = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            }

            Text(
                text = "Existing Categories",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isEditing = category.id == editingCategoryId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editingName,
                                onValueChange = { editingName = it },
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                label = { Text("Category Name") },
                                singleLine = true
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (editingName.isNotBlank()) {
                                            onUpdateCategory(category, editingName.trim())
                                        }
                                        editingCategoryId = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save Name",
                                        tint = Color(0xFF059669)
                                    )
                                }
                                IconButton(
                                    onClick = { editingCategoryId = null }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Edit",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            Text(category.name, fontWeight = FontWeight.Medium)

                            val isProtected = category.name.equals("Income", ignoreCase = true) ||
                                    category.name.equals("General", ignoreCase = true) ||
                                    category.name.equals("Food", ignoreCase = true) ||
                                    category.name.equals("Shopping", ignoreCase = true)

                            if (isProtected) {
                                Text(
                                    text = "System",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            editingCategoryId = category.id
                                            editingName = category.name
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Category Name",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteCategory(category) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Category",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Filters Results Screen (displays expenses from configured period by default)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    filteredTransactions: List<Transaction>,
    filterState: FilterState,
    onBackClick: () -> Unit,
    onConfigureClick: () -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    val totalFilteredExpenses = filteredTransactions
        .filter { !it.category.equals("Income", ignoreCase = true) }
        .sumOf { it.amount }


    Scaffold(
        topBar = {
            val context = LocalContext.current
            TopAppBar(
                title = { Text("Filter Expenses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    IconButton(onClick = { exportToCsv(context, filteredTransactions) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onConfigureClick) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Configure Filters")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Configure", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Active Filters",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Period: ${formatDateOnly(filterState.startDate)} to ${formatDateOnly(filterState.endDate)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Category: ${filterState.selectedCategory} • Merchant: ${filterState.selectedMerchant}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Filtered Expense Total",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(totalFilteredExpenses),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Matching Transactions (${filteredTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredTransactions) { transaction ->
                    TransactionRow(transaction, onClick = { onTransactionClick(transaction) })
                }
            }
        }
    }
}

// Dialog for configuring custom filter filters
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureFiltersDialog(
    filterState: FilterState,
    categories: List<com.example.finalytics.data.entity.Category>,
    uniqueMerchants: List<String>,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (start: Long, end: Long, category: String, merchant: String) -> Unit
) {
    var startDateStr by remember { mutableStateOf(formatDateOnly(filterState.startDate)) }
    var endDateStr by remember { mutableStateOf(formatDateOnly(filterState.endDate)) }
    
    var selectedCategory by remember { mutableStateOf(filterState.selectedCategory) }
    var selectedMerchant by remember { mutableStateOf(filterState.selectedMerchant) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var merchantExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Filters") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startDateStr,
                    onValueChange = { startDateStr = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endDateStr,
                    onValueChange = { endDateStr = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = {
                                selectedCategory = "All"
                                categoryExpanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category.name
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Merchant selector dropdown
                ExposedDropdownMenuBox(
                    expanded = merchantExpanded,
                    onExpandedChange = { merchantExpanded = !merchantExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedMerchant,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Merchant / Sender") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = merchantExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = merchantExpanded,
                        onDismissRequest = { merchantExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Merchants") },
                            onClick = {
                                selectedMerchant = "All"
                                merchantExpanded = false
                            }
                        )
                        uniqueMerchants.forEach { merchant ->
                            DropdownMenuItem(
                                text = { Text(merchant) },
                                onClick = {
                                    selectedMerchant = merchant
                                    merchantExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val start = parseDateOnly(startDateStr, filterState.startDate)
                    val end = parseEndDateOnly(endDateStr, filterState.endDate)
                    onApply(start, end, selectedCategory, selectedMerchant)
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReset) {
                    Text("Reset")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// Helpers
fun hasSmsPermissions(context: Context): Boolean {
    val receive = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    val read = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    return receive && read
}

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
}

fun formatCurrencyCompact(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return format.format(amount)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDateOnly(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun parseDateTime(str: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.parse(str)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

fun parseDateOnly(str: String, default: Long): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.parse(str)?.time ?: default
    } catch (e: Exception) {
        default
    }
}

fun parseEndDateOnly(str: String, default: Long): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(str)
        if (date != null) {
            val cal = Calendar.getInstance()
            cal.time = date
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            cal.timeInMillis
        } else {
            default
        }
    } catch (e: Exception) {
        default
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000} mins ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        else -> "${diff / 86400000} days ago"
    }
}

private fun exportToCsv(context: Context, transactions: List<Transaction>) {
    try {
        val sdf = SimpleDateFormat("yy-MM-dd-HH-mm-ss", Locale.getDefault())
        val currentDateStr = sdf.format(Date())
        val filename = "finalytics-expense-report-$currentDateStr.csv"
        
        val directory = context.getExternalFilesDir(null)
        val file = java.io.File(directory, filename)
        
        file.bufferedWriter().use { writer ->
            writer.write("ID,Timestamp,Date,Merchant,Amount,Category,Enabled,Note\n")
            transactions.forEach { tx ->
                val dateStr = formatDate(tx.timestamp)
                val escapedMerchant = escapeCsvField(tx.sender)
                val escapedCategory = escapeCsvField(tx.category)
                val escapedNote = escapeCsvField(tx.note)
                writer.write("${tx.id},${tx.timestamp},$dateStr,$escapedMerchant,${tx.amount},$escapedCategory,${tx.isEnabled},$escapedNote\n")
            }
        }
        
        android.widget.Toast.makeText(context, "Exported: ${file.name}", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun escapeCsvField(value: String): String {
    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
    return value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsView(
    viewModel: DashboardViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val isNewTxEnabled by viewModel.isNewTxNotificationsEnabled.collectAsState()
    val isAuditEnabled by viewModel.isAuditNotificationsEnabled.collectAsState()
    val auditTime by viewModel.auditNotificationTime.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // New transaction notifications card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New Transaction Alerts",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Sends a notification for\nevery new transaction detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isNewTxEnabled,
                        onCheckedChange = { viewModel.setNewTxNotificationsEnabled(it) }
                    )
                }
            }

            // Audit daily notifications card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Audit Notification",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Reminder to verify and categorize\ntoday's transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAuditEnabled,
                            onCheckedChange = { viewModel.setAuditNotificationsEnabled(it) }
                        )
                    }

                    if (isAuditEnabled) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val parts = auditTime.split(":")
                                    val currentHour = parts.getOrNull(0)?.toIntOrNull() ?: 21
                                    val currentMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val newTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                                            viewModel.setAuditNotificationTime(newTime)
                                        },
                                        currentHour,
                                        currentMinute,
                                        false
                                    ).show()
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Audit Time",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "configure a convenient time to\nrecieve the daily audit notification",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatTimeStr(auditTime),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeStr(timeStr: String): String {
    val parts = timeStr.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 21
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val suffix = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(java.util.Locale.getDefault(), "%02d:%02d %s", displayHour, minute, suffix)
}



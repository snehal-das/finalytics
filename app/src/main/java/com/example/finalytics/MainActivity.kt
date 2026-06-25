package com.example.finalytics

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.finalytics.ui.DashboardScreen
import com.example.finalytics.ui.DashboardViewModel
import com.example.finalytics.ui.theme.FinalyticsTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinalyticsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
        
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra("edit_transaction_id")) {
                val transactionId = it.getLongExtra("edit_transaction_id", -1L)
                if (transactionId != -1L) {
                    viewModel.setEditingTransactionFromIntent(transactionId)
                }
            }
            if (it.getBooleanExtra("audit_transactions", false)) {
                viewModel.triggerAuditTransactionsFromIntent()
            }
            if (it.getBooleanExtra("monthly_summary", false)) {
                viewModel.triggerMonthlySummaryFromIntent()
            }
        }
    }
}
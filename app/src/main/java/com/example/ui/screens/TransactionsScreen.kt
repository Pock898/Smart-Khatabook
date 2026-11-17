package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCustomer
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TransactionCard
import com.example.ui.localization.AppStrings
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedContainer
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import java.util.Calendar

enum class TxnTypeFilter {
    ALL,
    CREDIT_ONLY,
    PAYMENT_ONLY
}

enum class TxnDateFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

@Composable
fun TransactionsScreen(
    transactions: List<TransactionWithCustomer>,
    currency: String,
    strings: AppStrings.Dictionary,
    onAddTransactionClick: () -> Unit,
    onEditTransaction: (TransactionWithCustomer) -> Unit,
    onDeleteTransaction: (TransactionWithCustomer) -> Unit,
    onViewReceipt: (TransactionWithCustomer) -> Unit,
    onShareTransaction: (TransactionWithCustomer) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf(TxnTypeFilter.ALL) }
    var selectedDateFilter by remember { mutableStateOf(TxnDateFilter.ALL) }
    var transactionToDelete by remember { mutableStateOf<TransactionWithCustomer?>(null) }

    val (startOfToday, startOfWeek, startOfMonth) = remember(transactions, selectedDateFilter) {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        Triple(todayCal.timeInMillis, weekCal.timeInMillis, monthCal.timeInMillis)
    }

    val filteredTransactions = transactions.filter { t ->
        val matchesSearch = t.customerName.contains(searchQuery, ignoreCase = true) ||
            t.description.contains(searchQuery, ignoreCase = true) ||
            t.billNumber.contains(searchQuery, ignoreCase = true) ||
            t.amount.toString().contains(searchQuery)

        val matchesType = when (selectedTypeFilter) {
            TxnTypeFilter.ALL -> true
            TxnTypeFilter.CREDIT_ONLY -> t.type == TransactionType.CREDIT
            TxnTypeFilter.PAYMENT_ONLY -> t.type == TransactionType.PAYMENT
        }

        val matchesDate = when (selectedDateFilter) {
            TxnDateFilter.ALL -> true
            TxnDateFilter.TODAY -> t.transactionDate >= startOfToday
            TxnDateFilter.THIS_WEEK -> t.transactionDate >= startOfWeek
            TxnDateFilter.THIS_MONTH -> t.transactionDate >= startOfMonth
        }

        matchesSearch && matchesType && matchesDate
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by customer, note, bill...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_transactions_input")
                )
            }

            // Type Filter Chips (All / Credit / Payment)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TxnTypeFilter.ALL,
                            onClick = { selectedTypeFilter = TxnTypeFilter.ALL },
                            label = { Text("All Types") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TxnTypeFilter.CREDIT_ONLY,
                            onClick = { selectedTypeFilter = TxnTypeFilter.CREDIT_ONLY },
                            label = { Text("Gave (Udhar)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CreditRedContainer,
                                selectedLabelColor = CreditRed
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TxnTypeFilter.PAYMENT_ONLY,
                            onClick = { selectedTypeFilter = TxnTypeFilter.PAYMENT_ONLY },
                            label = { Text("Got (Payments)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaymentGreenContainer,
                                selectedLabelColor = PaymentGreen
                            )
                        )
                    }
                }
            }

            // Date Filter Chips (All / Today / This Week / This Month)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedDateFilter == TxnDateFilter.ALL,
                            onClick = { selectedDateFilter = TxnDateFilter.ALL },
                            label = { Text("All Time") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDateFilter == TxnDateFilter.TODAY,
                            onClick = { selectedDateFilter = TxnDateFilter.TODAY },
                            label = { Text("Today") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDateFilter == TxnDateFilter.THIS_WEEK,
                            onClick = { selectedDateFilter = TxnDateFilter.THIS_WEEK },
                            label = { Text("This Week") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDateFilter == TxnDateFilter.THIS_MONTH,
                            onClick = { selectedDateFilter = TxnDateFilter.THIS_MONTH },
                            label = { Text("This Month") }
                        )
                    }
                }
            }

            // Transactions List
            if (filteredTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.ReceiptLong,
                        title = strings.noTransactions,
                        message = if (searchQuery.isNotEmpty()) "No entries match your search criteria." else "Start recording customer credit and payments.",
                        buttonText = if (searchQuery.isEmpty()) strings.addCredit else null,
                        onButtonClick = onAddTransactionClick
                    )
                }
            } else {
                items(filteredTransactions, key = { it.transactionId }) { txn ->
                    TransactionCard(
                        transaction = txn,
                        currency = currency,
                        onEdit = { onEditTransaction(txn) },
                        onDelete = { transactionToDelete = txn },
                        onViewReceipt = { onViewReceipt(txn) },
                        onShare = { onShareTransaction(txn) }
                    )
                }
            }
        }

        // New Transaction FAB
        FloatingActionButton(
            onClick = onAddTransactionClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_transaction_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.newTransaction)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.newTransaction, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Delete Confirmation Dialog
    if (transactionToDelete != null) {
        val txn = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text(strings.deleteTransaction, fontWeight = FontWeight.Bold) },
            text = { Text("${strings.deleteTransactionConfirm} (${txn.customerName}: $currency${txn.amount})") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(txn)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditRed)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

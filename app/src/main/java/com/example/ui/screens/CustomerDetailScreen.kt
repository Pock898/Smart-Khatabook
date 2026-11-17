package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BusinessEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.BalanceType
import com.example.data.model.CustomerWithBalance
import com.example.data.model.TransactionType
import com.example.ui.components.EmptyStateView
import com.example.ui.localization.AppStrings
import com.example.ui.pdf.ReportExporter
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import com.example.ui.theme.PendingAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: CustomerWithBalance,
    transactions: List<TransactionEntity>,
    business: BusinessEntity?,
    currency: String,
    strings: AppStrings.Dictionary,
    onBackClick: () -> Unit,
    onAddCreditClick: () -> Unit,
    onAddPaymentClick: () -> Unit,
    onEditCustomerClick: () -> Unit,
    onDeleteCustomerClick: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onReminderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteCustomerDialog by remember { mutableStateOf(false) }

    // Intercept hardware back button
    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("customer_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (customer.phone.isNotBlank()) {
                            Text(
                                text = customer.phone,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    if (customer.phone.isNotBlank()) {
                        IconButton(onClick = {
                            ReportExporter.safeDialPhone(context, customer.phone)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Call")
                        }
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.editCustomer) },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditCustomerClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Download PDF Statement") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                                onClick = {
                                    menuExpanded = false
                                    ReportExporter.generateAndShareCustomerPdf(
                                        context, business, customer, transactions, currency
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV Statement") },
                                leadingIcon = { Icon(Icons.Default.TableChart, null) },
                                onClick = {
                                    menuExpanded = false
                                    ReportExporter.shareStatementCsv(
                                        context, business, customer, transactions, currency
                                    )
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(strings.deleteCustomer, color = CreditRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = CreditRed) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteCustomerDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Dual Action Bottom Bar (Gave / Got)
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAddCreditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CreditRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("gave_credit_button")
                    ) {
                        Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.gaveCreditTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onAddPaymentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("got_payment_button")
                    ) {
                        Icon(Icons.Default.TrendingDown, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.receivedPaymentTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Net Balance Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (customer.balanceType) {
                            BalanceType.YOU_WILL_GET -> CreditRedContainer
                            BalanceType.YOU_WILL_GIVE -> PaymentGreenContainer
                            BalanceType.SETTLED -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (customer.balanceType) {
                                BalanceType.YOU_WILL_GET -> "NET BALANCE (YOU'LL GET / उधारी)"
                                BalanceType.YOU_WILL_GIVE -> "ADVANCE BALANCE (YOU'LL GIVE)"
                                BalanceType.SETTLED -> "ACCOUNT SETTLED"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when (customer.balanceType) {
                                BalanceType.YOU_WILL_GET -> CreditRed
                                BalanceType.YOU_WILL_GIVE -> PaymentGreen
                                BalanceType.SETTLED -> Color.Gray
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when (customer.balanceType) {
                                BalanceType.YOU_WILL_GET -> "$currency%.2f".format(customer.netBalance)
                                BalanceType.YOU_WILL_GIVE -> "$currency%.2f".format(-customer.netBalance)
                                BalanceType.SETTLED -> "$currency 0.00"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = when (customer.balanceType) {
                                BalanceType.YOU_WILL_GET -> CreditRed
                                BalanceType.YOU_WILL_GIVE -> PaymentGreen
                                BalanceType.SETTLED -> MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (customer.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Address: ${customer.address}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick Share / Reminder Buttons
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (customer.balanceType == BalanceType.YOU_WILL_GET && customer.phone.isNotBlank()) {
                                Button(
                                    onClick = onReminderClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = PendingAmber),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Send Reminder", fontSize = 11.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    ReportExporter.generateAndShareCustomerPdf(
                                        context, business, customer, transactions, currency
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF Report", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Ledger Entries Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ledger History (${transactions.size} entries)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (customer.openingBalance > 0.0) {
                        Text(
                            text = "Opening: $currency${customer.openingBalance} (${customer.openingBalanceType})",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Ledger Entries
            if (transactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Receipt,
                        title = "No ledger entries yet",
                        message = "Tap 'Gave (Udhar)' or 'Got (Payment)' below to add the first entry for ${customer.name}."
                    )
                }
            } else {
                items(transactions, key = { it.id }) { txn ->
                    val isCredit = txn.type == TransactionType.CREDIT.value
                    var itemMenuExpanded by remember { mutableStateOf(false) }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isCredit) CreditRedContainer else PaymentGreenContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isCredit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isCredit) CreditRed else PaymentGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (txn.description.isNotBlank()) txn.description else if (isCredit) "Gave Credit" else "Payment Received",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dateFormat.format(Date(txn.transactionDate)),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    if (txn.billNumber.isNotBlank()) {
                                        Text(
                                            text = " • #${txn.billNumber}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (!isCredit && txn.paymentMethod.isNotBlank()) {
                                        Text(
                                            text = " • ${txn.paymentMethod}",
                                            fontSize = 10.sp,
                                            color = PaymentGreen
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = (if (isCredit) "+ " else "- ") + "$currency%.2f".format(txn.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (isCredit) CreditRed else PaymentGreen
                                )
                                Text(
                                    text = if (isCredit) "You Gave" else "You Got",
                                    fontSize = 10.sp,
                                    color = if (isCredit) CreditRed else PaymentGreen
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { itemMenuExpanded = true },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Entry options", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(
                                    expanded = itemMenuExpanded,
                                    onDismissRequest = { itemMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Entry") },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                                        onClick = {
                                            itemMenuExpanded = false
                                            onEditTransaction(txn)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Entry", color = CreditRed) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = CreditRed) },
                                        onClick = {
                                            itemMenuExpanded = false
                                            onDeleteTransaction(txn)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCustomerDialog = false },
            title = { Text(strings.deleteCustomer, fontWeight = FontWeight.Bold) },
            text = { Text(strings.deleteCustomerConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteCustomerDialog = false
                        onDeleteCustomerClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditRed)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCustomerDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

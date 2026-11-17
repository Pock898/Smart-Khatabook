package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BusinessEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.BalanceType
import com.example.data.model.CustomerWithBalance
import com.example.data.model.DashboardSummary
import com.example.data.model.StaffWithSummary
import com.example.data.model.TransactionWithCustomer
import com.example.ui.components.CreditPaymentCanvasChart
import com.example.ui.components.CustomerCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatCard
import com.example.ui.components.TransactionCard
import com.example.ui.localization.AppStrings
import com.example.ui.pdf.ReportExporter
import com.example.ui.theme.AttendanceAbsentRed
import com.example.ui.theme.AttendancePresentGreen
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedContainer
import com.example.ui.theme.KhatabookBlue
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberContainer

private enum class DashboardCustomerFilter {
    ALL,
    YOU_WILL_GET,
    YOU_WILL_GIVE
}

@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    customers: List<CustomerWithBalance>,
    recentTransactions: List<TransactionWithCustomer>,
    staffList: List<StaffWithSummary>,
    business: BusinessEntity?,
    currency: String,
    strings: AppStrings.Dictionary,
    onNavigateToCustomers: () -> Unit,
    onNavigateToStaff: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit,
    onCustomerClick: (CustomerWithBalance) -> Unit,
    onReminderClick: (CustomerWithBalance) -> Unit,
    onAddCustomerClick: () -> Unit,
    onAddCreditClick: () -> Unit,
    onAddPaymentClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onTransactionClick: (TransactionWithCustomer) -> Unit,
    onEditTransaction: (TransactionWithCustomer) -> Unit,
    onDeleteTransaction: (TransactionWithCustomer) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var customerSearch by remember { mutableStateOf("") }
    var customerFilter by remember { mutableStateOf(DashboardCustomerFilter.ALL) }

    val filteredCustomers = remember(customers, customerSearch, customerFilter) {
        customers.filter { cust ->
            val matchesQuery = customerSearch.isBlank() ||
                cust.name.contains(customerSearch, ignoreCase = true) ||
                cust.phone.contains(customerSearch, ignoreCase = true)
            val matchesFilter = when (customerFilter) {
                DashboardCustomerFilter.ALL -> true
                DashboardCustomerFilter.YOU_WILL_GET -> cust.balanceType == BalanceType.YOU_WILL_GET
                DashboardCustomerFilter.YOU_WILL_GIVE -> cust.balanceType == BalanceType.YOU_WILL_GIVE
            }
            matchesQuery && matchesFilter
        }
    }
    val displayedCustomers = remember(filteredCustomers) { filteredCustomers.take(8) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome & Business Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NavyPrimary, KhatabookBlue)
                            )
                        )
                        .testTag("dashboard_header_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = business?.name ?: strings.appName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Digital Credit Account Book",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${summary.totalCustomers} ${strings.customers} • ${summary.pendingCustomersCount} ${strings.pendingPayments}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Primary Summary Cards: Total Credit & Total Expenses
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = strings.totalCredit,
                        amount = summary.totalCredit,
                        currency = currency,
                        icon = Icons.Default.TrendingUp,
                        color = CreditRed,
                        containerColor = CreditRedContainer,
                        subtitle = "${strings.youWillGet}: $currency%.2f".format(summary.totalReceivable),
                        onClick = onNavigateToTransactions,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_total_credit_card")
                    )

                    StatCard(
                        title = strings.totalExpenses,
                        amount = summary.totalExpenses,
                        currency = currency,
                        icon = Icons.Default.ShoppingBag,
                        color = PendingAmber,
                        containerColor = PendingAmberContainer,
                        subtitle = "${strings.today}: $currency%.2f".format(summary.todayExpenses),
                        onClick = onNavigateToExpenses,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_total_expenses_card")
                    )
                }
            }

            // Ledger Snapshot Strip: You'll Get, You'll Give, Today's Collection
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_balance_snapshot_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SnapshotMetricColumn(
                            label = strings.totalReceivable,
                            value = "$currency%.2f".format(summary.totalReceivable),
                            color = CreditRed,
                            onClick = onNavigateToCustomers,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(34.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        )
                        SnapshotMetricColumn(
                            label = strings.totalPayable,
                            value = "$currency%.2f".format(summary.totalPayable),
                            color = PaymentGreen,
                            onClick = onNavigateToCustomers,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(34.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        )
                        SnapshotMetricColumn(
                            label = strings.todayCollection,
                            value = "$currency%.2f".format(summary.todayCollection),
                            color = KhatabookBlue,
                            onClick = onNavigateToTransactions,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Actions Section
            item {
                Column {
                    Text(
                        text = strings.quickActions,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        item {
                            QuickActionButton(
                                icon = Icons.Default.PersonAdd,
                                label = strings.addCustomer,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onAddCustomerClick,
                                testTag = "dashboard_quick_add_customer"
                            )
                        }
                        item {
                            QuickActionButton(
                                icon = Icons.Default.TrendingUp,
                                label = strings.addCredit,
                                color = CreditRed,
                                onClick = onAddCreditClick,
                                testTag = "dashboard_quick_add_credit"
                            )
                        }
                        item {
                            QuickActionButton(
                                icon = Icons.Default.TrendingDown,
                                label = strings.addPayment,
                                color = PaymentGreen,
                                onClick = onAddPaymentClick,
                                testTag = "dashboard_quick_add_payment"
                            )
                        }
                        item {
                            QuickActionButton(
                                icon = Icons.Default.ShoppingBag,
                                label = strings.addExpense,
                                color = PendingAmber,
                                onClick = onAddExpenseClick,
                                testTag = "dashboard_quick_add_expense"
                            )
                        }
                        item {
                            QuickActionButton(
                                icon = Icons.Default.EventAvailable,
                                label = strings.staffAttendance,
                                color = KhatabookBlue,
                                onClick = onNavigateToStaff,
                                testTag = "dashboard_quick_staff"
                            )
                        }
                        item {
                            QuickActionButton(
                                icon = Icons.Default.Analytics,
                                label = strings.viewReports,
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = onNavigateToReports,
                                testTag = "dashboard_quick_reports"
                            )
                        }
                    }
                }
            }

            // Customers List Section Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${strings.customers} (${customers.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onAddCustomerClick,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_header_add_customer")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = strings.addCustomer,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            TextButton(
                                onClick = onNavigateToCustomers,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_view_all_customers")
                            ) {
                                Text(strings.viewAll, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (customers.isNotEmpty()) {
                        OutlinedTextField(
                            value = customerSearch,
                            onValueChange = { customerSearch = it },
                            placeholder = { Text(strings.searchCustomer, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (customerSearch.isNotEmpty()) {
                                    IconButton(onClick = { customerSearch = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_customer_search_input")
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = customerFilter == DashboardCustomerFilter.ALL,
                                    onClick = { customerFilter = DashboardCustomerFilter.ALL },
                                    label = { Text("${strings.allCustomers} (${customers.size})") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = customerFilter == DashboardCustomerFilter.YOU_WILL_GET,
                                    onClick = { customerFilter = DashboardCustomerFilter.YOU_WILL_GET },
                                    label = {
                                        Text("${strings.filterYouWillGet} (${customers.count { it.balanceType == BalanceType.YOU_WILL_GET }})")
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CreditRedContainer,
                                        selectedLabelColor = CreditRed
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = customerFilter == DashboardCustomerFilter.YOU_WILL_GIVE,
                                    onClick = { customerFilter = DashboardCustomerFilter.YOU_WILL_GIVE },
                                    label = {
                                        Text("${strings.filterYouWillGive} (${customers.count { it.balanceType == BalanceType.YOU_WILL_GIVE }})")
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PaymentGreenContainer,
                                        selectedLabelColor = PaymentGreen
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Customer Cards List on Dashboard
            if (customers.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.People,
                        title = strings.noCustomersYet,
                        message = strings.addFirstCustomer,
                        buttonText = strings.addCustomer,
                        onButtonClick = onAddCustomerClick,
                        modifier = Modifier.testTag("dashboard_empty_customers")
                    )
                }
            } else if (displayedCustomers.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = "No matching customers",
                        message = "Try a different name, phone number, or filter."
                    )
                }
            } else {
                items(displayedCustomers, key = { "dash_cust_${it.id}" }) { customer ->
                    CustomerCard(
                        customer = customer,
                        currency = currency,
                        strings = strings,
                        onClick = { onCustomerClick(customer) },
                        onCallClick = {
                            ReportExporter.safeDialPhone(context, customer.phone)
                        },
                        onReminderClick = { onReminderClick(customer) }
                    )
                }

                if (filteredCustomers.size > displayedCustomers.size) {
                    item {
                        OutlinedButton(
                            onClick = onNavigateToCustomers,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("dashboard_more_customers_button")
                        ) {
                            Text("${strings.viewAll} (${filteredCustomers.size})")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Staff Attendance Quick Summary Card
            item {
                val presentToday = staffList.count {
                    it.todayStatus == AttendanceStatus.PRESENT || it.todayStatus == AttendanceStatus.OVERTIME
                }
                val absentToday = staffList.count { it.todayStatus == AttendanceStatus.ABSENT }
                val monthAdvance = staffList.sumOf { it.totalAdvance }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToStaff() }
                        .testTag("dashboard_staff_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = KhatabookBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.staffAttendance,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strings.openReport,
                                    fontSize = 12.sp,
                                    color = KhatabookBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = KhatabookBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = strings.staff, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = staffList.size.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(text = "${strings.today} ${strings.present}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = presentToday.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AttendancePresentGreen
                                )
                            }
                            Column {
                                Text(text = "${strings.today} ${strings.absent}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = absentToday.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AttendanceAbsentRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = strings.advance, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currency%.0f".format(monthAdvance),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KhatabookBlue
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Business Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.monthlySummary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = strings.currentMonth,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = strings.creditGiven, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currency%.2f".format(summary.monthCreditGiven),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditRed
                                )
                            }
                            Column {
                                Text(text = strings.paymentsReceived, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currency%.2f".format(summary.monthPaymentsReceived),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaymentGreen
                                )
                            }
                            Column {
                                Text(text = strings.monthExpenses, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currency%.2f".format(summary.monthExpenses),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PendingAmber
                                )
                            }
                        }
                    }
                }
            }

            // Credit vs Collection Canvas Chart
            item {
                CreditPaymentCanvasChart(
                    creditGiven = summary.monthCreditGiven,
                    paymentsReceived = summary.monthPaymentsReceived,
                    currency = currency
                )
            }

            // Recent Transactions Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentTransactions,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    TextButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(strings.viewAll)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Recent Transactions List
            if (recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.ReceiptLong,
                        title = strings.noTransactions,
                        message = "Transactions will appear here as you record credit and payments.",
                        buttonText = strings.addCredit,
                        onButtonClick = onAddCreditClick
                    )
                }
            } else {
                items(recentTransactions.take(5), key = { it.transactionId }) { txn ->
                    TransactionCard(
                        transaction = txn,
                        currency = currency,
                        onEdit = { onEditTransaction(txn) },
                        onDelete = { onDeleteTransaction(txn) },
                        onViewReceipt = { onTransactionClick(txn) },
                        onShare = { onTransactionClick(txn) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SnapshotMetricColumn(
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

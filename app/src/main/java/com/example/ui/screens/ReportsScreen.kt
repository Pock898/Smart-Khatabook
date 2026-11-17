package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.data.model.BalanceType
import com.example.data.model.CashbookSummary
import com.example.data.model.CategoryExpenseSummary
import com.example.data.model.CustomerWithBalance
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCustomer
import com.example.ui.components.CreditPaymentCanvasChart
import com.example.ui.components.StatCard
import com.example.ui.localization.AppStrings
import com.example.ui.pdf.ReportExporter
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import com.example.ui.theme.PendingAmber
import java.util.Calendar

enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ALL_TIME
}

@Composable
fun ReportsScreen(
    business: BusinessEntity?,
    customers: List<CustomerWithBalance>,
    suppliers: List<CustomerWithBalance> = emptyList(),
    cashbookSummary: CashbookSummary = CashbookSummary(),
    transactions: List<TransactionWithCustomer>,
    expenses: List<ExpenseEntity>,
    categoryExpenses: List<CategoryExpenseSummary>,
    currency: String,
    strings: AppStrings.Dictionary,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.MONTHLY) }

    val (startDate, endDate) = remember(selectedPeriod, transactions.size, expenses.size) {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        when (selectedPeriod) {
            ReportPeriod.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, end)
            }
            ReportPeriod.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, end)
            }
            ReportPeriod.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, end)
            }
            ReportPeriod.YEARLY -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, end)
            }
            ReportPeriod.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
        }
    }

    val periodTxns = transactions.filter { it.transactionDate in startDate..endDate }
    val periodExpenses = expenses.filter { it.expenseDate in startDate..endDate }

    val creditGiven = periodTxns.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
    val paymentsCollected = periodTxns.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }
    val totalExpenseAmt = periodExpenses.sumOf { it.amount }
    val netCashflow = paymentsCollected - totalExpenseAmt

    val totalOutstandingReceivable = customers.filter { it.balanceType == BalanceType.YOU_WILL_GET }.sumOf { it.netBalance }
    val totalSupplierPayable = suppliers.filter { it.balanceType == BalanceType.YOU_WILL_GIVE }.sumOf { -it.netBalance }
    val topPendingCustomers = customers.filter { it.balanceType == BalanceType.YOU_WILL_GET }.sortedByDescending { it.netBalance }.take(5)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period Filter Tabs
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedPeriod == ReportPeriod.DAILY,
                        onClick = { selectedPeriod = ReportPeriod.DAILY },
                        label = { Text(strings.dailyReport) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == ReportPeriod.WEEKLY,
                        onClick = { selectedPeriod = ReportPeriod.WEEKLY },
                        label = { Text(strings.weeklyReport) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == ReportPeriod.MONTHLY,
                        onClick = { selectedPeriod = ReportPeriod.MONTHLY },
                        label = { Text(strings.monthlyReport) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == ReportPeriod.YEARLY,
                        onClick = { selectedPeriod = ReportPeriod.YEARLY },
                        label = { Text(strings.yearlyReport) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == ReportPeriod.ALL_TIME,
                        onClick = { selectedPeriod = ReportPeriod.ALL_TIME },
                        label = { Text("All Time") }
                    )
                }
            }
        }

        // Export Actions Bar (PDF & CSV)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        ReportExporter.generateAndShareBusinessSummaryPdf(
                            context = context,
                            business = business,
                            period = selectedPeriod.name,
                            credit = creditGiven,
                            collection = paymentsCollected,
                            expenses = totalExpenseAmt,
                            cashflow = netCashflow,
                            outstanding = totalOutstandingReceivable,
                            currency = currency
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.exportPdf)
                }

                OutlinedButton(
                    onClick = {
                        ReportExporter.shareBusinessSummaryCsv(
                            context = context,
                            business = business,
                            period = selectedPeriod.name,
                            credit = creditGiven,
                            collection = paymentsCollected,
                            expenses = totalExpenseAmt,
                            cashflow = netCashflow,
                            outstanding = totalOutstandingReceivable,
                            currency = currency
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.TableChart, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.exportCsv)
                }
            }
        }

        // Primary Performance Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Credit Given",
                    amount = creditGiven,
                    currency = currency,
                    icon = Icons.Default.TrendingUp,
                    color = CreditRed,
                    containerColor = CreditRedContainer,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Payments Collected",
                    amount = paymentsCollected,
                    currency = currency,
                    icon = Icons.Default.MonetizationOn,
                    color = PaymentGreen,
                    containerColor = PaymentGreenContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Secondary Stats: Expenses & Net Cashflow
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Total Expenses",
                    amount = totalExpenseAmt,
                    currency = currency,
                    icon = Icons.Default.ShoppingBag,
                    color = PendingAmber,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Net Cash Flow",
                    amount = netCashflow,
                    currency = currency,
                    icon = Icons.Default.Analytics,
                    color = if (netCashflow >= 0) PaymentGreen else CreditRed,
                    containerColor = MaterialTheme.colorScheme.surface,
                    subtitle = "Collected - Expenses",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Visual Canvas Bar Chart
        item {
            CreditPaymentCanvasChart(
                creditGiven = creditGiven,
                paymentsReceived = paymentsCollected,
                currency = currency
            )
        }

        // All Ledgers Summary (Suppliers + Cashbook Cash/Online)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${strings.suppliers} & ${strings.cashbook}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${strings.suppliers} (${strings.youWillGive})", fontSize = 13.sp)
                        Text(
                            text = "$currency%.2f".format(totalSupplierPayable),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PaymentGreen
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${strings.cashbook} (${strings.cashInHand})", fontSize = 13.sp)
                        Text(
                            text = "$currency%.2f".format(cashbookSummary.totalCashInHand),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${strings.cashbook} (${strings.online})", fontSize = 13.sp)
                        Text(
                            text = "$currency%.2f".format(cashbookSummary.totalOnline),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Top Customer Outstanding section
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
                            text = "Customer Outstanding (Total: $currency%.2f)".format(totalOutstandingReceivable),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (topPendingCustomers.isEmpty()) {
                        Text(
                            text = "No pending customer dues! All accounts settled.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        topPendingCustomers.forEach { cust ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    if (cust.phone.isNotBlank()) {
                                        Text(cust.phone, fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                Text(
                                    text = "$currency%.2f".format(cust.netBalance),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = CreditRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expense Category Breakdown
        if (categoryExpenses.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.categoryBreakdown,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        categoryExpenses.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${cat.category.replace("_", " ")} (${cat.count} items)",
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$currency%.2f".format(cat.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

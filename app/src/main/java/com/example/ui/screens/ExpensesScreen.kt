package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExpenseEntity
import com.example.data.model.ExpenseCategory
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ExpenseCard
import com.example.ui.localization.AppStrings
import com.example.ui.theme.AttendanceOffPurple
import com.example.ui.theme.AttendanceOvertimeAmber
import com.example.ui.theme.AttendancePresentGreen
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CreditRed
import com.example.ui.theme.KhatabookBlue
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberContainer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class CurrentMonthCategoryShare(
    val categoryKey: String,
    val displayName: String,
    val amount: Double,
    val percentage: Float
)

data class CurrentMonthExpenseSummary(
    val monthLabel: String,
    val currentMonthTotal: Double,
    val previousMonthTotal: Double,
    val todayTotal: Double,
    val entryCount: Int,
    val dailyAverage: Double,
    val categoryShares: List<CurrentMonthCategoryShare>,
    val startOfCurrentMonth: Long,
    val startOfNextMonth: Long
)

fun calculateCurrentMonthExpenseSummary(
    expenses: List<ExpenseEntity>,
    referenceTimeMillis: Long = System.currentTimeMillis()
): CurrentMonthExpenseSummary {
    val todayCal = Calendar.getInstance().apply {
        timeInMillis = referenceTimeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = todayCal.timeInMillis
    val startOfTomorrow = (todayCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    val monthStartCal = Calendar.getInstance().apply {
        timeInMillis = referenceTimeMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfCurrentMonth = monthStartCal.timeInMillis
    val startOfNextMonth = (monthStartCal.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
    }.timeInMillis
    val startOfPreviousMonth = (monthStartCal.clone() as Calendar).apply {
        add(Calendar.MONTH, -1)
    }.timeInMillis

    val elapsedDaysInMonth = Calendar.getInstance().apply {
        timeInMillis = referenceTimeMillis
    }.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)

    val monthLabel = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        .format(Date(startOfCurrentMonth))

    val currentMonthExpenses = expenses.filter {
        it.expenseDate >= startOfCurrentMonth && it.expenseDate < startOfNextMonth
    }
    val previousMonthExpenses = expenses.filter {
        it.expenseDate >= startOfPreviousMonth && it.expenseDate < startOfCurrentMonth
    }
    val todayTotal = expenses.filter {
        it.expenseDate >= startOfToday && it.expenseDate < startOfTomorrow
    }.sumOf { it.amount }

    val currentMonthTotal = currentMonthExpenses.sumOf { it.amount }
    val previousMonthTotal = previousMonthExpenses.sumOf { it.amount }
    val dailyAverage = currentMonthTotal / elapsedDaysInMonth

    val categoryShares = if (currentMonthTotal > 0.0) {
        currentMonthExpenses
            .groupBy { it.category }
            .map { (catKey, items) ->
                val catTotal = items.sumOf { it.amount }
                val displayName = ExpenseCategory.entries
                    .firstOrNull { it.name == catKey }
                    ?.displayName ?: catKey
                CurrentMonthCategoryShare(
                    categoryKey = catKey,
                    displayName = displayName,
                    amount = catTotal,
                    percentage = (catTotal / currentMonthTotal).toFloat().coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.amount }
    } else {
        emptyList()
    }

    return CurrentMonthExpenseSummary(
        monthLabel = monthLabel,
        currentMonthTotal = currentMonthTotal,
        previousMonthTotal = previousMonthTotal,
        todayTotal = todayTotal,
        entryCount = currentMonthExpenses.size,
        dailyAverage = dailyAverage,
        categoryShares = categoryShares,
        startOfCurrentMonth = startOfCurrentMonth,
        startOfNextMonth = startOfNextMonth
    )
}

private fun categoryAccentColor(categoryKey: String, index: Int): Color {
    return when (categoryKey) {
        ExpenseCategory.INVENTORY.name -> AttendanceOvertimeAmber
        ExpenseCategory.RENT.name -> BlueAccent
        ExpenseCategory.ELECTRICITY.name -> Color(0xFF38BDF8)
        ExpenseCategory.SALARY.name -> AttendancePresentGreen
        ExpenseCategory.FOOD_TEA.name -> Color(0xFFFB7185)
        ExpenseCategory.TRANSPORT.name -> Color(0xFF2DD4BF)
        ExpenseCategory.MAINTENANCE.name -> AttendanceOffPurple
        ExpenseCategory.MARKETING.name -> Color(0xFFF472B6)
        else -> when (index % 4) {
            0 -> AttendanceOvertimeAmber
            1 -> Color(0xFF38BDF8)
            2 -> AttendancePresentGreen
            else -> Color(0xFFFB7185)
        }
    }
}

@Composable
fun ExpensesScreen(
    expenses: List<ExpenseEntity>,
    currency: String,
    strings: AppStrings.Dictionary,
    onAddExpenseClick: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var filterCurrentMonthOnly by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    val monthSummary = remember(expenses) {
        calculateCurrentMonthExpenseSummary(expenses)
    }

    val filteredExpenses = remember(expenses, selectedCategoryFilter, filterCurrentMonthOnly, monthSummary) {
        expenses.filter { exp ->
            val matchesCategory = selectedCategoryFilter == null || exp.category == selectedCategoryFilter
            val matchesMonth = !filterCurrentMonthOnly ||
                (exp.expenseDate >= monthSummary.startOfCurrentMonth &&
                    exp.expenseDate < monthSummary.startOfNextMonth)
            matchesCategory && matchesMonth
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Current Month Visual Summary Card
            item {
                CurrentMonthExpenseSummaryCard(
                    summary = monthSummary,
                    currency = currency,
                    strings = strings,
                    selectedCategoryFilter = selectedCategoryFilter,
                    onCategoryClick = { catKey ->
                        selectedCategoryFilter = if (selectedCategoryFilter == catKey) null else catKey
                    }
                )
            }

            // Period & Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filterCurrentMonthOnly,
                            onClick = { filterCurrentMonthOnly = !filterCurrentMonthOnly },
                            label = {
                                Text(
                                    text = monthSummary.monthLabel,
                                    fontWeight = if (filterCurrentMonthOnly) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PendingAmberContainer,
                                selectedLabelColor = PendingAmber,
                                selectedLeadingIconColor = PendingAmber
                            ),
                            modifier = Modifier.testTag("expense_filter_current_month")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All Categories") }
                        )
                    }
                    ExpenseCategory.entries.forEach { cat ->
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == cat.name,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == cat.name) null else cat.name
                                },
                                label = { Text(cat.displayName) }
                            )
                        }
                    }
                }
            }

            // Expenses List
            if (filteredExpenses.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.ShoppingBag,
                        title = strings.noExpensesYet,
                        message = if (filterCurrentMonthOnly) {
                            "No matching expenses found for ${monthSummary.monthLabel}."
                        } else {
                            "Track shop rent, electricity, inventory, and staff expenses here."
                        },
                        buttonText = strings.addExpense,
                        onButtonClick = onAddExpenseClick
                    )
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        currency = currency,
                        onEdit = { onEditExpense(expense) },
                        onDelete = { expenseToDelete = expense }
                    )
                }
            }
        }

        // Add Expense FAB
        FloatingActionButton(
            onClick = onAddExpenseClick,
            containerColor = PendingAmber,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_expense_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addExpense, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this expense of $currency${exp.amount} (${exp.category})?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(exp)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CurrentMonthExpenseSummaryCard(
    summary: CurrentMonthExpenseSummary,
    currency: String,
    strings: AppStrings.Dictionary,
    selectedCategoryFilter: String?,
    onCategoryClick: (String) -> Unit
) {
    val topShare = summary.categoryShares.firstOrNull()
    val hasLastMonth = summary.previousMonthTotal > 0.0
    val deltaPercent = if (hasLastMonth) {
        (((summary.currentMonthTotal - summary.previousMonthTotal) / summary.previousMonthTotal) * 100.0)
            .roundToInt()
    } else {
        0
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(NavyDark, NavyPrimary, KhatabookBlue)
                )
            )
            .drawBehind {
                drawCircle(
                    color = PendingAmber.copy(alpha = 0.22f),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.92f, size.height * 0.14f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.minDimension * 0.4f,
                    center = Offset(size.width * 0.08f, size.height * 0.92f)
                )
            }
            .testTag("current_month_expense_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title + Current Month Pill + Entry Count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PendingAmber.copy(alpha = 0.24f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = summary.monthLabel,
                                tint = PendingAmberContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.monthExpenses,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = summary.monthLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PendingAmberContainer
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (summary.entryCount == 1) "1 entry" else "${summary.entryCount} entries",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Hero Current Month Total + Trend Comparison Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$currency%.2f".format(summary.currentMonthTotal),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("current_month_expense_total")
                )

                if (hasLastMonth) {
                    val isIncrease = deltaPercent > 0
                    val trendColor = if (isIncrease) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isIncrease) {
                                    Icons.AutoMirrored.Filled.TrendingUp
                                } else {
                                    Icons.AutoMirrored.Filled.TrendingDown
                                },
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${if (deltaPercent > 0) "+" else ""}$deltaPercent% vs prev",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = trendColor
                            )
                        }
                    }
                }
            }

            // Visual Category Breakdown Bar + Interactive Legend
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (summary.categoryShares.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                    )
                    Text(
                        text = "No expenses logged for ${summary.monthLabel} yet.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                } else {
                    val visibleShares = summary.categoryShares.take(4)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        val gapPx = if (summary.categoryShares.size > 1) 3.dp.toPx() else 0f
                        val totalGap = gapPx * (summary.categoryShares.size - 1)
                        val usableWidth = (size.width - totalGap).coerceAtLeast(1f)
                        var currentX = 0f
                        summary.categoryShares.forEachIndexed { idx, share ->
                            val segWidth = (usableWidth * share.percentage).coerceAtLeast(4.dp.toPx())
                            val drawWidth = segWidth.coerceAtMost(size.width - currentX)
                            if (drawWidth > 0f) {
                                drawRoundRect(
                                    color = categoryAccentColor(share.categoryKey, idx),
                                    topLeft = Offset(currentX, 0f),
                                    size = Size(drawWidth, size.height),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                                currentX += drawWidth + gapPx
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        visibleShares.forEachIndexed { idx, share ->
                            val dotColor = categoryAccentColor(share.categoryKey, idx)
                            val isSelected = selectedCategoryFilter == share.categoryKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    Color.White.copy(alpha = 0.26f)
                                } else {
                                    Color.White.copy(alpha = 0.12f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onCategoryClick(share.categoryKey) }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = share.displayName.substringBefore(" /"),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${(share.percentage * 100f).roundToInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PendingAmberContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Sub-Metrics Strip: Today, Top Category, Daily Avg
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ExpenseSubMetric(
                        label = strings.todayExpenses,
                        value = "$currency%.2f".format(summary.todayTotal),
                        modifier = Modifier.weight(1f)
                    )
                    ExpenseSubMetric(
                        label = strings.topCategory,
                        value = topShare?.displayName?.substringBefore(" /") ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    ExpenseSubMetric(
                        label = "Daily Avg",
                        value = "$currency%.0f".format(summary.dailyAverage),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseSubMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BusinessEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.BalanceType
import com.example.data.model.CustomerWithBalance
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCustomer
import com.example.ui.localization.AppStrings
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedContainer
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatabookTopBar(
    business: BusinessEntity?,
    businesses: List<BusinessEntity>,
    onSelectBusiness: (Long) -> Unit,
    onAddNewBusiness: () -> Unit,
    currentLang: String,
    onToggleLang: () -> Unit,
    isPinEnabled: Boolean,
    onLockApp: () -> Unit,
    title: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    var showBusinessMenu by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            } else {
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Business",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        title = {
            if (title != null) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showBusinessMenu = true }
                        .padding(vertical = 4.dp)
                ) {
                    Column {
                        Text(
                            text = business?.name ?: "Smart Khatabook",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (business?.ownerName.isNullOrBlank()) "Tap to switch shop" else business?.ownerName.orEmpty(),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Switch Business",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp).padding(start = 2.dp)
                    )

                    DropdownMenu(
                        expanded = showBusinessMenu,
                        onDismissRequest = { showBusinessMenu = false }
                    ) {
                        Text(
                            text = "My Businesses / दुकानें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        businesses.forEach { b ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(b.name, fontWeight = FontWeight.SemiBold)
                                        if (b.ownerName.isNotBlank()) {
                                            Text(b.ownerName, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = if (b.id == business?.id) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                },
                                onClick = {
                                    onSelectBusiness(b.id)
                                    showBusinessMenu = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Add New Business", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showBusinessMenu = false
                                onAddNewBusiness()
                            }
                        )
                    }
                }
            }
        },
        actions = {
            // Language Switcher Badge (EN / HI)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleLang() }
            ) {
                Text(
                    text = if (currentLang == "hi") "हिंदी" else "EN",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            // Lock button if PIN is configured
            if (isPinEnabled) {
                IconButton(
                    onClick = onLockApp,
                    modifier = Modifier.testTag("lock_app_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock App",
                        tint = Color.White
                    )
                }
            }
        }
    )
}

@Composable
fun StatCard(
    title: String,
    amount: Double,
    currency: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$currency%.2f".format(amount),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer: CustomerWithBalance,
    currency: String,
    strings: AppStrings.Dictionary,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onReminderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // Customer Avatar with Initials
            val initials = customer.name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
                .ifBlank { "C" }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name and last activity
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (customer.phone.isNotBlank()) {
                    Text(
                        text = customer.phone,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (customer.lastTransactionDate != null) {
                        dateFormat.format(Date(customer.lastTransactionDate))
                    } else {
                        "No transactions yet"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Balance Tag
            Column(horizontalAlignment = Alignment.End) {
                when (customer.balanceType) {
                    BalanceType.YOU_WILL_GET -> {
                        Text(
                            text = "$currency%.2f".format(customer.netBalance),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = CreditRed
                        )
                        Text(
                            text = strings.youWillGet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = CreditRed
                        )
                    }
                    BalanceType.YOU_WILL_GIVE -> {
                        Text(
                            text = "$currency%.2f".format(-customer.netBalance),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = PaymentGreen
                        )
                        Text(
                            text = strings.youWillGive,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = PaymentGreen
                        )
                    }
                    BalanceType.SETTLED -> {
                        Text(
                            text = "$currency 0.00",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = strings.settled,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Quick Action Icons
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    if (customer.phone.isNotBlank()) {
                        IconButton(
                            onClick = onCallClick,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (customer.balanceType == BalanceType.YOU_WILL_GET && customer.phone.isNotBlank()) {
                        IconButton(
                            onClick = onReminderClick,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Remind",
                                tint = PendingAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionWithCustomer,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewReceipt: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var menuExpanded by remember { mutableStateOf(false) }
    val isCredit = transaction.type == TransactionType.CREDIT

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            // Direction Icon badge
            Surface(
                shape = CircleShape,
                color = if (isCredit) CreditRedContainer else PaymentGreenContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isCredit) CreditRed else PaymentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.customerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.description.isNotBlank()) {
                    Text(
                        text = transaction.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(transaction.transactionDate)),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    if (transaction.billNumber.isNotBlank()) {
                        Text(
                            text = " • #${transaction.billNumber}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (transaction.paymentMethod.isNotBlank() && !isCredit) {
                        Text(
                            text = " • ${transaction.paymentMethod}",
                            fontSize = 10.sp,
                            color = PaymentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isCredit) "+ " else "- ") + "$currency%.2f".format(transaction.amount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (isCredit) CreditRed else PaymentGreen
                )
                Text(
                    text = if (isCredit) "Gave (Udhar)" else "Received (Got)",
                    fontSize = 10.sp,
                    color = if (isCredit) CreditRed else PaymentGreen
                )
            }

            // Options menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("View Receipt") },
                        leadingIcon = { Icon(Icons.Default.Receipt, null) },
                        onClick = {
                            menuExpanded = false
                            onViewReceipt()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Details") },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Entry") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Entry", color = CreditRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = CreditRed) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: ExpenseEntity,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = PendingAmberContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = expense.category.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = PendingAmber,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category.replace("_", " "),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (expense.description.isNotBlank()) {
                    Text(
                        text = expense.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${dateFormat.format(Date(expense.expenseDate))} • ${expense.paymentMethod}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = "$currency%.2f".format(expense.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = CreditRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = CreditRed) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 18.sp
        )
        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(buttonText)
            }
        }
    }
}

@Composable
fun CreditPaymentCanvasChart(
    creditGiven: Double,
    paymentsReceived: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Credit Given vs Payments Collected",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val maxVal = maxOf(creditGiven, paymentsReceived, 1.0)
            val creditRatio = (creditGiven / maxVal).toFloat().coerceIn(0.05f, 1f)
            val paymentRatio = (paymentsReceived / maxVal).toFloat().coerceIn(0.05f, 1f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val barWidth = size.width * 0.28f
                val spacing = size.width * 0.15f
                val maxHeight = size.height * 0.75f
                val bottomY = size.height - 20f

                // Base guideline
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, bottomY),
                    end = Offset(size.width, bottomY),
                    strokeWidth = 2f
                )

                // Bar 1: Credit (Red)
                val cHeight = maxHeight * creditRatio
                val cLeft = spacing
                drawRoundRect(
                    color = CreditRed,
                    topLeft = Offset(cLeft, bottomY - cHeight),
                    size = Size(barWidth, cHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Bar 2: Payment (Green)
                val pHeight = maxHeight * paymentRatio
                val pLeft = cLeft + barWidth + spacing
                drawRoundRect(
                    color = PaymentGreen,
                    topLeft = Offset(pLeft, bottomY - pHeight),
                    size = Size(barWidth, pHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }

            // Legend & values below chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(CreditRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Credit (Udhar)", fontSize = 12.sp, color = CreditRed, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$currency%.2f".format(creditGiven),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CreditRed
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(PaymentGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Payment (Got)", fontSize = 12.sp, color = PaymentGreen, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$currency%.2f".format(paymentsReceived),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PaymentGreen
                    )
                }
            }
        }
    }
}

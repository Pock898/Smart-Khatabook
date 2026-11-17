package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.data.model.BalanceType
import com.example.data.model.CustomerWithBalance
import com.example.data.model.PartyType
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import com.example.ui.localization.AppStrings
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedContainer
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Computes the projected net balance for [selectedCustomer] after saving a transaction of [newAmount]
 * and [newType]. If [existingTransaction] is non-null and belongs to the same customer, its previous
 * effect is reversed first so the preview stays accurate during edits.
 */
fun calculateProjectedCustomerBalance(
    selectedCustomer: CustomerWithBalance,
    newAmount: Double,
    newType: TransactionType,
    existingTransaction: TransactionEntity? = null
): Double {
    val priorDeltaForCustomer = if (
        existingTransaction != null && existingTransaction.customerId == selectedCustomer.id
    ) {
        if (TransactionType.from(existingTransaction.type) == TransactionType.CREDIT) {
            existingTransaction.amount
        } else {
            -existingTransaction.amount
        }
    } else {
        0.0
    }
    val baseBalance = selectedCustomer.netBalance - priorDeltaForCustomer
    val newDelta = if (newType == TransactionType.CREDIT) newAmount else -newAmount
    return baseBalance + newDelta
}

private fun sanitizeDecimalInput(raw: String): String {
    val cleaned = raw.filter { it.isDigit() || it == '.' }
    val firstDot = cleaned.indexOf('.')
    if (firstDot == -1) return cleaned.take(10)
    val integerPart = cleaned.substring(0, firstDot).take(9)
    val fractionalPart = cleaned.substring(firstDot + 1).replace(".", "").take(2)
    return "$integerPart.$fractionalPart"
}

private fun formatEditableAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        amount.toLong().toString()
    } else {
        "%.2f".format(Locale.US, amount)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionSheet(
    strings: AppStrings.Dictionary,
    currency: String,
    customers: List<CustomerWithBalance>,
    initialCustomerId: Long? = null,
    initialType: TransactionType = TransactionType.CREDIT,
    existingTransaction: TransactionEntity? = null,
    onAddCustomerFirst: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (
        customerId: Long,
        amount: Double,
        type: TransactionType,
        paymentMethod: String,
        description: String,
        billNumber: String,
        transactionDate: Long,
        dueDate: Long?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCustomerId by remember {
        mutableStateOf(
            existingTransaction?.customerId
                ?: initialCustomerId
                ?: customers.firstOrNull()?.id
                ?: 0L
        )
    }
    LaunchedEffect(customers, initialCustomerId, existingTransaction) {
        if (customers.isNotEmpty() && customers.none { it.id == selectedCustomerId }) {
            selectedCustomerId = existingTransaction?.customerId
                ?: initialCustomerId
                ?: customers.first().id
        }
    }

    var type by remember {
        mutableStateOf(
            if (existingTransaction != null) TransactionType.from(existingTransaction.type)
            else initialType
        )
    }
    var amountStr by remember {
        mutableStateOf(
            if (existingTransaction != null) formatEditableAmount(existingTransaction.amount) else ""
        )
    }
    var description by remember {
        mutableStateOf(existingTransaction?.description ?: "")
    }
    var billNumber by remember {
        mutableStateOf(existingTransaction?.billNumber ?: "")
    }
    var paymentMethod by remember {
        mutableStateOf(existingTransaction?.paymentMethod ?: PaymentMethod.CASH.name)
    }
    var transactionDate by remember {
        mutableLongStateOf(existingTransaction?.transactionDate ?: System.currentTimeMillis())
    }

    var amountError by remember { mutableStateOf(false) }
    var customerError by remember { mutableStateOf(false) }
    var customerPickerExpanded by remember { mutableStateOf(false) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId }
    val filteredCustomers = remember(customers, customerSearchQuery) {
        if (customerSearchQuery.isBlank()) {
            customers
        } else {
            val q = customerSearchQuery.trim()
            customers.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.phone.contains(q, ignoreCase = true)
            }
        }
    }

    val isCredit = type == TransactionType.CREDIT
    val accentColor by animateColorAsState(
        targetValue = if (isCredit) CreditRed else PaymentGreen,
        label = "txn_accent"
    )
    val accentContainer by animateColorAsState(
        targetValue = if (isCredit) CreditRedContainer else PaymentGreenContainer,
        label = "txn_accent_container"
    )
    val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = Modifier.testTag("new_transaction_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Sheet Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCredit) {
                                Icons.AutoMirrored.Filled.CallMade
                            } else {
                                Icons.AutoMirrored.Filled.CallReceived
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (existingTransaction == null) strings.newTransaction else strings.editTransaction,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isCredit) strings.gaveCreditTitle else strings.receivedPaymentTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("close_new_transaction_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.cancel
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable Form Body
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Transaction Type Toggle: 'Given' (Credit) vs 'Received' (Payment)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Transaction Type",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TransactionTypeOptionCard(
                            title = strings.givenCreditOption,
                            subtitle = "Udhar • ${strings.youWillGet}",
                            selected = isCredit,
                            accentColor = CreditRed,
                            accentContainer = CreditRedContainer,
                            icon = Icons.AutoMirrored.Filled.CallMade,
                            onClick = { type = TransactionType.CREDIT },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("transaction_type_given")
                        )
                        TransactionTypeOptionCard(
                            title = strings.receivedPaymentOption,
                            subtitle = "Jama • Settles balance",
                            selected = !isCredit,
                            accentColor = PaymentGreen,
                            accentContainer = PaymentGreenContainer,
                            icon = Icons.AutoMirrored.Filled.CallReceived,
                            onClick = { type = TransactionType.PAYMENT },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("transaction_type_received")
                        )
                    }
                }

                // 2. Customer Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.selectCustomerLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (customers.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CreditRedContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = strings.noCustomersYet,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CreditRed
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = strings.addFirstCustomer,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (onAddCustomerFirst != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onAddCustomerFirst,
                                        colors = ButtonDefaults.buttonColors(containerColor = CreditRed),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.addCustomer, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        val isCustomerLocked = initialCustomerId != null && existingTransaction == null
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (customerError) CreditRed else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (!isCustomerLocked) {
                                        Modifier.clickable {
                                            customerPickerExpanded = !customerPickerExpanded
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("transaction_customer_selector")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (selectedCustomer != null) {
                                            Text(
                                                text = selectedCustomer.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 16.sp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedCustomer?.name ?: "Choose a customer",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (selectedCustomer != null) {
                                        val partyTag = if (PartyType.from(selectedCustomer.partyType) == PartyType.SUPPLIER) {
                                            strings.suppliers
                                        } else {
                                            strings.customers
                                        }
                                        val balLabel = formatBalanceLabel(selectedCustomer.netBalance, currency, strings)
                                        Text(
                                            text = buildString {
                                                append(partyTag)
                                                if (selectedCustomer.phone.isNotBlank()) {
                                                    append(" • ${selectedCustomer.phone}")
                                                }
                                                append(" • $balLabel")
                                            },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (!isCustomerLocked) {
                                    Icon(
                                        imageVector = if (customerPickerExpanded) {
                                            Icons.Default.ExpandLess
                                        } else {
                                            Icons.Default.ExpandMore
                                        },
                                        contentDescription = "Select Customer",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (customerError) {
                            Text(
                                text = "Please select a customer",
                                color = CreditRed,
                                fontSize = 12.sp
                            )
                        }

                        // Inline Searchable Customer Picker
                        AnimatedVisibility(visible = customerPickerExpanded && !isCustomerLocked) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                tonalElevation = 2.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    OutlinedTextField(
                                        value = customerSearchQuery,
                                        onValueChange = { customerSearchQuery = it },
                                        placeholder = { Text(strings.searchCustomer, fontSize = 13.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        trailingIcon = {
                                            if (customerSearchQuery.isNotEmpty()) {
                                                IconButton(onClick = { customerSearchQuery = "" }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("transaction_customer_search_input")
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (filteredCustomers.isEmpty()) {
                                        Text(
                                            text = "No matching customer found",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier.heightIn(max = 220.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            filteredCustomers.take(8).forEach { candidate ->
                                                val isChosen = candidate.id == selectedCustomerId
                                                val balanceColor = when (candidate.balanceType) {
                                                    BalanceType.YOU_WILL_GET -> CreditRed
                                                    BalanceType.YOU_WILL_GIVE -> PaymentGreen
                                                    BalanceType.SETTLED -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isChosen) {
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            selectedCustomerId = candidate.id
                                                            customerError = false
                                                            customerPickerExpanded = false
                                                            customerSearchQuery = ""
                                                        }
                                                        .testTag("transaction_customer_option_${candidate.id}")
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 10.dp, vertical = 10.dp)
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = candidate.name,
                                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                                                fontSize = 14.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            if (candidate.phone.isNotBlank()) {
                                                                Text(
                                                                    text = candidate.phone,
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = formatBalanceLabel(candidate.netBalance, currency, strings),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = balanceColor
                                                        )
                                                        if (isChosen) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Selected",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (onAddCustomerFirst != null) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                        TextButton(
                                            onClick = {
                                                customerPickerExpanded = false
                                                onAddCustomerFirst()
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(strings.addCustomer, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Amount Field + Quick Amount Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { raw ->
                            val clean = sanitizeDecimalInput(raw)
                            amountStr = clean
                            amountError = (clean.toDoubleOrNull() ?: 0.0) <= 0.0
                        },
                        label = { Text("${strings.amount} ($currency)") },
                        placeholder = { Text("0.00", fontSize = 20.sp) },
                        leadingIcon = {
                            Text(
                                text = currency,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = amountError,
                        supportingText = {
                            if (amountError) {
                                Text(strings.validationAmount, color = CreditRed)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_amount_input")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        val quickAmounts = listOf(100, 500, 1000, 2000, 5000)
                        items(quickAmounts) { delta ->
                            AssistChip(
                                onClick = {
                                    val updated = (amountStr.toDoubleOrNull() ?: 0.0) + delta
                                    amountStr = formatEditableAmount(updated)
                                    amountError = false
                                },
                                label = {
                                    Text(
                                        text = "+$currency$delta",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }
                }

                // Live Balance Impact Preview
                if (selectedCustomer != null && parsedAmount > 0.0) {
                    val projected = calculateProjectedCustomerBalance(
                        selectedCustomer = selectedCustomer,
                        newAmount = parsedAmount,
                        newType = type,
                        existingTransaction = existingTransaction
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Current Balance",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatBalanceLabel(selectedCustomer.netBalance, currency, strings),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Updated Balance",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatBalanceLabel(projected, currency, strings),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        projected > 0.009 -> CreditRed
                                        projected < -0.009 -> PaymentGreen
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Payment Mode Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.paymentMethod,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(PaymentMethod.entries) { method ->
                            val isSelected = paymentMethod == method.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { paymentMethod = method.name },
                                label = { Text(method.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentContainer,
                                    selectedLabelColor = accentColor
                                )
                            )
                        }
                    }
                }

                // 5. Note / Item Description & Optional Bill Number + Date
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.description) },
                    placeholder = { Text("e.g., Groceries, Invoice #12, Cash received") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_description_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = billNumber,
                        onValueChange = { billNumber = it },
                        label = { Text(strings.billNumber) },
                        leadingIcon = {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transaction_bill_input")
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDatePicker = true }
                            .testTag("transaction_date_picker_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = strings.selectDate,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dateFormat.format(Date(transactionDate)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Bottom Save & Cancel Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 50.dp)
                        .testTag("cancel_transaction_button")
                ) {
                    Text(strings.cancel, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        val hasValidCustomer = selectedCustomerId > 0L && selectedCustomer != null
                        customerError = !hasValidCustomer
                        amountError = amount <= 0.0
                        if (!hasValidCustomer || amountError) {
                            if (!hasValidCustomer && customers.isNotEmpty()) {
                                customerPickerExpanded = true
                            }
                            return@Button
                        }
                        onSave(
                            selectedCustomerId,
                            amount,
                            type,
                            paymentMethod,
                            description.trim(),
                            billNumber.trim(),
                            transactionDate,
                            existingTransaction?.dueDate
                        )
                    },
                    enabled = customers.isNotEmpty() || initialCustomerId != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("save_transaction_button")
                ) {
                    Text(
                        text = when {
                            existingTransaction != null -> strings.saveTransaction
                            isCredit -> strings.saveGivenCredit
                            else -> strings.saveReceivedPayment
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = transactionDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { picked ->
                            transactionDate = picked
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TransactionTypeOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    accentColor: Color,
    accentContainer: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accentContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected) accentColor else MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (selected) accentColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatBalanceLabel(
    netBalance: Double,
    currency: String,
    strings: AppStrings.Dictionary
): String {
    val absStr = "$currency%.2f".format(abs(netBalance))
    return when {
        netBalance > 0.009 -> "${strings.youWillGet} $absStr"
        netBalance < -0.009 -> "${strings.youWillGive} $absStr"
        else -> strings.settled
    }
}

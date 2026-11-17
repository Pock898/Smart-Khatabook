package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.BusinessEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.CustomerWithBalance
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCustomer
import com.example.ui.localization.AppStrings
import com.example.ui.pdf.ReportExporter
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedContainer
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import com.example.ui.theme.PendingAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddCustomerDialog(
    strings: AppStrings.Dictionary,
    currency: String,
    existingCustomer: CustomerEntity? = null,
    partyType: String = existingCustomer?.partyType ?: "CUSTOMER",
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, openingBal: Double, balType: String) -> Unit
) {
    val isSupplier = partyType.equals("SUPPLIER", ignoreCase = true)
    var name by remember { mutableStateOf(existingCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(existingCustomer?.phone ?: "") }
    var address by remember { mutableStateOf(existingCustomer?.address ?: "") }
    var openingBalStr by remember { mutableStateOf(if (existingCustomer != null && existingCustomer.openingBalance > 0) existingCustomer.openingBalance.toString() else "") }
    var balType by remember { mutableStateOf(existingCustomer?.openingBalanceType ?: if (isSupplier) "GIVE" else "GET") } // "GET" or "GIVE"

    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    existingCustomer == null && isSupplier -> strings.addSupplier
                    existingCustomer == null -> strings.addCustomer
                    isSupplier -> strings.editSupplier
                    else -> strings.editCustomer
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text(if (isSupplier) strings.supplierName else strings.customerName) },
                    isError = nameError,
                    supportingText = { if (nameError) Text(strings.validationRequired, color = CreditRed) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.mobileNumber) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(strings.address) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = strings.openingBalance,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = openingBalStr,
                    onValueChange = { openingBalStr = it },
                    label = { Text("Opening Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (openingBalStr.toDoubleOrNull() ?: 0.0 > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilterChip(
                            selected = balType == "GET",
                            onClick = { balType = "GET" },
                            label = { Text(strings.youWillGet) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CreditRedContainer,
                                selectedLabelColor = CreditRed
                            )
                        )
                        FilterChip(
                            selected = balType == "GIVE",
                            onClick = { balType = "GIVE" },
                            label = { Text(strings.youWillGive) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaymentGreenContainer,
                                selectedLabelColor = PaymentGreen
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val opening = openingBalStr.toDoubleOrNull() ?: 0.0
                    onSave(name.trim(), phone.trim(), address.trim(), opening, balType)
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun AddTransactionDialog(
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
    NewTransactionSheet(
        strings = strings,
        currency = currency,
        customers = customers,
        initialCustomerId = initialCustomerId,
        initialType = initialType,
        existingTransaction = existingTransaction,
        onAddCustomerFirst = onAddCustomerFirst,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    strings: AppStrings.Dictionary,
    currency: String,
    existingExpense: ExpenseEntity? = null,
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: String, paymentMethod: String, description: String, date: Long) -> Unit
) {
    var amountStr by remember { mutableStateOf(existingExpense?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(existingExpense?.category ?: ExpenseCategory.INVENTORY.name) }
    var paymentMethod by remember { mutableStateOf(existingExpense?.paymentMethod ?: "CASH") }
    var description by remember { mutableStateOf(existingExpense?.description ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingExpense == null) strings.addExpense else "Edit Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        amountError = (it.toDoubleOrNull() ?: 0.0) <= 0.0
                    },
                    label = { Text("${strings.amount} ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = { if (amountError) Text(strings.validationAmount, color = CreditRed) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = ExpenseCategory.entries.firstOrNull { it.name == selectedCategory }?.displayName ?: selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.expenseCategory) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ExpenseCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat.name
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.description) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount <= 0.0) {
                        amountError = true
                        return@Button
                    }
                    val date = existingExpense?.expenseDate ?: System.currentTimeMillis()
                    onSave(amount, selectedCategory, paymentMethod, description.trim(), date)
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text(strings.saveExpense)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun TransactionReceiptDialog(
    transaction: TransactionWithCustomer,
    business: BusinessEntity?,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val isCredit = transaction.type == TransactionType.CREDIT

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isCredit) CreditRedContainer else PaymentGreenContainer,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = if (isCredit) CreditRed else PaymentGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = business?.name ?: "Smart Khatabook",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                if (!business?.phone.isNullOrBlank()) {
                    Text(
                        text = "Phone: ${business?.phone}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isCredit) "CREDIT (UDHAR) RECEIPT" else "PAYMENT CONFIRMATION RECEIPT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isCredit) CreditRed else PaymentGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$currency%.2f".format(transaction.amount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = if (isCredit) CreditRed else PaymentGreen
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    ReceiptRow(label = "Customer", value = transaction.customerName)
                    ReceiptRow(label = "Date & Time", value = dateFormat.format(Date(transaction.transactionDate)))
                    if (transaction.billNumber.isNotBlank()) {
                        ReceiptRow(label = "Bill / Ref No.", value = transaction.billNumber)
                    }
                    if (transaction.description.isNotBlank()) {
                        ReceiptRow(label = "Details", value = transaction.description)
                    }
                    if (!isCredit) {
                        ReceiptRow(label = "Payment Mode", value = transaction.paymentMethod)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareText = "Receipt from ${business?.name ?: "Store"}\n" +
                                "Customer: ${transaction.customerName}\n" +
                                "Amount: $currency ${transaction.amount}\n" +
                                "Type: ${if (isCredit) "Credit (Udhar)" else "Payment Received"}\n" +
                                "Date: ${dateFormat.format(Date(transaction.transactionDate))}\n" +
                                "Details: ${transaction.description}\n" +
                                "Smart Khatabook Digital Records"
                            ReportExporter.safeShareText(
                                context = context,
                                chooserTitle = "Share Receipt",
                                subject = "Transaction Receipt - ${transaction.customerName}",
                                text = shareText
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PaymentReminderDialog(
    customer: CustomerWithBalance,
    business: BusinessEntity?,
    currency: String,
    currentLang: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTemplateIndex by remember { mutableStateOf(0) }

    val templatesEn = listOf(
        "Dear ${customer.name}, your payment of $currency%.2f is pending with ${business?.name ?: "our store"}. Kindly clear the pending balance at your earliest convenience. Thank you!".format(customer.netBalance),
        "Gentle reminder: As per our records on ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}, your outstanding balance is $currency%.2f at ${business?.name ?: "our shop"}. For any questions, please contact ${business?.phone ?: ""}. Thank you!".format(customer.netBalance),
        "Hello ${customer.name}, please clear your pending credit dues of $currency%.2f to ${business?.name ?: "our shop"}. You can pay via UPI or Cash. Thank you!".format(customer.netBalance)
    )

    val templatesHi = listOf(
        "नमस्ते ${customer.name} जी, ${business?.name ?: "हमारी दुकान"} पर आपका $currency%.2f का भुगतान बकाया है। कृपया जल्द से जल्द भुगतान करने की कृपा करें। धन्यवाद!".format(customer.netBalance),
        "तगादा संदेश: ${business?.name ?: "हमारी दुकान"} के रिकॉर्ड के अनुसार आपका बकाया शेष $currency%.2f है। कृपया इसे चुकता करें। संपर्क: ${business?.phone ?: ""}".format(customer.netBalance),
        "प्रिय ${customer.name}, कृपया अपनी उधारी राशि $currency%.2f का भुगतान नकद या यूपीआई द्वारा करें। धन्यवाद! - ${business?.name ?: "दुकानदार"}".format(customer.netBalance)
    )

    val templates = if (currentLang == "hi") templatesHi else templatesEn
    val messageText = templates[selectedTemplateIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentLang == "hi") "तगादा / तकाजा भेजें" else "Send Payment Reminder",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CreditRedContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CreditRed
                        )
                        Text(
                            text = "Pending Dues: $currency%.2f".format(customer.netBalance),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = CreditRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select Reminder Template:",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                templates.forEachIndexed { index, preview ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTemplateIndex = index }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedTemplateIndex == index,
                            onClick = { selectedTemplateIndex = index }
                        )
                        Text(
                            text = preview,
                            fontSize = 11.sp,
                            maxLines = 2,
                            modifier = Modifier.padding(start = 4.dp),
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // One-click Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ReportExporter.safeSendWhatsApp(context, customer.phone, messageText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            ReportExporter.safeSendSms(context, customer.phone, messageText)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SMS", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun BusinessProfileDialog(
    business: BusinessEntity?,
    isNew: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, owner: String, phone: String, address: String, gst: String) -> Unit
) {
    var name by remember { mutableStateOf(if (isNew) "" else business?.name ?: "") }
    var owner by remember { mutableStateOf(if (isNew) "" else business?.ownerName ?: "") }
    var phone by remember { mutableStateOf(if (isNew) "" else business?.phone ?: "") }
    var address by remember { mutableStateOf(if (isNew) "" else business?.address ?: "") }
    var gst by remember { mutableStateOf(if (isNew) "" else business?.gstNumber ?: "") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add New Business" else "Business Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = it.isBlank()
                    },
                    label = { Text("Business / Shop Name *") },
                    isError = error,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Owner Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Shop Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = gst,
                    onValueChange = { gst = it },
                    label = { Text("GSTIN / Tax ID (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = true
                        return@Button
                    }
                    onSave(name.trim(), owner.trim(), phone.trim(), address.trim(), gst.trim())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

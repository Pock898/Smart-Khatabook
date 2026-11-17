package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashbookCalculator
import com.example.data.model.CashbookEntryUi
import com.example.data.model.CashbookPaymentMode
import com.example.data.model.CashbookType
import com.example.ui.localization.AppStrings
import com.example.ui.theme.CreditRed
import com.example.ui.theme.KhatabookBlue
import com.example.ui.theme.PaymentGreen

@Composable
fun CashbookEntryScreen(
    entryType: CashbookType,
    initialMode: CashbookPaymentMode,
    existingEntry: CashbookEntryUi? = null,
    currency: String,
    strings: AppStrings.Dictionary,
    onBackClick: () -> Unit,
    onSaveEntry: (amount: Double, type: CashbookType, mode: CashbookPaymentMode, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAccent = if (entryType == CashbookType.OUT) CreditRed else PaymentGreen

    var expression by remember(existingEntry?.id) {
        mutableStateOf(
            if (existingEntry != null && existingEntry.amount > 0.0) {
                CashbookCalculator.formatNumber(existingEntry.amount)
            } else {
                ""
            }
        )
    }
    var memoryValue by remember { mutableDoubleStateOf(0.0) }
    var paymentMode by remember(existingEntry?.id, initialMode) {
        mutableStateOf(existingEntry?.paymentMode ?: initialMode)
    }
    var note by remember(existingEntry?.id) {
        mutableStateOf(existingEntry?.note ?: "")
    }

    val evaluatedAmount = remember(expression) {
        CashbookCalculator.evaluateToAmount(expression)
    }
    val formattedHeaderAmount = remember(evaluatedAmount) {
        CashbookCalculator.formatNumber(evaluatedAmount)
    }
    val canSave = evaluatedAmount > 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cashbook_entry_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = activeAccent
                    )
                }
                val titlePrefix = if (entryType == CashbookType.OUT) strings.outEntryTitle else strings.inEntryTitle
                Text(
                    text = "$titlePrefix $currency $formattedHeaderAmount",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Amount Card & Payment Mode Toggle
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currency,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeAccent
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (expression.isEmpty()) strings.enterAmountPlaceholder else expression,
                            fontSize = 24.sp,
                            fontWeight = if (expression.isEmpty()) FontWeight.Normal else FontWeight.Bold,
                            color = if (expression.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cashbook_amount_display")
                        )
                    }
                    if (expression.any { it in setOf('+', '-', '×', '÷') } && evaluatedAmount > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "= $currency $formattedHeaderAmount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = activeAccent
                        )
                    }
                }
            }

            // Payment Mode Row (Cash / Online pill switch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.paymentMethod,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        PaymentModePill(
                            label = strings.cashMode,
                            selected = paymentMode == CashbookPaymentMode.CASH,
                            accentColor = activeAccent,
                            onClick = { paymentMode = CashbookPaymentMode.CASH },
                            modifier = Modifier.testTag("cashbook_mode_cash")
                        )
                        PaymentModePill(
                            label = strings.online,
                            selected = paymentMode == CashbookPaymentMode.ONLINE,
                            accentColor = activeAccent,
                            onClick = { paymentMode = CashbookPaymentMode.ONLINE },
                            modifier = Modifier.testTag("cashbook_mode_online")
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(strings.entryNoteHint, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cashbook_note_input")
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (canSave) {
                        onSaveEntry(evaluatedAmount, entryType, paymentMode, note.trim())
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeAccent,
                    contentColor = Color.White,
                    disabledContainerColor = activeAccent.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("cashbook_save_button")
            ) {
                Text(
                    text = strings.save.uppercase(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Docked Calculator Keypad
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val functionBg = Color(0xFFD6E2F5)
                val operatorBlue = KhatabookBlue
                val keyText = Color(0xFF111827)

                // Row 1: C, M+, M-, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKey(
                        label = "C",
                        containerColor = functionBg,
                        contentColor = keyText,
                        onClick = { expression = "" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_key_clear")
                    )
                    CalcKey(
                        label = "M+",
                        containerColor = functionBg,
                        contentColor = keyText,
                        onClick = {
                            memoryValue += evaluatedAmount
                            if (expression.isNotBlank()) {
                                expression = CashbookCalculator.evaluateOrKeep(expression)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CalcKey(
                        label = "M-",
                        containerColor = functionBg,
                        contentColor = keyText,
                        onClick = {
                            memoryValue -= evaluatedAmount
                            if (expression.isNotBlank()) {
                                expression = CashbookCalculator.evaluateOrKeep(expression)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = functionBg,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expression = CashbookCalculator.backspace(expression) }
                            .testTag("calc_key_backspace")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = keyText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Row 2: 7, 8, 9, ÷, %
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKey("7", Color.White, keyText, { expression = CashbookCalculator.appendDigit(expression, '7') }, Modifier.weight(1f).testTag("calc_key_7"))
                    CalcKey("8", Color.White, keyText, { expression = CashbookCalculator.appendDigit(expression, '8') }, Modifier.weight(1f).testTag("calc_key_8"))
                    CalcKey("9", Color.White, keyText, { expression = CashbookCalculator.appendDigit(expression, '9') }, Modifier.weight(1f).testTag("calc_key_9"))
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalcKey("÷", functionBg, keyText, { expression = CashbookCalculator.appendOperator(expression, '÷') }, Modifier.weight(1f).testTag("calc_key_div"))
                        CalcKey("%", functionBg, keyText, { expression = CashbookCalculator.applyPercent(expression) }, Modifier.weight(1f).testTag("calc_key_pct"))
                    }
                }

                // Row 3: 4, 5, 6, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKey("4", Color.White, keyText, { expression = CashbookCalculator.appendDigit(expression, '4') }, Modifier.weight(1f).testTag("calc_key_4"))
                    CalcKey("5", Color.White, keyText, { expression = CashbookCalculator.appendDigit(expression, '5') }, Modifier.weight(1f).testTag("calc_key_5"))
                    CalcKey("6", Color.White, keyText, { expression = CashbookCalculator.appendDigit(expression, '6') }, Modifier.weight(1f).testTag("calc_key_6"))
                    CalcKey("×", functionBg, keyText, { expression = CashbookCalculator.appendOperator(expression, '×') }, Modifier.weight(1f).testTag("calc_key_mul"))
                }

                // Row 4: 1, 2, 3, -
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKey("1", Color.White, keyText, { expression = CashbookCalculator.appendDigit(expression, '1') }, Modifier.weight(1f).testTag("calc_key_1"))
                    CalcKey("2", Color.White, keyText, { expression = CashbookCalculator.appendDigit(expression, '2') }, Modifier.weight(1f).testTag("calc_key_2"))
                    CalcKey("3", Color.White, keyText, { expression = CashbookCalculator.appendDigit(expression, '3') }, Modifier.weight(1f).testTag("calc_key_3"))
                    CalcKey("−", operatorBlue, Color.White, { expression = CashbookCalculator.appendOperator(expression, '-') }, Modifier.weight(1f).testTag("calc_key_sub"))
                }

                // Row 5: 0, ., =, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKey("0", Color.White, keyText, { expression = CashbookCalculator.appendDigit(expression, '0') }, Modifier.weight(1f).testTag("calc_key_0"))
                    CalcKey(".", Color.White, keyText, { expression = CashbookCalculator.appendDecimal(expression) }, Modifier.weight(1f).testTag("calc_key_dot"))
                    CalcKey("=", Color.White, keyText, { expression = CashbookCalculator.evaluateOrKeep(expression) }, Modifier.weight(1f).testTag("calc_key_eq"))
                    CalcKey("+", operatorBlue, Color.White, { expression = CashbookCalculator.appendOperator(expression, '+') }, Modifier.weight(1f).testTag("calc_key_add"))
                }
            }
        }
    }
}

@Composable
private fun PaymentModePill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) accentColor else Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun CalcKey(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        shadowElevation = 1.dp,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

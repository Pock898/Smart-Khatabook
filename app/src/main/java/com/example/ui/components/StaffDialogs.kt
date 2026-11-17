package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StaffEntity
import com.example.data.model.StaffDayRecord
import com.example.data.model.StaffSalarySummary
import com.example.data.model.StaffSalaryType
import com.example.data.model.StaffWithSummary
import com.example.ui.localization.AppStrings
import com.example.ui.theme.AttendanceAbsentRed
import com.example.ui.theme.AttendanceOffPurple
import com.example.ui.theme.AttendanceOvertimeAmber
import com.example.ui.theme.AttendancePresentGreen
import com.example.ui.theme.CreditRed
import com.example.ui.theme.KhatabookBlue
import com.example.ui.theme.PaymentGreen

@Composable
fun AddStaffDialog(
    strings: AppStrings.Dictionary,
    currency: String,
    existingStaff: StaffEntity? = null,
    onDismiss: () -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    onSave: (
        name: String,
        phone: String,
        role: String,
        salaryType: String,
        salaryAmount: Double,
        overtimeRate: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf(existingStaff?.name ?: "") }
    var phone by remember { mutableStateOf(existingStaff?.phone ?: "") }
    var role by remember { mutableStateOf(existingStaff?.role ?: "Shop Assistant") }
    var salaryType by remember { mutableStateOf(existingStaff?.salaryType ?: StaffSalaryType.MONTHLY.name) }
    var salaryAmountStr by remember {
        mutableStateOf(
            if (existingStaff != null && existingStaff.salaryAmount > 0) existingStaff.salaryAmount.toString() else ""
        )
    }
    var overtimeRateStr by remember {
        mutableStateOf(
            if (existingStaff != null && existingStaff.overtimeRate > 0) existingStaff.overtimeRate.toString() else ""
        )
    }

    var nameError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && existingStaff != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteStaff, fontWeight = FontWeight.Bold, color = CreditRed) },
            text = { Text(strings.deleteStaffConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(existingStaff.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditRed),
                    modifier = Modifier.testTag("confirm_delete_staff_button")
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingStaff == null) strings.addStaff else strings.editStaff,
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
                    label = { Text(strings.staffName) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = nameError,
                    supportingText = { if (nameError) Text(strings.validationRequired, color = CreditRed) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.mobileNumber.removeSuffix(" *")) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_phone_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text(strings.staffRole) },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_role_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = strings.salaryType,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StaffSalaryType.entries.forEach { st ->
                        val selected = salaryType == st.name
                        FilterChip(
                            selected = selected,
                            onClick = { salaryType = st.name },
                            label = { Text(st.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KhatabookBlue.copy(alpha = 0.14f),
                                selectedLabelColor = KhatabookBlue
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = salaryAmountStr,
                    onValueChange = { salaryAmountStr = it },
                    label = { Text("${strings.salaryAmount} ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_salary_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = overtimeRateStr,
                    onValueChange = { overtimeRateStr = it },
                    label = { Text("${strings.overtimeRate} ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_overtime_rate_input")
                )

                if (existingStaff != null && onDelete != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CreditRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_staff_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.deleteStaff)
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
                    val salaryAmt = salaryAmountStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val otRate = overtimeRateStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    onSave(name.trim(), phone.trim(), role.trim(), salaryType, salaryAmt, otRate)
                },
                modifier = Modifier.testTag("save_staff_button")
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
fun StaffAdvanceDialog(
    staffName: String,
    dayRecord: StaffDayRecord,
    currency: String,
    strings: AppStrings.Dictionary,
    onDismiss: () -> Unit,
    onSave: (amount: Double, note: String) -> Unit
) {
    var amountStr by remember {
        mutableStateOf(if (dayRecord.advanceAmount > 0.0) dayRecord.advanceAmount.toString() else "")
    }
    var note by remember { mutableStateOf(dayRecord.advanceNote) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(strings.recordAdvance, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "$staffName • ${dayRecord.dayOfMonth} ${dayRecord.dayOfWeek} (${dayRecord.dateKey})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        amountError = it.isNotBlank() && (it.toDoubleOrNull() == null || it.toDouble() < 0.0)
                    },
                    label = { Text("${strings.advance} ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = { if (amountError) Text(strings.validationAmount, color = CreditRed) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_advance_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.advanceNote) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_advance_note_input")
                )

                if (dayRecord.advanceAmount > 0.0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { onSave(0.0, "") },
                        colors = ButtonDefaults.textButtonColors(contentColor = CreditRed),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(strings.clearAdvance)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = if (amountStr.isBlank()) 0.0 else amountStr.toDoubleOrNull()
                    if (parsed == null || parsed < 0.0) {
                        amountError = true
                        return@Button
                    }
                    onSave(parsed, note.trim())
                },
                modifier = Modifier.testTag("save_staff_advance_button")
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
fun StaffReportDialog(
    staff: StaffWithSummary,
    monthLabel: String,
    salarySummary: StaffSalarySummary,
    currency: String,
    strings: AppStrings.Dictionary,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onShareCsv: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(strings.monthlySalaryReport, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    text = "${staff.name} • $monthLabel",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReportCountChip(
                        label = strings.present,
                        count = salarySummary.presentCount,
                        color = AttendancePresentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    ReportCountChip(
                        label = strings.absent,
                        count = salarySummary.absentCount,
                        color = AttendanceAbsentRed,
                        modifier = Modifier.weight(1f)
                    )
                    ReportCountChip(
                        label = strings.overtime,
                        count = salarySummary.overtimeCount,
                        color = AttendanceOvertimeAmber,
                        modifier = Modifier.weight(1f)
                    )
                    ReportCountChip(
                        label = strings.off,
                        count = salarySummary.offCount,
                        color = AttendanceOffPurple,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryLineRow(
                            label = strings.salaryAmount,
                            value = "$currency%.2f (${staff.salaryType.lowercase().replaceFirstChar { it.uppercase() }})".format(staff.salaryAmount)
                        )
                        SummaryLineRow(
                            label = strings.grossEarned,
                            value = "$currency%.2f".format(salarySummary.earnedBasePay)
                        )
                        if (salarySummary.overtimePay > 0.0) {
                            SummaryLineRow(
                                label = strings.overtimePay,
                                value = "+ $currency%.2f".format(salarySummary.overtimePay),
                                valueColor = AttendanceOvertimeAmber
                            )
                        }
                        SummaryLineRow(
                            label = strings.totalAdvance,
                            value = "- $currency%.2f".format(salarySummary.totalAdvance),
                            valueColor = KhatabookBlue
                        )
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.netSalary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "$currency%.2f".format(salarySummary.netPayable),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (salarySummary.netPayable >= 0.0) PaymentGreen else CreditRed
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = KhatabookBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_staff_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onShareCsv,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_staff_csv_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.confirm)
            }
        }
    )
}

@Composable
private fun ReportCountChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SummaryLineRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

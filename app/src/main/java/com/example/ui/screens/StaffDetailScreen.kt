package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.StaffAttendanceCalculator
import com.example.data.model.StaffDayRecord
import com.example.data.model.StaffWithSummary
import com.example.ui.localization.AppStrings
import com.example.ui.theme.AttendanceAbsentRed
import com.example.ui.theme.AttendanceOffPurple
import com.example.ui.theme.AttendanceOvertimeAmber
import com.example.ui.theme.AttendancePresentGreen
import com.example.ui.theme.KhatabookBlue
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDetailScreen(
    staff: StaffWithSummary,
    days: List<StaffDayRecord>,
    selectedMonth: Pair<Int, Int>,
    currency: String,
    strings: AppStrings.Dictionary,
    onBackClick: () -> Unit,
    onEditStaffClick: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenReportClick: () -> Unit,
    onToggleAttendance: (dateKey: String, status: AttendanceStatus) -> Unit,
    onAdvanceClick: (StaffDayRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val monthLabel = remember(selectedMonth, strings.currentMonth) {
        StaffAttendanceCalculator.formatMonthDisplay(
            year = selectedMonth.first,
            monthZeroIndexed = selectedMonth.second,
            currentMonthLabel = strings.currentMonth
        )
    }

    val numberFmt = remember { NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KhatabookBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("staff_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = staff.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = staff.phone.ifBlank { staff.role },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEditStaffClick,
                        modifier = Modifier.testTag("edit_staff_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = strings.editStaff
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Summary + Month Selector + Open Report section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 4-Card Summary Row matching the Khatabook Attendance reference
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceSummaryTile(
                        icon = Icons.Default.CheckCircle,
                        value = staff.presentCount.toString(),
                        label = strings.present,
                        accentColor = AttendancePresentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceSummaryTile(
                        icon = Icons.Default.Cancel,
                        value = staff.absentCount.toString(),
                        label = strings.absent,
                        accentColor = AttendanceAbsentRed,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceSummaryTile(
                        icon = Icons.Default.Schedule,
                        value = staff.overtimeCount.toString(),
                        label = strings.overtime,
                        accentColor = AttendanceOvertimeAmber,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceSummaryTile(
                        icon = Icons.Default.AccountBalanceWallet,
                        value = "$currency${numberFmt.format(staff.totalAdvance)}",
                        label = strings.advance,
                        accentColor = KhatabookBlue,
                        modifier = Modifier.weight(1.15f)
                    )
                }

                // Month Navigator Card (< Current Month >)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onPreviousMonth() }
                                .testTag("staff_prev_month_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Month",
                                    tint = KhatabookBlue
                                )
                            }
                        }

                        Text(
                            text = monthLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = KhatabookBlue
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onNextMonth() }
                                .testTag("staff_next_month_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next Month",
                                    tint = KhatabookBlue
                                )
                            }
                        }
                    }
                }

                // Full-width Open Report Primary CTA
                Button(
                    onClick = onOpenReportClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KhatabookBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("staff_open_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.openReport,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Table Card Container (Date | Attendance | Advance)
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.dateLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = KhatabookBlue,
                            modifier = Modifier.weight(0.22f)
                        )
                        Text(
                            text = strings.attendanceLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = KhatabookBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.54f)
                        )
                        Text(
                            text = strings.advance,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = KhatabookBlue,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.24f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(days, key = { it.dateKey }) { day ->
                            StaffDailyAttendanceRow(
                                day = day,
                                currency = currency,
                                onToggleStatus = { status -> onToggleAttendance(day.dateKey, status) },
                                onAdvanceClick = { onAdvanceClick(day) }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceSummaryTile(
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = accentColor,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StaffDailyAttendanceRow(
    day: StaffDayRecord,
    currency: String,
    onToggleStatus: (AttendanceStatus) -> Unit,
    onAdvanceClick: () -> Unit
) {
    val rowBg = if (day.isToday) {
        KhatabookBlue.copy(alpha = 0.06f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date Column (01 Mon)
        Column(
            modifier = Modifier.weight(0.20f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = day.dayOfMonth,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (day.isToday) KhatabookBlue else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = day.dayOfWeek,
                fontSize = 11.sp,
                color = if (day.isToday) KhatabookBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // One-tap Circular Status Toggles (P, A, OT, Off)
        Row(
            modifier = Modifier.weight(0.56f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttendanceCircleButton(
                label = "P",
                color = AttendancePresentGreen,
                selected = day.status == AttendanceStatus.PRESENT,
                onClick = { onToggleStatus(AttendanceStatus.PRESENT) },
                testTag = "att_p_${day.dateKey}"
            )
            AttendanceCircleButton(
                label = "A",
                color = AttendanceAbsentRed,
                selected = day.status == AttendanceStatus.ABSENT,
                onClick = { onToggleStatus(AttendanceStatus.ABSENT) },
                testTag = "att_a_${day.dateKey}"
            )
            AttendanceCircleButton(
                label = "OT",
                color = AttendanceOvertimeAmber,
                selected = day.status == AttendanceStatus.OVERTIME,
                onClick = { onToggleStatus(AttendanceStatus.OVERTIME) },
                testTag = "att_ot_${day.dateKey}"
            )
            AttendanceCircleButton(
                label = "Off",
                color = AttendanceOffPurple,
                selected = day.status == AttendanceStatus.OFF,
                onClick = { onToggleStatus(AttendanceStatus.OFF) },
                testTag = "att_off_${day.dateKey}"
            )
        }

        // Advance Column
        Box(
            modifier = Modifier
                .weight(0.24f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onAdvanceClick() }
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .testTag("staff_advance_${day.dateKey}"),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = if (day.advanceAmount > 0.0) {
                    "$currency${"%.2f".format(day.advanceAmount)}"
                } else {
                    "$currency" + "0.00"
                },
                fontSize = 13.sp,
                fontWeight = if (day.advanceAmount > 0.0) FontWeight.Bold else FontWeight.Medium,
                color = if (day.advanceAmount > 0.0) KhatabookBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AttendanceCircleButton(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) color else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, color),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else color
                )
            }
        }
    }
}

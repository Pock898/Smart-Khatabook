package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.StaffAttendanceCalculator
import com.example.data.model.StaffWithSummary
import com.example.ui.components.EmptyStateView
import com.example.ui.localization.AppStrings
import com.example.ui.theme.AttendanceAbsentRed
import com.example.ui.theme.AttendanceOffPurple
import com.example.ui.theme.AttendanceOvertimeAmber
import com.example.ui.theme.AttendancePresentGreen
import com.example.ui.theme.KhatabookBlue
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StaffScreen(
    staffList: List<StaffWithSummary>,
    selectedMonth: Pair<Int, Int>,
    currency: String,
    strings: AppStrings.Dictionary,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onStaffClick: (StaffWithSummary) -> Unit,
    onAddStaffClick: () -> Unit,
    onQuickMarkToday: (staffId: Long, status: AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val monthLabel = remember(selectedMonth, strings.currentMonth) {
        StaffAttendanceCalculator.formatMonthDisplay(
            year = selectedMonth.first,
            monthZeroIndexed = selectedMonth.second,
            currentMonthLabel = strings.currentMonth
        )
    }

    val filteredStaff = remember(staffList, searchQuery) {
        if (searchQuery.isBlank()) staffList
        else staffList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true) ||
                it.role.contains(searchQuery, ignoreCase = true)
        }
    }

    val presentToday = staffList.count {
        it.todayStatus == AttendanceStatus.PRESENT || it.todayStatus == AttendanceStatus.OVERTIME
    }
    val absentToday = staffList.count { it.todayStatus == AttendanceStatus.ABSENT }
    val totalMonthlyAdvance = staffList.sumOf { it.totalAdvance }
    val numberFmt = remember { NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Monthly Staff Summary Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StaffStatPill(
                                label = strings.staff,
                                value = staffList.size.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StaffStatPill(
                                label = "${strings.today} ${strings.present}",
                                value = presentToday.toString(),
                                color = AttendancePresentGreen,
                                modifier = Modifier.weight(1f)
                            )
                            StaffStatPill(
                                label = "${strings.today} ${strings.absent}",
                                value = absentToday.toString(),
                                color = AttendanceAbsentRed,
                                modifier = Modifier.weight(1f)
                            )
                            StaffStatPill(
                                label = strings.advance,
                                value = "$currency${numberFmt.format(totalMonthlyAdvance)}",
                                color = KhatabookBlue,
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onPreviousMonth,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("staff_list_prev_month")
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = KhatabookBlue)
                            }
                            Text(
                                text = monthLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = KhatabookBlue
                            )
                            IconButton(
                                onClick = onNextMonth,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("staff_list_next_month")
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = KhatabookBlue)
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchStaff, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_staff_input")
                )
            }

            if (filteredStaff.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Badge,
                        title = if (searchQuery.isNotEmpty()) "No matching staff" else strings.noStaffYet,
                        message = if (searchQuery.isNotEmpty()) "Try a different name, phone, or role." else strings.addFirstStaff,
                        buttonText = if (searchQuery.isEmpty()) strings.addStaff else null,
                        onButtonClick = onAddStaffClick
                    )
                }
            } else {
                items(filteredStaff, key = { it.id }) { staff ->
                    StaffMemberCard(
                        staff = staff,
                        currency = currency,
                        strings = strings,
                        onClick = { onStaffClick(staff) },
                        onQuickMarkToday = { status -> onQuickMarkToday(staff.id, status) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddStaffClick,
            containerColor = KhatabookBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_staff_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = strings.addStaff)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.addStaff, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StaffStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = color,
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

@Composable
private fun StaffMemberCard(
    staff: StaffWithSummary,
    currency: String,
    strings: AppStrings.Dictionary,
    onClick: () -> Unit,
    onQuickMarkToday: (AttendanceStatus) -> Unit
) {
    val initials = staff.name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifBlank { "S" }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("staff_card_${staff.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = KhatabookBlue.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Bold,
                            color = KhatabookBlue,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = staff.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(staff.role)
                            if (staff.phone.isNotBlank()) {
                                append(" • ${staff.phone}")
                            }
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currency${"%.0f".format(staff.salaryAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = KhatabookBlue
                    )
                    Text(
                        text = if (staff.salaryType == "DAILY") "/day" else "/month",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Monthly mini metrics + One-tap Today buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "P:${staff.presentCount}  A:${staff.absentCount}  OT:${staff.overtimeCount}  Adv:$currency${"%.0f".format(staff.totalAdvance)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strings.today}:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAttendanceCircle(
                        label = "P",
                        color = AttendancePresentGreen,
                        selected = staff.todayStatus == AttendanceStatus.PRESENT,
                        onClick = { onQuickMarkToday(AttendanceStatus.PRESENT) },
                        testTag = "quick_att_p_${staff.id}"
                    )
                    QuickAttendanceCircle(
                        label = "A",
                        color = AttendanceAbsentRed,
                        selected = staff.todayStatus == AttendanceStatus.ABSENT,
                        onClick = { onQuickMarkToday(AttendanceStatus.ABSENT) },
                        testTag = "quick_att_a_${staff.id}"
                    )
                    QuickAttendanceCircle(
                        label = "OT",
                        color = AttendanceOvertimeAmber,
                        selected = staff.todayStatus == AttendanceStatus.OVERTIME,
                        onClick = { onQuickMarkToday(AttendanceStatus.OVERTIME) },
                        testTag = "quick_att_ot_${staff.id}"
                    )
                    QuickAttendanceCircle(
                        label = "Off",
                        color = AttendanceOffPurple,
                        selected = staff.todayStatus == AttendanceStatus.OFF,
                        onClick = { onQuickMarkToday(AttendanceStatus.OFF) },
                        testTag = "quick_att_off_${staff.id}"
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAttendanceCircle(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) color else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, color),
            modifier = Modifier.size(38.dp)
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

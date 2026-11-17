package com.example.data.model

import com.example.data.local.StaffAttendanceEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class StaffSalarySummary(
    val daysInMonth: Int,
    val presentCount: Int,
    val absentCount: Int,
    val overtimeCount: Int,
    val offCount: Int,
    val earnedBasePay: Double,
    val overtimePay: Double,
    val grossEarned: Double,
    val totalAdvance: Double,
    val netPayable: Double
)

object StaffAttendanceCalculator {

    fun formatMonthPrefix(year: Int, monthZeroIndexed: Int): String {
        return "%04d-%02d".format(Locale.US, year, monthZeroIndexed + 1)
    }

    fun todayDateKey(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Calendar.getInstance().time)
    }

    fun formatMonthDisplay(year: Int, monthZeroIndexed: Int, currentMonthLabel: String): String {
        val now = Calendar.getInstance()
        if (now.get(Calendar.YEAR) == year && now.get(Calendar.MONTH) == monthZeroIndexed) {
            return currentMonthLabel
        }
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun buildMonthDayRecords(
        year: Int,
        monthZeroIndexed: Int,
        entries: List<StaffAttendanceEntity>
    ): List<StaffDayRecord> {
        val byDate = entries.associateBy { it.dateKey }
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayKey = todayDateKey()
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dowFmt = SimpleDateFormat("EEE", Locale.getDefault())

        return (1..maxDay).map { day ->
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dateKey = keyFmt.format(cal.time)
            val existing = byDate[dateKey]
            StaffDayRecord(
                dateKey = dateKey,
                dayOfMonth = "%02d".format(Locale.US, day),
                dayOfWeek = dowFmt.format(cal.time),
                status = AttendanceStatus.from(existing?.status),
                advanceAmount = existing?.advanceAmount ?: 0.0,
                advanceNote = existing?.advanceNote ?: "",
                isToday = dateKey == todayKey
            )
        }
    }

    fun calculateSalarySummary(
        staff: StaffWithSummary,
        days: List<StaffDayRecord>
    ): StaffSalarySummary {
        val daysInMonth = days.size.coerceAtLeast(28)
        val present = days.count { it.status == AttendanceStatus.PRESENT }
        val absent = days.count { it.status == AttendanceStatus.ABSENT }
        val overtime = days.count { it.status == AttendanceStatus.OVERTIME }
        val off = days.count { it.status == AttendanceStatus.OFF }
        val totalAdvance = days.sumOf { it.advanceAmount }

        val earnedBase = if (staff.salaryType.equals(StaffSalaryType.DAILY.name, ignoreCase = true)) {
            (present + overtime) * staff.salaryAmount
        } else {
            val paidDays = present + overtime + off
            if (paidDays == 0) 0.0 else (paidDays.toDouble() / daysInMonth.toDouble()) * staff.salaryAmount
        }
        val overtimePay = overtime * staff.overtimeRate
        val gross = earnedBase + overtimePay
        val net = gross - totalAdvance

        return StaffSalarySummary(
            daysInMonth = daysInMonth,
            presentCount = present,
            absentCount = absent,
            overtimeCount = overtime,
            offCount = off,
            earnedBasePay = earnedBase,
            overtimePay = overtimePay,
            grossEarned = gross,
            totalAdvance = totalAdvance,
            netPayable = net
        )
    }
}

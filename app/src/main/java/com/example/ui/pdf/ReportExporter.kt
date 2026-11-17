package com.example.ui.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.BusinessEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.CustomerWithBalance
import com.example.data.model.StaffDayRecord
import com.example.data.model.StaffSalarySummary
import com.example.data.model.StaffWithSummary
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private fun safeFileName(raw: String): String {
        return raw.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "Report" }
    }

    private fun csvCell(raw: String): String {
        return "\"" + raw.replace("\"", "\"\"") + "\""
    }

    fun safeStartActivity(context: Context, intent: Intent, fallbackMessage: String) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, fallbackMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun safeDialPhone(context: Context, phone: String) {
        val cleanPhone = phone.replace(Regex("[^0-9+*#]"), "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "No valid phone number available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
        safeStartActivity(context, intent, "No phone dialer app found on this device")
    }

    fun safeShareText(context: Context, chooserTitle: String, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            if (subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, chooserTitle)
        safeStartActivity(context, chooser, "No app available to share this content")
    }

    fun safeSendWhatsApp(context: Context, phone: String, messageText: String) {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isNotBlank()) {
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Fall back cleanly to general text share chooser below
            }
        }
        safeShareText(context, "Share Payment Reminder", "Payment Reminder", messageText)
    }

    fun safeSendSms(context: Context, phone: String, messageText: String) {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isNotBlank()) {
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanPhone")).apply {
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(smsIntent)
                return
            } catch (_: Exception) {
                // Fall back to share sheet if no SMS client is installed (e.g. tablets/emulators)
            }
        }
        safeShareText(context, "Send Reminder", "Payment Reminder", messageText)
    }

    fun generateAndShareCustomerPdf(
        context: Context,
        business: BusinessEntity?,
        customer: CustomerWithBalance,
        transactions: List<TransactionEntity>,
        currency: String
    ) {
        val doc = PdfDocument()
        try {
            val paint = Paint().apply { isAntiAlias = true }
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val shortDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            val sortedTxns = transactions.sortedBy { it.transactionDate }
            val rowsPerFirstPage = 27
            val rowsPerNextPage = 34
            val chunks = if (sortedTxns.isEmpty()) {
                listOf(emptyList())
            } else {
                val list = mutableListOf<List<TransactionEntity>>()
                list.add(sortedTxns.take(rowsPerFirstPage))
                var idx = rowsPerFirstPage
                while (idx < sortedTxns.size) {
                    list.add(sortedTxns.subList(idx, minOf(idx + rowsPerNextPage, sortedTxns.size)))
                    idx += rowsPerNextPage
                }
                list
            }

            val balText = when {
                customer.netBalance > 0.009 -> "Net Dues (You'll Get): $currency%.2f".format(customer.netBalance)
                customer.netBalance < -0.009 -> "Advance (You'll Give): $currency%.2f".format(-customer.netBalance)
                else -> "Balance: Settled ($currency 0.00)"
            }

            chunks.forEachIndexed { pageIndex, pageTxns ->
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                var y: Float
                if (pageIndex == 0) {
                    paint.color = Color.rgb(30, 58, 138)
                    canvas.drawRect(0f, 0f, 595f, 90f, paint)

                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 22f
                    canvas.drawText(business?.name ?: "Smart Khatabook", 30f, 42f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 10f
                    val phone = if (!business?.phone.isNullOrBlank()) "Phone: ${business?.phone}" else ""
                    val gst = if (!business?.gstNumber.isNullOrBlank()) " | GSTIN: ${business?.gstNumber}" else ""
                    canvas.drawText("$phone$gst", 30f, 62f, paint)
                    canvas.drawText("CUSTOMER ACCOUNT STATEMENT", 30f, 78f, paint)

                    y = 120f
                    paint.color = Color.rgb(243, 244, 246)
                    canvas.drawRoundRect(30f, y, 565f, y + 65f, 8f, 8f, paint)

                    paint.color = Color.rgb(31, 41, 55)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 14f
                    canvas.drawText("Customer: ${customer.name}", 45f, y + 25f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 11f
                    canvas.drawText("Phone: ${customer.phone.ifBlank { "N/A" }}", 45f, y + 45f, paint)

                    paint.color = if (customer.netBalance > 0.009) Color.rgb(220, 38, 38) else Color.rgb(22, 163, 74)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 13f
                    canvas.drawText(balText, 305f, y + 35f, paint)
                    y += 95f
                } else {
                    paint.color = Color.rgb(30, 58, 138)
                    canvas.drawRect(0f, 0f, 595f, 46f, paint)
                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 13f
                    canvas.drawText("${customer.name} — Statement (Page ${pageIndex + 1})", 30f, 29f, paint)
                    y = 66f
                }

                // Table Header
                paint.color = Color.rgb(229, 231, 235)
                canvas.drawRect(30f, y, 565f, y + 26f, paint)

                paint.color = Color.rgb(17, 24, 39)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                canvas.drawText("DATE", 40f, y + 17f, paint)
                canvas.drawText("DETAILS / BILL", 120f, y + 17f, paint)
                canvas.drawText("GAVE (CREDIT)", 320f, y + 17f, paint)
                canvas.drawText("GOT (PAYMENT)", 430f, y + 17f, paint)

                y += 26f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                if (pageTxns.isEmpty()) {
                    y += 24f
                    paint.color = Color.GRAY
                    canvas.drawText("No ledger transactions recorded yet.", 40f, y, paint)
                } else {
                    for (t in pageTxns) {
                        y += 20f
                        paint.color = Color.rgb(55, 65, 81)
                        canvas.drawText(shortDate.format(Date(t.transactionDate)), 40f, y, paint)

                        val desc = (t.description.ifBlank { "Transaction" } + if (t.billNumber.isNotBlank()) " (#${t.billNumber})" else "")
                        val truncatedDesc = if (desc.length > 26) desc.substring(0, 23) + "..." else desc
                        canvas.drawText(truncatedDesc, 120f, y, paint)

                        if (t.type == TransactionType.CREDIT.value) {
                            paint.color = Color.rgb(220, 38, 38)
                            canvas.drawText("$currency%.2f".format(t.amount), 320f, y, paint)
                            paint.color = Color.GRAY
                            canvas.drawText("-", 440f, y, paint)
                        } else {
                            paint.color = Color.GRAY
                            canvas.drawText("-", 330f, y, paint)
                            paint.color = Color.rgb(22, 163, 74)
                            canvas.drawText("$currency%.2f".format(t.amount), 430f, y, paint)
                        }

                        paint.color = Color.rgb(243, 244, 246)
                        canvas.drawLine(30f, y + 5f, 565f, y + 5f, paint)
                    }
                }

                paint.color = Color.rgb(107, 114, 128)
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText(
                    "Generated on ${dateFormat.format(Date())} via Smart Khatabook (Page ${pageIndex + 1}/${chunks.size})",
                    30f,
                    820f,
                    paint
                )
                doc.finishPage(page)
            }

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "Statement_${safeFileName(customer.name)}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Account Statement - ${customer.name}")
                putExtra(Intent.EXTRA_TEXT, "Hello ${customer.name}, please find attached your ledger account statement from ${business?.name ?: "our store"}. $balText.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            safeStartActivity(
                context,
                Intent.createChooser(intent, "Share Statement PDF"),
                "No PDF viewer or share app available"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to export PDF statement", Toast.LENGTH_SHORT).show()
        } finally {
            doc.close()
        }
    }

    fun shareStatementCsv(
        context: Context,
        business: BusinessEntity?,
        customer: CustomerWithBalance,
        transactions: List<TransactionEntity>,
        currency: String
    ) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val csv = StringBuilder()
        csv.append("Smart Khatabook - Customer Statement\n")
        csv.append("Business:,${csvCell(business?.name ?: "My Business")}\n")
        csv.append("Customer:,${csvCell(customer.name)}\n")
        csv.append("Phone:,${csvCell(customer.phone)}\n")
        csv.append("Net Balance:,${csvCell("$currency ${customer.netBalance}")}\n\n")
        csv.append("Date,Description,Bill No,Credit ($currency),Payment ($currency)\n")

        for (t in transactions.sortedBy { it.transactionDate }) {
            val isCredit = t.type == TransactionType.CREDIT.value
            val creditAmt = if (isCredit) t.amount.toString() else "0.0"
            val paymentAmt = if (!isCredit) t.amount.toString() else "0.0"
            csv.append("${csvCell(dateFormat.format(Date(t.transactionDate)))},${csvCell(t.description)},${csvCell(t.billNumber)},$creditAmt,$paymentAmt\n")
        }

        safeShareText(
            context = context,
            chooserTitle = "Share Statement CSV",
            subject = "Statement - ${customer.name}",
            text = csv.toString()
        )
    }

    fun generateAndShareBusinessSummaryPdf(
        context: Context,
        business: BusinessEntity?,
        period: String,
        credit: Double,
        collection: Double,
        expenses: Double,
        cashflow: Double,
        outstanding: Double,
        currency: String
    ) {
        val doc = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply { isAntiAlias = true }
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

            paint.color = Color.rgb(30, 58, 138)
            canvas.drawRect(0f, 0f, 595f, 92f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 22f
            canvas.drawText(business?.name ?: "Smart Khatabook", 30f, 42f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 11f
            canvas.drawText("BUSINESS PERFORMANCE REPORT — $period", 30f, 68f, paint)

            val rows = listOf(
                "Total Credit Given (Udhar)" to "$currency%.2f".format(credit),
                "Total Payments Collected (Jama)" to "$currency%.2f".format(collection),
                "Total Business Expenses" to "$currency%.2f".format(expenses),
                "Net Cash Flow (Collected - Expenses)" to "$currency%.2f".format(cashflow),
                "Total Outstanding Customer Dues" to "$currency%.2f".format(outstanding)
            )

            var y = 136f
            rows.forEach { (label, value) ->
                paint.color = Color.rgb(243, 244, 246)
                canvas.drawRoundRect(30f, y, 565f, y + 48f, 8f, 8f, paint)

                paint.color = Color.rgb(55, 65, 81)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 13f
                canvas.drawText(label, 46f, y + 29f, paint)

                paint.color = Color.rgb(17, 24, 39)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 14f
                canvas.drawText(value, 410f, y + 29f, paint)
                y += 62f
            }

            paint.color = Color.rgb(107, 114, 128)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Generated on ${dateFormat.format(Date())} via Smart Khatabook", 30f, 820f, paint)

            doc.finishPage(page)

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "Business_Report_${safeFileName(period)}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Business Report - ${business?.name ?: "Store"}")
                putExtra(Intent.EXTRA_TEXT, "Business performance report ($period) for ${business?.name ?: "Store"}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            safeStartActivity(
                context,
                Intent.createChooser(intent, "Share Business Report PDF"),
                "No PDF viewer or share app available"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to export PDF report", Toast.LENGTH_SHORT).show()
        } finally {
            doc.close()
        }
    }

    fun shareBusinessSummaryCsv(
        context: Context,
        business: BusinessEntity?,
        period: String,
        credit: Double,
        collection: Double,
        expenses: Double,
        cashflow: Double,
        outstanding: Double,
        currency: String
    ) {
        val csv = "Metric,Value\n" +
            "Business,${csvCell(business?.name ?: "Store")}\n" +
            "Period,${csvCell(period)}\n" +
            "Total Credit Given,${csvCell("$currency%.2f".format(credit))}\n" +
            "Total Payments Collected,${csvCell("$currency%.2f".format(collection))}\n" +
            "Total Expenses,${csvCell("$currency%.2f".format(expenses))}\n" +
            "Net Cash Flow,${csvCell("$currency%.2f".format(cashflow))}\n" +
            "Total Customer Outstanding,${csvCell("$currency%.2f".format(outstanding))}\n"

        safeShareText(
            context = context,
            chooserTitle = "Share Report CSV",
            subject = "Business Summary CSV",
            text = csv
        )
    }

    fun generateAndShareStaffReportPdf(
        context: Context,
        business: BusinessEntity?,
        staff: StaffWithSummary,
        monthLabel: String,
        days: List<StaffDayRecord>,
        salarySummary: StaffSalarySummary,
        currency: String
    ) {
        val doc = PdfDocument()
        try {
            val paint = Paint().apply { isAntiAlias = true }
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

            val rowsPerPage = 32
            val chunks = if (days.isEmpty()) listOf(emptyList()) else days.chunked(rowsPerPage)

            chunks.forEachIndexed { pageIndex, pageDays ->
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                var y: Float
                if (pageIndex == 0) {
                    paint.color = Color.rgb(21, 88, 176)
                    canvas.drawRect(0f, 0f, 595f, 88f, paint)

                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 20f
                    canvas.drawText(business?.name ?: "Smart Khatabook", 30f, 38f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 11f
                    canvas.drawText("STAFF ATTENDANCE & SALARY REPORT - $monthLabel", 30f, 62f, paint)

                    y = 104f
                    paint.color = Color.rgb(243, 244, 246)
                    canvas.drawRoundRect(30f, y, 565f, y + 86f, 8f, 8f, paint)

                    paint.color = Color.rgb(17, 24, 39)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 14f
                    canvas.drawText("${staff.name} (${staff.role})", 45f, y + 24f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 10f
                    canvas.drawText("Phone: ${staff.phone.ifBlank { "N/A" }} | Basis: ${staff.salaryType} ($currency%.2f)".format(staff.salaryAmount), 45f, y + 42f, paint)
                    canvas.drawText(
                        "Present: ${salarySummary.presentCount}  |  Absent: ${salarySummary.absentCount}  |  OT: ${salarySummary.overtimeCount}  |  Off: ${salarySummary.offCount}",
                        45f,
                        y + 62f,
                        paint
                    )

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 12f
                    paint.color = Color.rgb(21, 88, 176)
                    canvas.drawText("Advance: $currency%.2f".format(salarySummary.totalAdvance), 385f, y + 26f, paint)
                    paint.color = Color.rgb(22, 163, 74)
                    canvas.drawText("Net Payable: $currency%.2f".format(salarySummary.netPayable), 385f, y + 48f, paint)
                    paint.color = Color.rgb(75, 85, 99)
                    paint.textSize = 10f
                    canvas.drawText("Gross Earned: $currency%.2f".format(salarySummary.grossEarned), 385f, y + 66f, paint)
                    y += 102f
                } else {
                    paint.color = Color.rgb(21, 88, 176)
                    canvas.drawRect(0f, 0f, 595f, 46f, paint)
                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 13f
                    canvas.drawText("${staff.name} — $monthLabel (Page ${pageIndex + 1})", 30f, 29f, paint)
                    y = 64f
                }

                paint.color = Color.rgb(229, 231, 235)
                canvas.drawRect(30f, y, 565f, y + 22f, paint)

                paint.color = Color.rgb(17, 24, 39)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                canvas.drawText("DATE", 42f, y + 15f, paint)
                canvas.drawText("STATUS", 155f, y + 15f, paint)
                canvas.drawText("ADVANCE", 265f, y + 15f, paint)
                canvas.drawText("NOTE", 375f, y + 15f, paint)

                y += 24f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 9.5f

                for (day in pageDays) {
                    y += 17f
                    paint.color = Color.rgb(55, 65, 81)
                    canvas.drawText("${day.dayOfMonth} ${day.dayOfWeek}", 42f, y, paint)

                    val statusLabel = when (day.status) {
                        AttendanceStatus.PRESENT -> "Present (P)"
                        AttendanceStatus.ABSENT -> "Absent (A)"
                        AttendanceStatus.OVERTIME -> "Overtime (OT)"
                        AttendanceStatus.OFF -> "Off"
                        AttendanceStatus.NONE -> "-"
                    }
                    paint.color = when (day.status) {
                        AttendanceStatus.PRESENT -> Color.rgb(22, 163, 74)
                        AttendanceStatus.ABSENT -> Color.rgb(220, 38, 38)
                        AttendanceStatus.OVERTIME -> Color.rgb(217, 119, 6)
                        AttendanceStatus.OFF -> Color.rgb(147, 51, 234)
                        AttendanceStatus.NONE -> Color.GRAY
                    }
                    canvas.drawText(statusLabel, 155f, y, paint)

                    paint.color = if (day.advanceAmount > 0.0) Color.rgb(21, 88, 176) else Color.GRAY
                    canvas.drawText(if (day.advanceAmount > 0.0) "$currency%.2f".format(day.advanceAmount) else "-", 265f, y, paint)

                    paint.color = Color.rgb(107, 114, 128)
                    val note = if (day.advanceNote.length > 26) day.advanceNote.substring(0, 23) + "..." else day.advanceNote
                    canvas.drawText(note.ifBlank { "-" }, 375f, y, paint)

                    paint.color = Color.rgb(243, 244, 246)
                    canvas.drawLine(30f, y + 4f, 565f, y + 4f, paint)
                }

                paint.color = Color.rgb(107, 114, 128)
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText(
                    "Generated on ${dateFormat.format(Date())} via Smart Khatabook (Page ${pageIndex + 1}/${chunks.size})",
                    30f,
                    822f,
                    paint
                )

                doc.finishPage(page)
            }

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "Staff_Report_${safeFileName(staff.name)}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Staff Report - ${staff.name} ($monthLabel)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attendance & Salary Report for ${staff.name} ($monthLabel). Net Payable: $currency%.2f".format(salarySummary.netPayable)
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            safeStartActivity(
                context,
                Intent.createChooser(intent, "Share Staff Report PDF"),
                "No PDF viewer or share app available"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to export staff PDF report", Toast.LENGTH_SHORT).show()
        } finally {
            doc.close()
        }
    }

    fun shareStaffReportCsv(
        context: Context,
        business: BusinessEntity?,
        staff: StaffWithSummary,
        monthLabel: String,
        days: List<StaffDayRecord>,
        salarySummary: StaffSalarySummary,
        currency: String
    ) {
        val csv = StringBuilder()
        csv.append("Smart Khatabook - Staff Attendance & Salary Report\n")
        csv.append("Business:,${csvCell(business?.name ?: "My Business")}\n")
        csv.append("Staff:,${csvCell(staff.name)},Role:,${csvCell(staff.role)}\n")
        csv.append("Month:,${csvCell(monthLabel)}\n")
        csv.append("Present,${salarySummary.presentCount},Absent,${salarySummary.absentCount},Overtime,${salarySummary.overtimeCount},Off,${salarySummary.offCount}\n")
        csv.append("Gross Earned,$currency ${"%.2f".format(salarySummary.grossEarned)},Total Advance,$currency ${"%.2f".format(salarySummary.totalAdvance)},Net Payable,$currency ${"%.2f".format(salarySummary.netPayable)}\n\n")
        csv.append("Date,Day,Status,Advance ($currency),Note\n")

        for (day in days) {
            csv.append("${csvCell(day.dateKey)},${csvCell(day.dayOfWeek)},${csvCell(day.status.code)},${day.advanceAmount},${csvCell(day.advanceNote)}\n")
        }

        safeShareText(
            context = context,
            chooserTitle = "Share Staff Report CSV",
            subject = "Staff Report - ${staff.name} ($monthLabel)",
            text = csv.toString()
        )
    }

    fun generateAndShareCashbookPdf(
        context: Context,
        business: BusinessEntity?,
        summary: com.example.data.model.CashbookSummary,
        entries: List<com.example.data.local.CashbookEntity>,
        currency: String
    ) {
        val doc = PdfDocument()
        try {
            val paint = Paint().apply { isAntiAlias = true }
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val shortDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sortedEntries = entries.sortedByDescending { it.entryDate }
            val chunks = if (sortedEntries.isEmpty()) listOf(emptyList()) else sortedEntries.chunked(28)

            chunks.forEachIndexed { pageIndex, pageEntries ->
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                var y: Float
                if (pageIndex == 0) {
                    paint.color = Color.rgb(21, 88, 176)
                    canvas.drawRect(0f, 0f, 595f, 88f, paint)

                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 20f
                    canvas.drawText(business?.name ?: "Smart Khatabook", 30f, 38f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 11f
                    canvas.drawText("DAILY CASHBOOK REPORT", 30f, 64f, paint)

                    y = 106f
                    paint.color = Color.rgb(243, 244, 246)
                    canvas.drawRoundRect(30f, y, 565f, y + 66f, 8f, 8f, paint)

                    paint.color = Color.rgb(17, 24, 39)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 13f
                    canvas.drawText("Total Balance: $currency%.2f".format(summary.totalBalance), 45f, y + 25f, paint)
                    canvas.drawText("Today's Balance: $currency%.2f".format(summary.todayBalance), 320f, y + 25f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 10.5f
                    canvas.drawText("Cash in Hand: $currency%.2f  |  Online: $currency%.2f".format(summary.totalCashInHand, summary.totalOnline), 45f, y + 48f, paint)
                    canvas.drawText("Today IN: $currency%.2f  |  OUT: $currency%.2f".format(summary.todayIn, summary.todayOut), 320f, y + 48f, paint)
                    y += 84f
                } else {
                    paint.color = Color.rgb(21, 88, 176)
                    canvas.drawRect(0f, 0f, 595f, 46f, paint)
                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 13f
                    canvas.drawText("${business?.name ?: "Store"} — Cashbook Report (Page ${pageIndex + 1})", 30f, 29f, paint)
                    y = 66f
                }

                paint.color = Color.rgb(229, 231, 235)
                canvas.drawRect(30f, y, 565f, y + 24f, paint)
                paint.color = Color.rgb(17, 24, 39)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                canvas.drawText("DATE", 40f, y + 16f, paint)
                canvas.drawText("MODE", 130f, y + 16f, paint)
                canvas.drawText("NOTE", 200f, y + 16f, paint)
                canvas.drawText("OUT", 385f, y + 16f, paint)
                canvas.drawText("IN", 480f, y + 16f, paint)
                y += 26f

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                if (pageEntries.isEmpty()) {
                    y += 22f
                    paint.color = Color.GRAY
                    canvas.drawText("No cashbook entries recorded yet.", 40f, y, paint)
                } else {
                    for (entry in pageEntries) {
                        y += 19f
                        paint.color = Color.rgb(55, 65, 81)
                        canvas.drawText(shortDate.format(Date(entry.entryDate)), 40f, y, paint)
                        canvas.drawText(entry.paymentMode, 130f, y, paint)
                        val note = entry.note.ifBlank { "-" }.let { if (it.length > 26) it.take(23) + "..." else it }
                        canvas.drawText(note, 200f, y, paint)

                        if (entry.type.equals("OUT", ignoreCase = true)) {
                            paint.color = Color.rgb(220, 38, 38)
                            canvas.drawText("$currency%.2f".format(entry.amount), 385f, y, paint)
                            paint.color = Color.GRAY
                            canvas.drawText("-", 480f, y, paint)
                        } else {
                            paint.color = Color.GRAY
                            canvas.drawText("-", 385f, y, paint)
                            paint.color = Color.rgb(22, 163, 74)
                            canvas.drawText("$currency%.2f".format(entry.amount), 480f, y, paint)
                        }
                        paint.color = Color.rgb(243, 244, 246)
                        canvas.drawLine(30f, y + 4f, 565f, y + 4f, paint)
                    }
                }

                paint.color = Color.rgb(107, 114, 128)
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText(
                    "Generated on ${dateFormat.format(Date())} via Smart Khatabook (Page ${pageIndex + 1}/${chunks.size})",
                    30f,
                    820f,
                    paint
                )
                doc.finishPage(page)
            }

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "Cashbook_Report_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cashbook Report - ${business?.name ?: "Store"}")
                putExtra(Intent.EXTRA_TEXT, "Cashbook report for ${business?.name ?: "Store"}. Total Balance: $currency%.2f".format(summary.totalBalance))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            safeStartActivity(context, Intent.createChooser(intent, "Share Cashbook PDF"), "No PDF viewer or share app available")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to export cashbook PDF", Toast.LENGTH_SHORT).show()
        } finally {
            doc.close()
        }
    }

    fun shareCashbookCsv(
        context: Context,
        business: BusinessEntity?,
        summary: com.example.data.model.CashbookSummary,
        entries: List<com.example.data.local.CashbookEntity>,
        currency: String
    ) {
        val shortDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val csv = StringBuilder()
        csv.append("Smart Khatabook - Cashbook Report\n")
        csv.append("Business:,${csvCell(business?.name ?: "My Business")}\n")
        csv.append("Total Balance:,${csvCell("$currency%.2f".format(summary.totalBalance))},Cash in Hand:,${csvCell("$currency%.2f".format(summary.totalCashInHand))},Online:,${csvCell("$currency%.2f".format(summary.totalOnline))}\n\n")
        csv.append("Date,Type,Mode,Amount ($currency),Note\n")
        for (entry in entries.sortedByDescending { it.entryDate }) {
            csv.append("${csvCell(shortDate.format(Date(entry.entryDate)))},${csvCell(entry.type)},${csvCell(entry.paymentMode)},${entry.amount},${csvCell(entry.note)}\n")
        }
        safeShareText(context, "Share Cashbook CSV", "Cashbook Report", csv.toString())
    }
}

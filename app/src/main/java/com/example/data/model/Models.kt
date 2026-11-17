package com.example.data.model

enum class TransactionType(val value: String) {
    CREDIT("CREDIT"),   // You gave money/goods (Udhar) - Customer owes you
    PAYMENT("PAYMENT"); // You received payment (Jama) - Settles credit

    companion object {
        fun from(value: String): TransactionType {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: CREDIT
        }
    }
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    UPI("UPI / GPay / PhonePe"),
    BANK_TRANSFER("Bank Transfer"),
    CHEQUE("Cheque"),
    OTHER("Other")
}

enum class ExpenseCategory(val displayName: String, val iconName: String) {
    INVENTORY("Inventory / Stock", "inventory"),
    RENT("Shop Rent", "store"),
    ELECTRICITY("Electricity / Bills", "bolt"),
    SALARY("Staff Salary", "payments"),
    FOOD_TEA("Tea & Refreshments", "coffee"),
    TRANSPORT("Transport / Delivery", "local_shipping"),
    MAINTENANCE("Maintenance & Repairs", "build"),
    MARKETING("Marketing & Ads", "campaign"),
    OTHER("Other Expenses", "more_horiz")
}

enum class BalanceType {
    YOU_WILL_GET,
    YOU_WILL_GIVE,
    SETTLED
}

enum class PartyType(val value: String) {
    CUSTOMER("CUSTOMER"),
    SUPPLIER("SUPPLIER");

    companion object {
        fun from(value: String?): PartyType {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: CUSTOMER
        }
    }
}

enum class CashbookType(val value: String) {
    IN("IN"),
    OUT("OUT");

    companion object {
        fun from(value: String?): CashbookType {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: OUT
        }
    }
}

enum class CashbookPaymentMode(val value: String, val label: String) {
    CASH("CASH", "Cash"),
    ONLINE("ONLINE", "Online");

    companion object {
        fun from(value: String?): CashbookPaymentMode {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: CASH
        }
    }
}

data class CustomerWithBalance(
    val id: Long,
    val businessId: Long,
    val name: String,
    val phone: String,
    val address: String,
    val openingBalance: Double,
    val openingBalanceType: String,
    val totalCredit: Double,
    val totalPayment: Double,
    val netBalance: Double, // Positive = You'll Get, Negative = You'll Give, 0 = Settled
    val lastTransactionDate: Long?,
    val transactionCount: Int,
    val partyType: String = "CUSTOMER"
) {
    val balanceType: BalanceType
        get() = when {
            netBalance > 0.009 -> BalanceType.YOU_WILL_GET
            netBalance < -0.009 -> BalanceType.YOU_WILL_GIVE
            else -> BalanceType.SETTLED
        }
}

data class TransactionWithCustomer(
    val transactionId: Long,
    val businessId: Long,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String,
    val amount: Double,
    val type: TransactionType,
    val paymentMethod: String,
    val description: String,
    val billNumber: String,
    val transactionDate: Long,
    val dueDate: Long?,
    val createdAt: Long
)

data class DashboardSummary(
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val totalCredit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val todayCollection: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val totalCustomers: Int = 0,
    val pendingCustomersCount: Int = 0,
    val monthCreditGiven: Double = 0.0,
    val monthPaymentsReceived: Double = 0.0,
    val monthExpenses: Double = 0.0
)

data class CashbookSummary(
    val totalBalance: Double = 0.0,
    val totalCashInHand: Double = 0.0,
    val totalOnline: Double = 0.0,
    val todayBalance: Double = 0.0,
    val todayCashInHand: Double = 0.0,
    val todayOnline: Double = 0.0,
    val todayOut: Double = 0.0,
    val todayIn: Double = 0.0,
    val todayEntryCount: Int = 0
)

data class CashbookEntryUi(
    val id: Long,
    val businessId: Long,
    val amount: Double,
    val type: CashbookType,
    val paymentMode: CashbookPaymentMode,
    val note: String,
    val entryDate: Long,
    val createdAt: Long
)

data class CashbookDayGroup(
    val dateKey: String,
    val dateLabel: String,
    val entryCount: Int,
    val totalOut: Double,
    val totalIn: Double,
    val entries: List<CashbookEntryUi>
)

enum class RecycleBinKind {
    PARTY,
    TRANSACTION,
    CASHBOOK
}

data class RecycleBinItem(
    val id: Long,
    val kind: RecycleBinKind,
    val title: String,
    val subtitle: String,
    val amount: Double?,
    val deletedAt: Long
)

data class CategoryExpenseSummary(
    val category: String,
    val totalAmount: Double,
    val count: Int
)

enum class AttendanceStatus(val code: String, val shortLabel: String) {
    NONE("NONE", ""),
    PRESENT("P", "P"),
    ABSENT("A", "A"),
    OVERTIME("OT", "OT"),
    OFF("OFF", "Off");

    companion object {
        fun from(value: String?): AttendanceStatus {
            return entries.firstOrNull { it.code.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

enum class StaffSalaryType(val displayName: String) {
    MONTHLY("Monthly Salary"),
    DAILY("Daily Wage")
}

data class StaffWithSummary(
    val id: Long,
    val businessId: Long,
    val name: String,
    val phone: String,
    val role: String,
    val salaryType: String,
    val salaryAmount: Double,
    val overtimeRate: Double,
    val presentCount: Int,
    val absentCount: Int,
    val overtimeCount: Int,
    val offCount: Int,
    val totalAdvance: Double,
    val todayStatus: AttendanceStatus
)

data class StaffDayRecord(
    val dateKey: String,        // yyyy-MM-dd
    val dayOfMonth: String,     // "01"
    val dayOfWeek: String,      // "Mon"
    val status: AttendanceStatus,
    val advanceAmount: Double,
    val advanceNote: String,
    val isToday: Boolean
)



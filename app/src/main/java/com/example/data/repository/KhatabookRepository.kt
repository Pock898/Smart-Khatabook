package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.BusinessEntity
import com.example.data.local.CashbookEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ItemEntity
import com.example.data.local.StaffAttendanceEntity
import com.example.data.local.StaffEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.CashbookDayGroup
import com.example.data.model.CashbookEntryUi
import com.example.data.model.CashbookPaymentMode
import com.example.data.model.CashbookSummary
import com.example.data.model.CashbookType
import com.example.data.model.CategoryExpenseSummary
import com.example.data.model.CustomerWithBalance
import com.example.data.model.DashboardSummary
import com.example.data.model.PartyType
import com.example.data.model.RecycleBinItem
import com.example.data.model.RecycleBinKind
import com.example.data.model.StaffWithSummary
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCustomer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KhatabookRepository(private val database: AppDatabase) {
    private val businessDao = database.businessDao()
    private val customerDao = database.customerDao()
    private val transactionDao = database.transactionDao()
    private val expenseDao = database.expenseDao()
    private val cashbookDao = database.cashbookDao()
    private val itemDao = database.itemDao()
    private val staffDao = database.staffDao()

    // Businesses
    fun getAllBusinesses(): Flow<List<BusinessEntity>> = businessDao.getAllBusinesses()

    fun getBusinessById(id: Long): Flow<BusinessEntity?> = businessDao.getBusinessById(id)

    suspend fun getBusinessByIdSync(id: Long): BusinessEntity? = withContext(Dispatchers.IO) {
        businessDao.getBusinessByIdSync(id)
    }

    suspend fun ensureDefaultBusiness(): Long = withContext(Dispatchers.IO) {
        val existing = businessDao.getFirstBusiness()
        if (existing != null) {
            existing.id
        } else {
            val defaultBusiness = BusinessEntity(
                name = "My Business",
                ownerName = "",
                phone = "",
                address = "",
                gstNumber = ""
            )
            businessDao.insertBusiness(defaultBusiness)
        }
    }

    suspend fun hasBusinessData(businessId: Long): Boolean = withContext(Dispatchers.IO) {
        customerDao.getAllCustomersSync(businessId).isNotEmpty() ||
            expenseDao.getAllExpensesSync(businessId).isNotEmpty() ||
            cashbookDao.getAllEntriesSync(businessId).isNotEmpty() ||
            itemDao.getAllItemsSync(businessId).isNotEmpty() ||
            staffDao.getAllStaffSync(businessId).isNotEmpty()
    }

    suspend fun createBusiness(
        name: String,
        ownerName: String,
        phone: String,
        address: String,
        gstNumber: String
    ): Long = withContext(Dispatchers.IO) {
        val entity = BusinessEntity(
            name = name.ifBlank { "My Business" },
            ownerName = ownerName,
            phone = phone,
            address = address,
            gstNumber = gstNumber
        )
        businessDao.insertBusiness(entity)
    }

    suspend fun updateBusiness(business: BusinessEntity) = withContext(Dispatchers.IO) {
        businessDao.updateBusiness(business)
    }

    suspend fun deleteBusiness(business: BusinessEntity) = withContext(Dispatchers.IO) {
        businessDao.deleteBusiness(business)
    }

    // Customers / Suppliers with reactive computed balances
    fun getCustomersWithBalance(
        businessId: Long,
        partyType: String? = PartyType.CUSTOMER.value
    ): Flow<List<CustomerWithBalance>> {
        val partiesFlow = if (partyType == null) {
            customerDao.getCustomersByBusiness(businessId)
        } else {
            customerDao.getPartiesByBusinessAndType(businessId, partyType)
        }
        return combine(
            partiesFlow,
            transactionDao.getTransactionsByBusiness(businessId)
        ) { customers, transactions ->
            val transactionsByCustomer = transactions.groupBy { it.customerId }

            customers.map { customer ->
                val custTxns = transactionsByCustomer[customer.id] ?: emptyList()
                val totalCredit = custTxns.filter { it.type == TransactionType.CREDIT.value }
                    .sumOf { it.amount }
                val totalPayment = custTxns.filter { it.type == TransactionType.PAYMENT.value }
                    .sumOf { it.amount }

                val openingAdjustment = if (customer.openingBalanceType.equals("GIVE", ignoreCase = true)) {
                    -customer.openingBalance
                } else {
                    customer.openingBalance
                }

                val netBalance = openingAdjustment + totalCredit - totalPayment
                val lastDate = custTxns.maxOfOrNull { it.transactionDate } ?: customer.createdAt

                CustomerWithBalance(
                    id = customer.id,
                    businessId = customer.businessId,
                    name = customer.name,
                    phone = customer.phone,
                    address = customer.address,
                    openingBalance = customer.openingBalance,
                    openingBalanceType = customer.openingBalanceType,
                    totalCredit = totalCredit,
                    totalPayment = totalPayment,
                    netBalance = netBalance,
                    lastTransactionDate = lastDate,
                    transactionCount = custTxns.size,
                    partyType = customer.partyType
                )
            }.sortedByDescending { it.lastTransactionDate ?: 0L }
        }.flowOn(Dispatchers.Default)
    }

    fun getSuppliersWithBalance(businessId: Long): Flow<List<CustomerWithBalance>> =
        getCustomersWithBalance(businessId, PartyType.SUPPLIER.value)

    fun getAllPartiesWithBalance(businessId: Long): Flow<List<CustomerWithBalance>> =
        getCustomersWithBalance(businessId, null)

    fun getCustomerWithBalance(customerId: Long): Flow<CustomerWithBalance?> {
        return combine(
            customerDao.getCustomerById(customerId),
            transactionDao.getTransactionsByCustomer(customerId)
        ) { customer, custTxns ->
            if (customer == null || customer.isDeleted) return@combine null

            val totalCredit = custTxns.filter { it.type == TransactionType.CREDIT.value }
                .sumOf { it.amount }
            val totalPayment = custTxns.filter { it.type == TransactionType.PAYMENT.value }
                .sumOf { it.amount }

            val openingAdjustment = if (customer.openingBalanceType.equals("GIVE", ignoreCase = true)) {
                -customer.openingBalance
            } else {
                customer.openingBalance
            }

            val netBalance = openingAdjustment + totalCredit - totalPayment
            val lastDate = custTxns.maxOfOrNull { it.transactionDate } ?: customer.createdAt

            CustomerWithBalance(
                id = customer.id,
                businessId = customer.businessId,
                name = customer.name,
                phone = customer.phone,
                address = customer.address,
                openingBalance = customer.openingBalance,
                openingBalanceType = customer.openingBalanceType,
                totalCredit = totalCredit,
                totalPayment = totalPayment,
                netBalance = netBalance,
                lastTransactionDate = lastDate,
                transactionCount = custTxns.size,
                partyType = customer.partyType
            )
        }.flowOn(Dispatchers.Default)
    }

    suspend fun addCustomer(
        businessId: Long,
        name: String,
        phone: String,
        address: String,
        openingBalance: Double,
        openingBalanceType: String,
        partyType: String = PartyType.CUSTOMER.value
    ): Long = withContext(Dispatchers.IO) {
        val customer = CustomerEntity(
            businessId = businessId,
            name = name.trim(),
            phone = phone.trim(),
            address = address.trim(),
            openingBalance = openingBalance,
            openingBalanceType = openingBalanceType,
            partyType = PartyType.from(partyType).value,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCustomer(customerId: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            transactionDao.softDeleteTransactionsByCustomer(customerId, now)
            customerDao.softDeleteCustomer(customerId, now)
        }
    }

    // Transactions
    fun getCustomerTransactions(customerId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCustomer(customerId)

    fun getAllTransactionsWithCustomer(businessId: Long): Flow<List<TransactionWithCustomer>> {
        return combine(
            transactionDao.getTransactionsByBusiness(businessId),
            customerDao.getCustomersByBusiness(businessId)
        ) { txns, customers ->
            val customerMap = customers.associateBy { it.id }
            txns.map { txn ->
                val customer = customerMap[txn.customerId]
                TransactionWithCustomer(
                    transactionId = txn.id,
                    businessId = txn.businessId,
                    customerId = txn.customerId,
                    customerName = customer?.name ?: "Unknown Customer",
                    customerPhone = customer?.phone ?: "",
                    amount = txn.amount,
                    type = TransactionType.from(txn.type),
                    paymentMethod = txn.paymentMethod,
                    description = txn.description,
                    billNumber = txn.billNumber,
                    transactionDate = txn.transactionDate,
                    dueDate = txn.dueDate,
                    createdAt = txn.createdAt
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun addTransaction(
        businessId: Long,
        customerId: Long,
        amount: Double,
        type: TransactionType,
        paymentMethod: String,
        description: String,
        billNumber: String,
        transactionDate: Long,
        dueDate: Long?
    ): Long = withContext(Dispatchers.IO) {
        val txn = TransactionEntity(
            businessId = businessId,
            customerId = customerId,
            amount = amount,
            type = type.value,
            paymentMethod = paymentMethod,
            description = description.trim(),
            billNumber = billNumber.trim(),
            transactionDate = transactionDate,
            dueDate = dueDate,
            createdAt = System.currentTimeMillis()
        )
        val id = transactionDao.insertTransaction(txn)
        customerDao.updateCustomerTimestamp(customerId, System.currentTimeMillis())
        id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
        customerDao.updateCustomerTimestamp(transaction.customerId, System.currentTimeMillis())
    }

    suspend fun deleteTransaction(transactionId: Long) = withContext(Dispatchers.IO) {
        val txn = transactionDao.getTransactionByIdSync(transactionId)
        if (txn != null) {
            transactionDao.softDeleteTransaction(transactionId, System.currentTimeMillis())
            customerDao.updateCustomerTimestamp(txn.customerId, System.currentTimeMillis())
        }
    }

    // Expenses
    fun getExpenses(businessId: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByBusiness(businessId)

    suspend fun addExpense(
        businessId: Long,
        amount: Double,
        category: String,
        paymentMethod: String,
        description: String,
        expenseDate: Long
    ): Long = withContext(Dispatchers.IO) {
        val expense = ExpenseEntity(
            businessId = businessId,
            amount = amount,
            category = category,
            paymentMethod = paymentMethod,
            description = description.trim(),
            expenseDate = expenseDate
        )
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expenseId: Long) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpenseById(expenseId)
    }

    // Dashboard Summary
    fun getDashboardSummary(businessId: Long): Flow<DashboardSummary> {
        return combine(
            getCustomersWithBalance(businessId),
            transactionDao.getTransactionsByBusiness(businessId),
            expenseDao.getExpensesByBusiness(businessId)
        ) { customersWithBal, transactions, expenses ->
            val now = System.currentTimeMillis()
            val startOfToday = getStartOfDay(now)
            val startOfMonth = getStartOfMonth(now)

            var receivable = 0.0
            var payable = 0.0
            var pendingCount = 0

            for (cust in customersWithBal) {
                if (cust.netBalance > 0.009) {
                    receivable += cust.netBalance
                    pendingCount++
                } else if (cust.netBalance < -0.009) {
                    payable += (-cust.netBalance)
                }
            }

            val totalCredit = transactions
                .filter { it.type == TransactionType.CREDIT.value }
                .sumOf { it.amount }

            val totalExpenses = expenses.sumOf { it.amount }

            val todayCollection = transactions
                .filter { it.type == TransactionType.PAYMENT.value && it.transactionDate >= startOfToday }
                .sumOf { it.amount }

            val todayExpenses = expenses
                .filter { it.expenseDate >= startOfToday }
                .sumOf { it.amount }

            val monthCredit = transactions
                .filter { it.type == TransactionType.CREDIT.value && it.transactionDate >= startOfMonth }
                .sumOf { it.amount }

            val monthPayments = transactions
                .filter { it.type == TransactionType.PAYMENT.value && it.transactionDate >= startOfMonth }
                .sumOf { it.amount }

            val monthExpenses = expenses
                .filter { it.expenseDate >= startOfMonth }
                .sumOf { it.amount }

            DashboardSummary(
                totalReceivable = receivable,
                totalPayable = payable,
                totalCredit = totalCredit,
                totalExpenses = totalExpenses,
                todayCollection = todayCollection,
                todayExpenses = todayExpenses,
                totalCustomers = customersWithBal.size,
                pendingCustomersCount = pendingCount,
                monthCreditGiven = monthCredit,
                monthPaymentsReceived = monthPayments,
                monthExpenses = monthExpenses
            )
        }.flowOn(Dispatchers.Default)
    }

    fun getCategoryExpenseSummary(businessId: Long): Flow<List<CategoryExpenseSummary>> {
        return expenseDao.getExpensesByBusiness(businessId).map { expenses ->
            expenses.groupBy { it.category }.map { (category, list) ->
                CategoryExpenseSummary(
                    category = category,
                    totalAmount = list.sumOf { it.amount },
                    count = list.size
                )
            }.sortedByDescending { it.totalAmount }
        }.flowOn(Dispatchers.Default)
    }

    // Cashbook Management
    fun getCashbookEntries(businessId: Long): Flow<List<CashbookEntity>> =
        cashbookDao.getEntriesByBusiness(businessId)

    fun getCashbookSummary(businessId: Long): Flow<CashbookSummary> {
        return cashbookDao.getEntriesByBusiness(businessId).map { entries ->
            val startOfToday = getStartOfDay(System.currentTimeMillis())
            var totalCash = 0.0
            var totalOnline = 0.0
            var todayCash = 0.0
            var todayOnline = 0.0
            var todayIn = 0.0
            var todayOut = 0.0
            var todayCount = 0

            for (e in entries) {
                val signedAmount = if (CashbookType.from(e.type) == CashbookType.IN) e.amount else -e.amount
                val mode = CashbookPaymentMode.from(e.paymentMode)
                if (mode == CashbookPaymentMode.CASH) {
                    totalCash += signedAmount
                } else {
                    totalOnline += signedAmount
                }

                if (e.entryDate >= startOfToday) {
                    todayCount++
                    if (mode == CashbookPaymentMode.CASH) {
                        todayCash += signedAmount
                    } else {
                        todayOnline += signedAmount
                    }
                    if (signedAmount >= 0) todayIn += e.amount else todayOut += e.amount
                }
            }

            CashbookSummary(
                totalBalance = totalCash + totalOnline,
                totalCashInHand = totalCash,
                totalOnline = totalOnline,
                todayBalance = todayCash + todayOnline,
                todayCashInHand = todayCash,
                todayOnline = todayOnline,
                todayOut = todayOut,
                todayIn = todayIn,
                todayEntryCount = todayCount
            )
        }.flowOn(Dispatchers.Default)
    }

    fun getCashbookDayGroups(businessId: Long): Flow<List<CashbookDayGroup>> {
        return cashbookDao.getEntriesByBusiness(businessId).map { entries ->
            val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val labelFmt = SimpleDateFormat("dd MMM", Locale.getDefault())
            val uiList = entries.map { e ->
                CashbookEntryUi(
                    id = e.id,
                    businessId = e.businessId,
                    amount = e.amount,
                    type = CashbookType.from(e.type),
                    paymentMode = CashbookPaymentMode.from(e.paymentMode),
                    note = e.note,
                    entryDate = e.entryDate,
                    createdAt = e.createdAt
                )
            }
            uiList.groupBy { keyFmt.format(Date(it.entryDate)) }.map { (dateKey, dayEntries) ->
                val sampleDate = dayEntries.first().entryDate
                val totalIn = dayEntries.filter { it.type == CashbookType.IN }.sumOf { it.amount }
                val totalOut = dayEntries.filter { it.type == CashbookType.OUT }.sumOf { it.amount }
                CashbookDayGroup(
                    dateKey = dateKey,
                    dateLabel = labelFmt.format(Date(sampleDate)).uppercase(Locale.getDefault()),
                    entryCount = dayEntries.size,
                    totalOut = totalOut,
                    totalIn = totalIn,
                    entries = dayEntries
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun addCashbookEntry(
        businessId: Long,
        amount: Double,
        type: CashbookType,
        paymentMode: CashbookPaymentMode,
        note: String,
        entryDate: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        cashbookDao.insertEntry(
            CashbookEntity(
                businessId = businessId,
                amount = amount,
                type = type.value,
                paymentMode = paymentMode.value,
                note = note.trim(),
                entryDate = entryDate
            )
        )
    }

    suspend fun updateCashbookEntry(entry: CashbookEntity) = withContext(Dispatchers.IO) {
        cashbookDao.updateEntry(entry)
    }

    suspend fun deleteCashbookEntry(entryId: Long) = withContext(Dispatchers.IO) {
        cashbookDao.softDeleteEntry(entryId, System.currentTimeMillis())
    }

    // Items / Inventory Catalog
    fun getItems(businessId: Long): Flow<List<ItemEntity>> =
        itemDao.getItemsByBusiness(businessId)

    suspend fun addItem(
        businessId: Long,
        name: String,
        unit: String,
        salePrice: Double,
        purchasePrice: Double,
        stockQty: Double,
        lowStockAlert: Double
    ): Long = withContext(Dispatchers.IO) {
        itemDao.insertItem(
            ItemEntity(
                businessId = businessId,
                name = name.trim(),
                unit = unit.trim().ifBlank { "pcs" },
                salePrice = salePrice.coerceAtLeast(0.0),
                purchasePrice = purchasePrice.coerceAtLeast(0.0),
                stockQty = stockQty.coerceAtLeast(0.0),
                lowStockAlert = lowStockAlert.coerceAtLeast(0.0)
            )
        )
    }

    suspend fun updateItem(item: ItemEntity) = withContext(Dispatchers.IO) {
        itemDao.updateItem(item.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteItem(itemId: Long) = withContext(Dispatchers.IO) {
        itemDao.deleteItemById(itemId)
    }

    // Recycle Bin (Soft-deleted Parties, Transactions, Cashbook Entries)
    fun getRecycleBinItems(businessId: Long): Flow<List<RecycleBinItem>> {
        return combine(
            customerDao.getDeletedCustomersByBusiness(businessId),
            transactionDao.getDeletedTransactionsByBusiness(businessId),
            cashbookDao.getDeletedEntriesByBusiness(businessId),
            customerDao.getCustomersByBusiness(businessId)
        ) { deletedCustomers, deletedTxns, deletedCashbook, activeCustomers ->
            val nameMap = (activeCustomers + deletedCustomers).associate { it.id to it.name }
            val partyItems = deletedCustomers.map { c ->
                val kindLabel = if (PartyType.from(c.partyType) == PartyType.SUPPLIER) "Supplier" else "Customer"
                RecycleBinItem(
                    id = c.id,
                    kind = RecycleBinKind.PARTY,
                    title = c.name,
                    subtitle = "$kindLabel${if (c.phone.isNotBlank()) " • ${c.phone}" else ""}",
                    amount = c.openingBalance.takeIf { it > 0.0 },
                    deletedAt = c.deletedAt ?: c.updatedAt
                )
            }
            val txnItems = deletedTxns.map { t ->
                val partyName = nameMap[t.customerId] ?: "Party"
                val typeLabel = if (t.type == TransactionType.CREDIT.value) "Credit (Gave)" else "Payment (Got)"
                RecycleBinItem(
                    id = t.id,
                    kind = RecycleBinKind.TRANSACTION,
                    title = "$partyName • $typeLabel",
                    subtitle = t.description.ifBlank { "Ledger transaction" },
                    amount = t.amount,
                    deletedAt = t.deletedAt ?: t.createdAt
                )
            }
            val cbItems = deletedCashbook.map { cb ->
                val typeLabel = if (CashbookType.from(cb.type) == CashbookType.IN) "Cashbook IN" else "Cashbook OUT"
                RecycleBinItem(
                    id = cb.id,
                    kind = RecycleBinKind.CASHBOOK,
                    title = "$typeLabel (${cb.paymentMode})",
                    subtitle = cb.note.ifBlank { "Cashbook entry" },
                    amount = cb.amount,
                    deletedAt = cb.deletedAt ?: cb.createdAt
                )
            }
            (partyItems + txnItems + cbItems).sortedByDescending { it.deletedAt }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun restoreRecycleBinItem(item: RecycleBinItem) = withContext(Dispatchers.IO) {
        when (item.kind) {
            RecycleBinKind.PARTY -> {
                database.withTransaction {
                    customerDao.restoreCustomer(item.id)
                    transactionDao.restoreTransactionsByCustomer(item.id)
                }
            }
            RecycleBinKind.TRANSACTION -> {
                val txn = transactionDao.getTransactionByIdSync(item.id) ?: return@withContext
                database.withTransaction {
                    customerDao.restoreCustomer(txn.customerId)
                    transactionDao.restoreTransaction(item.id)
                }
            }
            RecycleBinKind.CASHBOOK -> {
                cashbookDao.restoreEntry(item.id)
            }
        }
    }

    suspend fun permanentlyDeleteRecycleBinItem(item: RecycleBinItem) = withContext(Dispatchers.IO) {
        when (item.kind) {
            RecycleBinKind.PARTY -> {
                transactionDao.deleteTransactionsByCustomer(item.id)
                customerDao.deleteCustomerById(item.id)
            }
            RecycleBinKind.TRANSACTION -> transactionDao.deleteTransactionById(item.id)
            RecycleBinKind.CASHBOOK -> cashbookDao.deleteEntryById(item.id)
        }
    }

    suspend fun emptyRecycleBin(businessId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            transactionDao.purgeDeletedTransactions(businessId)
            customerDao.purgeDeletedCustomers(businessId)
            cashbookDao.purgeDeletedEntries(businessId)
        }
    }

    // Staff & Attendance Management
    fun getStaffWithSummary(businessId: Long, monthPrefix: String, todayKey: String): Flow<List<StaffWithSummary>> {
        return combine(
            staffDao.getStaffByBusiness(businessId),
            staffDao.getAttendanceByBusinessMonth(businessId, monthPrefix)
        ) { staffList, monthEntries ->
            val entriesByStaff = monthEntries.groupBy { it.staffId }
            staffList.map { staff ->
                val list = entriesByStaff[staff.id] ?: emptyList()
                val present = list.count { AttendanceStatus.from(it.status) == AttendanceStatus.PRESENT }
                val absent = list.count { AttendanceStatus.from(it.status) == AttendanceStatus.ABSENT }
                val overtime = list.count { AttendanceStatus.from(it.status) == AttendanceStatus.OVERTIME }
                val off = list.count { AttendanceStatus.from(it.status) == AttendanceStatus.OFF }
                val totalAdv = list.sumOf { it.advanceAmount }
                val todayEntry = list.firstOrNull { it.dateKey == todayKey }
                StaffWithSummary(
                    id = staff.id,
                    businessId = staff.businessId,
                    name = staff.name,
                    phone = staff.phone,
                    role = staff.role,
                    salaryType = staff.salaryType,
                    salaryAmount = staff.salaryAmount,
                    overtimeRate = staff.overtimeRate,
                    presentCount = present,
                    absentCount = absent,
                    overtimeCount = overtime,
                    offCount = off,
                    totalAdvance = totalAdv,
                    todayStatus = AttendanceStatus.from(todayEntry?.status)
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    fun getStaffById(staffId: Long): Flow<StaffEntity?> = staffDao.getStaffById(staffId)

    fun getStaffAttendanceForMonth(staffId: Long, monthPrefix: String): Flow<List<StaffAttendanceEntity>> =
        staffDao.getAttendanceForStaffMonth(staffId, monthPrefix)

    suspend fun addStaff(
        businessId: Long,
        name: String,
        phone: String,
        role: String,
        salaryType: String,
        salaryAmount: Double,
        overtimeRate: Double
    ): Long = withContext(Dispatchers.IO) {
        val entity = StaffEntity(
            businessId = businessId,
            name = name.trim(),
            phone = phone.trim(),
            role = role.trim().ifBlank { "Staff" },
            salaryType = salaryType,
            salaryAmount = salaryAmount,
            overtimeRate = overtimeRate
        )
        staffDao.insertStaff(entity)
    }

    suspend fun updateStaff(staff: StaffEntity) = withContext(Dispatchers.IO) {
        staffDao.updateStaff(staff.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteStaff(staffId: Long) = withContext(Dispatchers.IO) {
        staffDao.deleteAttendanceByStaff(staffId)
        staffDao.deleteStaffById(staffId)
    }

    suspend fun setStaffAttendanceStatus(
        businessId: Long,
        staffId: Long,
        dateKey: String,
        targetStatus: AttendanceStatus
    ) = withContext(Dispatchers.IO) {
        val existing = staffDao.getAttendanceByStaffAndDateSync(staffId, dateKey)
        val currentStatus = AttendanceStatus.from(existing?.status)
        // One-tap toggle: tapping an already selected status clears it back to NONE
        val nextStatus = if (currentStatus == targetStatus) AttendanceStatus.NONE else targetStatus

        val entry = existing?.copy(
            status = nextStatus.code,
            updatedAt = System.currentTimeMillis()
        ) ?: StaffAttendanceEntity(
            businessId = businessId,
            staffId = staffId,
            dateKey = dateKey,
            status = nextStatus.code,
            advanceAmount = 0.0,
            advanceNote = ""
        )
        staffDao.upsertAttendance(entry)
    }

    suspend fun setStaffAdvance(
        businessId: Long,
        staffId: Long,
        dateKey: String,
        amount: Double,
        note: String
    ) = withContext(Dispatchers.IO) {
        val existing = staffDao.getAttendanceByStaffAndDateSync(staffId, dateKey)
        val entry = existing?.copy(
            advanceAmount = amount.coerceAtLeast(0.0),
            advanceNote = note.trim(),
            updatedAt = System.currentTimeMillis()
        ) ?: StaffAttendanceEntity(
            businessId = businessId,
            staffId = staffId,
            dateKey = dateKey,
            status = AttendanceStatus.NONE.code,
            advanceAmount = amount.coerceAtLeast(0.0),
            advanceNote = note.trim()
        )
        staffDao.upsertAttendance(entry)
    }

    // JSON Backup Export & Import
    suspend fun exportBackupJson(businessId: Long): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTime", System.currentTimeMillis())

        val business = businessDao.getBusinessByIdSync(businessId)
        if (business != null) {
            val bJson = JSONObject()
            bJson.put("id", business.id)
            bJson.put("name", business.name)
            bJson.put("ownerName", business.ownerName)
            bJson.put("phone", business.phone)
            bJson.put("address", business.address)
            bJson.put("gstNumber", business.gstNumber)
            root.put("business", bJson)
        }

        val customers = customerDao.getAllCustomersSync(businessId)
        val customersArray = JSONArray()
        for (c in customers) {
            val cJson = JSONObject()
            cJson.put("id", c.id)
            cJson.put("name", c.name)
            cJson.put("phone", c.phone)
            cJson.put("address", c.address)
            cJson.put("openingBalance", c.openingBalance)
            cJson.put("openingBalanceType", c.openingBalanceType)
            cJson.put("partyType", c.partyType)
            cJson.put("createdAt", c.createdAt)
            customersArray.put(cJson)
        }
        root.put("customers", customersArray)

        val txns = transactionDao.getAllTransactionsByBusinessSync(businessId)
        val txnsArray = JSONArray()
        for (t in txns) {
            val tJson = JSONObject()
            tJson.put("id", t.id)
            tJson.put("customerId", t.customerId)
            tJson.put("amount", t.amount)
            tJson.put("type", t.type)
            tJson.put("paymentMethod", t.paymentMethod)
            tJson.put("description", t.description)
            tJson.put("billNumber", t.billNumber)
            tJson.put("transactionDate", t.transactionDate)
            tJson.put("dueDate", t.dueDate ?: -1L)
            txnsArray.put(tJson)
        }
        root.put("transactions", txnsArray)

        val expenses = expenseDao.getAllExpensesSync(businessId)
        val expArray = JSONArray()
        for (e in expenses) {
            val eJson = JSONObject()
            eJson.put("id", e.id)
            eJson.put("amount", e.amount)
            eJson.put("category", e.category)
            eJson.put("paymentMethod", e.paymentMethod)
            eJson.put("description", e.description)
            eJson.put("expenseDate", e.expenseDate)
            expArray.put(eJson)
        }
        root.put("expenses", expArray)

        val cashbookList = cashbookDao.getAllEntriesSync(businessId)
        val cashbookArray = JSONArray()
        for (cb in cashbookList) {
            val cbJson = JSONObject()
            cbJson.put("id", cb.id)
            cbJson.put("amount", cb.amount)
            cbJson.put("type", cb.type)
            cbJson.put("paymentMode", cb.paymentMode)
            cbJson.put("note", cb.note)
            cbJson.put("entryDate", cb.entryDate)
            cashbookArray.put(cbJson)
        }
        root.put("cashbook", cashbookArray)

        val itemsList = itemDao.getAllItemsSync(businessId)
        val itemsArray = JSONArray()
        for (item in itemsList) {
            val iJson = JSONObject()
            iJson.put("id", item.id)
            iJson.put("name", item.name)
            iJson.put("unit", item.unit)
            iJson.put("salePrice", item.salePrice)
            iJson.put("purchasePrice", item.purchasePrice)
            iJson.put("stockQty", item.stockQty)
            iJson.put("lowStockAlert", item.lowStockAlert)
            itemsArray.put(iJson)
        }
        root.put("items", itemsArray)

        val staffList = staffDao.getAllStaffSync(businessId)
        val staffArray = JSONArray()
        for (s in staffList) {
            val sJson = JSONObject()
            sJson.put("id", s.id)
            sJson.put("name", s.name)
            sJson.put("phone", s.phone)
            sJson.put("role", s.role)
            sJson.put("salaryType", s.salaryType)
            sJson.put("salaryAmount", s.salaryAmount)
            sJson.put("overtimeRate", s.overtimeRate)
            staffArray.put(sJson)
        }
        root.put("staff", staffArray)

        val staffAtt = staffDao.getAllAttendanceSync(businessId)
        val attArray = JSONArray()
        for (a in staffAtt) {
            val aJson = JSONObject()
            aJson.put("staffId", a.staffId)
            aJson.put("dateKey", a.dateKey)
            aJson.put("status", a.status)
            aJson.put("advanceAmount", a.advanceAmount)
            aJson.put("advanceNote", a.advanceNote)
            attArray.put(aJson)
        }
        root.put("staffAttendance", attArray)

        root.toString(2)
    }

    suspend fun importBackupJson(businessId: Long, jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val trimmed = jsonStr.trim()
            if (trimmed.isBlank()) return@withContext false

            val root = JSONObject(trimmed)
            val hasExpectedStructure = root.has("version") ||
                root.has("customers") ||
                root.has("transactions") ||
                root.has("expenses") ||
                root.has("cashbook") ||
                root.has("items") ||
                root.has("staff")
            if (!hasExpectedStructure) return@withContext false

            database.withTransaction {
                val customersArray = root.optJSONArray("customers") ?: JSONArray()
                val oldToNewCustomerIdMap = mutableMapOf<Long, Long>()

                for (i in 0 until customersArray.length()) {
                    val cJson = customersArray.getJSONObject(i)
                    val oldId = cJson.optLong("id", -1L)
                    val customer = CustomerEntity(
                        businessId = businessId,
                        name = cJson.optString("name", "Customer"),
                        phone = cJson.optString("phone", ""),
                        address = cJson.optString("address", ""),
                        openingBalance = cJson.optDouble("openingBalance", 0.0),
                        openingBalanceType = cJson.optString("openingBalanceType", "GET"),
                        partyType = PartyType.from(cJson.optString("partyType", "CUSTOMER")).value
                    )
                    val newId = customerDao.insertCustomer(customer)
                    if (oldId != -1L) {
                        oldToNewCustomerIdMap[oldId] = newId
                    }
                }

                val txnsArray = root.optJSONArray("transactions") ?: JSONArray()
                for (i in 0 until txnsArray.length()) {
                    val tJson = txnsArray.getJSONObject(i)
                    val oldCustomerId = tJson.optLong("customerId", -1L)
                    val newCustomerId = oldToNewCustomerIdMap[oldCustomerId] ?: continue

                    val dueDateRaw = tJson.optLong("dueDate", -1L)
                    val txn = TransactionEntity(
                        businessId = businessId,
                        customerId = newCustomerId,
                        amount = tJson.optDouble("amount", 0.0),
                        type = tJson.optString("type", "CREDIT"),
                        paymentMethod = tJson.optString("paymentMethod", "CASH"),
                        description = tJson.optString("description", ""),
                        billNumber = tJson.optString("billNumber", ""),
                        transactionDate = tJson.optLong("transactionDate", System.currentTimeMillis()),
                        dueDate = if (dueDateRaw > 0) dueDateRaw else null
                    )
                    transactionDao.insertTransaction(txn)
                }

                val expArray = root.optJSONArray("expenses") ?: JSONArray()
                for (i in 0 until expArray.length()) {
                    val eJson = expArray.getJSONObject(i)
                    val exp = ExpenseEntity(
                        businessId = businessId,
                        amount = eJson.optDouble("amount", 0.0),
                        category = eJson.optString("category", "OTHER"),
                        paymentMethod = eJson.optString("paymentMethod", "CASH"),
                        description = eJson.optString("description", ""),
                        expenseDate = eJson.optLong("expenseDate", System.currentTimeMillis())
                    )
                    expenseDao.insertExpense(exp)
                }

                val cbArray = root.optJSONArray("cashbook") ?: JSONArray()
                for (i in 0 until cbArray.length()) {
                    val cbJson = cbArray.getJSONObject(i)
                    cashbookDao.insertEntry(
                        CashbookEntity(
                            businessId = businessId,
                            amount = cbJson.optDouble("amount", 0.0),
                            type = CashbookType.from(cbJson.optString("type", "OUT")).value,
                            paymentMode = CashbookPaymentMode.from(cbJson.optString("paymentMode", "CASH")).value,
                            note = cbJson.optString("note", ""),
                            entryDate = cbJson.optLong("entryDate", System.currentTimeMillis())
                        )
                    )
                }

                val itemsArray = root.optJSONArray("items") ?: JSONArray()
                for (i in 0 until itemsArray.length()) {
                    val iJson = itemsArray.getJSONObject(i)
                    itemDao.insertItem(
                        ItemEntity(
                            businessId = businessId,
                            name = iJson.optString("name", "Item"),
                            unit = iJson.optString("unit", "pcs"),
                            salePrice = iJson.optDouble("salePrice", 0.0),
                            purchasePrice = iJson.optDouble("purchasePrice", 0.0),
                            stockQty = iJson.optDouble("stockQty", 0.0),
                            lowStockAlert = iJson.optDouble("lowStockAlert", 5.0)
                        )
                    )
                }

                val staffArray = root.optJSONArray("staff") ?: JSONArray()
                val oldToNewStaffIdMap = mutableMapOf<Long, Long>()
                for (i in 0 until staffArray.length()) {
                    val sJson = staffArray.getJSONObject(i)
                    val oldId = sJson.optLong("id", -1L)
                    val staff = StaffEntity(
                        businessId = businessId,
                        name = sJson.optString("name", "Staff"),
                        phone = sJson.optString("phone", ""),
                        role = sJson.optString("role", "Staff"),
                        salaryType = sJson.optString("salaryType", "MONTHLY"),
                        salaryAmount = sJson.optDouble("salaryAmount", 0.0),
                        overtimeRate = sJson.optDouble("overtimeRate", 0.0)
                    )
                    val newId = staffDao.insertStaff(staff)
                    if (oldId != -1L) {
                        oldToNewStaffIdMap[oldId] = newId
                    }
                }

                val attArray = root.optJSONArray("staffAttendance") ?: JSONArray()
                for (i in 0 until attArray.length()) {
                    val aJson = attArray.getJSONObject(i)
                    val oldStaffId = aJson.optLong("staffId", -1L)
                    val newStaffId = oldToNewStaffIdMap[oldStaffId] ?: continue
                    val dateKey = aJson.optString("dateKey", "")
                    if (dateKey.isBlank()) continue
                    staffDao.upsertAttendance(
                        StaffAttendanceEntity(
                            businessId = businessId,
                            staffId = newStaffId,
                            dateKey = dateKey,
                            status = aJson.optString("status", "NONE"),
                            advanceAmount = aJson.optDouble("advanceAmount", 0.0),
                            advanceNote = aJson.optString("advanceNote", "")
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun populateDemoData(businessId: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val oneDay = 86_400_000L

        // Customer 1: Ramesh Kumar (High credit)
        val c1 = customerDao.insertCustomer(
            CustomerEntity(
                businessId = businessId,
                name = "Ramesh Kumar (Kirana)",
                phone = "+91 98234 56789",
                address = "Shop 12, Gandhi Chowk",
                openingBalance = 1500.0,
                openingBalanceType = "GET"
            )
        )
        transactionDao.insertTransaction(
            TransactionEntity(
                businessId = businessId,
                customerId = c1,
                amount = 2500.0,
                type = TransactionType.CREDIT.value,
                description = "Basmati Rice 25kg & Cooking Oil",
                billNumber = "INV-101",
                transactionDate = now - (oneDay * 4),
                dueDate = now + (oneDay * 3)
            )
        )
        transactionDao.insertTransaction(
            TransactionEntity(
                businessId = businessId,
                customerId = c1,
                amount = 1000.0,
                type = TransactionType.PAYMENT.value,
                paymentMethod = "UPI",
                description = "Google Pay partial payment",
                billNumber = "REC-201",
                transactionDate = now - (oneDay * 2)
            )
        )

        // Customer 2: Priya Sharma (Small pending)
        val c2 = customerDao.insertCustomer(
            CustomerEntity(
                businessId = businessId,
                name = "Priya Sharma",
                phone = "+91 94123 45678",
                address = "A-402, Green Valley Apts",
                openingBalance = 0.0,
                openingBalanceType = "GET"
            )
        )
        transactionDao.insertTransaction(
            TransactionEntity(
                businessId = businessId,
                customerId = c2,
                amount = 850.0,
                type = TransactionType.CREDIT.value,
                description = "Daily Dairy & Grocery items",
                billNumber = "INV-102",
                transactionDate = now - (oneDay * 1),
                dueDate = now + (oneDay * 2)
            )
        )

        // Customer 3: Suresh Verma (Settled)
        val c3 = customerDao.insertCustomer(
            CustomerEntity(
                businessId = businessId,
                name = "Suresh Verma (Electrician)",
                phone = "+91 97890 12345",
                address = "Near Railway Station",
                openingBalance = 0.0,
                openingBalanceType = "GET"
            )
        )
        transactionDao.insertTransaction(
            TransactionEntity(
                businessId = businessId,
                customerId = c3,
                amount = 1200.0,
                type = TransactionType.CREDIT.value,
                description = "Wires and switches",
                billNumber = "INV-103",
                transactionDate = now - (oneDay * 5)
            )
        )
        transactionDao.insertTransaction(
            TransactionEntity(
                businessId = businessId,
                customerId = c3,
                amount = 1200.0,
                type = TransactionType.PAYMENT.value,
                paymentMethod = "CASH",
                description = "Cash settlement in full",
                billNumber = "REC-202",
                transactionDate = now - (oneDay * 1)
            )
        )

        // Customer 4: Amit Patel (You'll give - Advance payment)
        val c4 = customerDao.insertCustomer(
            CustomerEntity(
                businessId = businessId,
                name = "Amit Patel",
                phone = "+91 99112 33445",
                address = "Patel Colony",
                openingBalance = 500.0,
                openingBalanceType = "GIVE"
            )
        )
        transactionDao.insertTransaction(
            TransactionEntity(
                businessId = businessId,
                customerId = c4,
                amount = 300.0,
                type = TransactionType.CREDIT.value,
                description = "Spices & tea pack",
                billNumber = "INV-104",
                transactionDate = now - (oneDay * 3)
            )
        )

        // Sample Expenses
        expenseDao.insertExpense(
            ExpenseEntity(
                businessId = businessId,
                amount = 450.0,
                category = "FOOD_TEA",
                paymentMethod = "CASH",
                description = "Morning tea and snacks for shop staff",
                expenseDate = now
            )
        )
        expenseDao.insertExpense(
            ExpenseEntity(
                businessId = businessId,
                amount = 1200.0,
                category = "ELECTRICITY",
                paymentMethod = "UPI",
                description = "Shop electricity bill payment",
                expenseDate = now - (oneDay * 2)
            )
        )
        expenseDao.insertExpense(
            ExpenseEntity(
                businessId = businessId,
                amount = 3500.0,
                category = "INVENTORY",
                paymentMethod = "BANK_TRANSFER",
                description = "Wholesale pulses & flour batch",
                expenseDate = now - (oneDay * 3)
            )
        )

        // Sample Staff & Attendance
        val s1 = staffDao.insertStaff(
            StaffEntity(
                businessId = businessId,
                name = "Harish",
                phone = "9988776433",
                role = "Shop Assistant",
                salaryType = "MONTHLY",
                salaryAmount = 15000.0,
                overtimeRate = 250.0
            )
        )
        val s2 = staffDao.insertStaff(
            StaffEntity(
                businessId = businessId,
                name = "Vikram Singh",
                phone = "9811223344",
                role = "Delivery Boy",
                salaryType = "DAILY",
                salaryAmount = 500.0,
                overtimeRate = 150.0
            )
        )

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val todayDay = cal.get(Calendar.DAY_OF_MONTH)
        for (d in 1..minOf(todayDay, 12)) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            val key = dateFmt.format(cal.time)
            val status = when {
                d == 4 -> AttendanceStatus.ABSENT.code
                d == 7 -> AttendanceStatus.OFF.code
                d == 9 -> AttendanceStatus.OVERTIME.code
                else -> AttendanceStatus.PRESENT.code
            }
            val adv = if (d == 5) 1500.0 else 0.0
            staffDao.upsertAttendance(
                StaffAttendanceEntity(
                    businessId = businessId,
                    staffId = s1,
                    dateKey = key,
                    status = status,
                    advanceAmount = adv,
                    advanceNote = if (adv > 0) "Cash advance" else ""
                )
            )
            staffDao.upsertAttendance(
                StaffAttendanceEntity(
                    businessId = businessId,
                    staffId = s2,
                    dateKey = key,
                    status = if (d == 6) AttendanceStatus.ABSENT.code else AttendanceStatus.PRESENT.code,
                    advanceAmount = if (d == 3) 500.0 else 0.0,
                    advanceNote = if (d == 3) "Fuel & lunch advance" else ""
                )
            )
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

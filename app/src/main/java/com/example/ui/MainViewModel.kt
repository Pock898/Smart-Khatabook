package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.FirebaseAuthHelper
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
import com.example.data.model.CashbookPaymentMode
import com.example.data.model.CashbookSummary
import com.example.data.model.CashbookType
import com.example.data.model.CategoryExpenseSummary
import com.example.data.model.CustomerWithBalance
import com.example.data.model.DashboardSummary
import com.example.data.model.RecycleBinItem
import com.example.data.model.StaffAttendanceCalculator
import com.example.data.model.StaffDayRecord
import com.example.data.model.StaffWithSummary
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCustomer
import com.example.data.repository.KhatabookRepository
import com.example.data.repository.PreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = KhatabookRepository(database)
    val prefs = PreferencesManager.getInstance(application)

    // Security & App Lock
    private val _isUnlocked = MutableStateFlow(!prefs.isPinEnabled.value)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Active Business ID & Auth Session
    val activeBusinessId = prefs.activeBusinessId
    val language = prefs.language
    val currency = prefs.currency
    val themeMode = prefs.themeMode
    val isPinEnabled = prefs.isPinEnabled
    val isLoggedIn = prefs.isLoggedIn
    val loggedInPhone = prefs.loggedInPhone
    val upiId = prefs.upiId
    val defaultCashbookMode = prefs.defaultCashbookMode
    val hasSeenCashbookGuide = prefs.hasSeenCashbookGuide

    // All businesses
    val allBusinesses: StateFlow<List<BusinessEntity>> = repository.getAllBusinesses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Business Entity
    val activeBusiness: StateFlow<BusinessEntity?> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(null) else repository.getBusinessById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Customers with reactive computed balances
    val customersWithBalance: StateFlow<List<CustomerWithBalance>> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(emptyList()) else repository.getCustomersWithBalance(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Transactions with customer details
    val allTransactions: StateFlow<List<TransactionWithCustomer>> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(emptyList()) else repository.getAllTransactionsWithCustomer(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Expenses
    val allExpenses: StateFlow<List<ExpenseEntity>> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(emptyList()) else repository.getExpenses(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Summary
    val dashboardSummary: StateFlow<DashboardSummary> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(DashboardSummary()) else repository.getDashboardSummary(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    // Expense Categories Breakdown
    val categoryExpenses: StateFlow<List<CategoryExpenseSummary>> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(emptyList()) else repository.getCategoryExpenseSummary(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cashbook State
    val cashbookEntries: StateFlow<List<CashbookEntity>> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(emptyList()) else repository.getCashbookEntries(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashbookSummary: StateFlow<CashbookSummary> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(CashbookSummary()) else repository.getCashbookSummary(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashbookSummary())

    val cashbookDayGroups: StateFlow<List<CashbookDayGroup>> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(emptyList()) else repository.getCashbookDayGroups(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Items / Inventory State
    val itemsList: StateFlow<List<ItemEntity>> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(emptyList()) else repository.getItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recycle Bin State
    val recycleBinItems: StateFlow<List<RecycleBinItem>> = activeBusinessId.flatMapLatest { id ->
        if (id <= 0) flowOf(emptyList()) else repository.getRecycleBinItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation and selection state
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    val selectedCustomerWithBalance: StateFlow<CustomerWithBalance?> = _selectedCustomerId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getCustomerWithBalance(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedCustomerTransactions: StateFlow<List<TransactionEntity>> = _selectedCustomerId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getCustomerTransactions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Staff Attendance & Advance State
    private val initialCal = Calendar.getInstance()
    private val _selectedStaffMonth = MutableStateFlow(
        initialCal.get(Calendar.YEAR) to initialCal.get(Calendar.MONTH)
    )
    val selectedStaffMonth: StateFlow<Pair<Int, Int>> = _selectedStaffMonth.asStateFlow()

    private val _selectedStaffId = MutableStateFlow<Long?>(null)
    val selectedStaffId: StateFlow<Long?> = _selectedStaffId.asStateFlow()

    val staffWithSummary: StateFlow<List<StaffWithSummary>> = combine(
        activeBusinessId,
        _selectedStaffMonth
    ) { busId, (year, month) ->
        busId to StaffAttendanceCalculator.formatMonthPrefix(year, month)
    }.flatMapLatest { (busId, monthPrefix) ->
        if (busId <= 0) flowOf(emptyList())
        else repository.getStaffWithSummary(busId, monthPrefix, StaffAttendanceCalculator.todayDateKey())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedStaffSummary: StateFlow<StaffWithSummary?> = combine(
        _selectedStaffId,
        staffWithSummary
    ) { id, list ->
        if (id == null) null else list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedStaffEntity: StateFlow<StaffEntity?> = _selectedStaffId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getStaffById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedStaffDays: StateFlow<List<StaffDayRecord>> = combine(
        _selectedStaffId,
        _selectedStaffMonth
    ) { staffId, (year, month) ->
        Triple(staffId, year, month)
    }.flatMapLatest { (staffId, year, month) ->
        if (staffId == null) {
            flowOf(emptyList())
        } else {
            val monthPrefix = StaffAttendanceCalculator.formatMonthPrefix(year, month)
            repository.getStaffAttendanceForMonth(staffId, monthPrefix).map { entries ->
                StaffAttendanceCalculator.buildMonthDayRecords(year, month, entries)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @Volatile
    private var cachedBusinessName: String = "My Business"

    // Toast/Snackbar notifications
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val busId = repository.ensureDefaultBusiness()
            val targetId = if (prefs.activeBusinessId.value <= 0) {
                prefs.setActiveBusinessId(busId)
                busId
            } else {
                prefs.activeBusinessId.value
            }
            repository.getBusinessByIdSync(targetId)?.let {
                cachedBusinessName = it.name
            }
        }
    }

    private suspend fun resolveActiveBusinessId(): Long {
        val current = activeBusinessId.value
        if (current > 0) return current
        val busId = repository.ensureDefaultBusiness()
        prefs.setActiveBusinessId(busId)
        return busId
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // App Lock Actions
    fun unlockWithPin(enteredPin: String): Boolean {
        return if (prefs.verifyPin(enteredPin)) {
            _isUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun resetPinViaSecurity(answer: String, newPin: String): Boolean {
        val cleanNewPin = newPin.trim()
        if (cleanNewPin.length != 4 || !cleanNewPin.all { it.isDigit() }) {
            return false
        }
        val cleanedAnswer = answer.trim()
        if (cleanedAnswer.isBlank()) return false

        val knownBusName = (activeBusiness.value?.name ?: cachedBusinessName).trim()
        val matchesStored = prefs.verifySecurityAnswer(cleanedAnswer)
        val matchesShopName = knownBusName.isNotBlank() && knownBusName.equals(cleanedAnswer, ignoreCase = true)

        return if (matchesStored || matchesShopName) {
            prefs.setPin(cleanNewPin, true)
            _isUnlocked.value = true
            _userMessage.value = "PIN reset successfully"
            true
        } else {
            false
        }
    }

    fun setPin(pin: String, enabled: Boolean, securityAnswer: String = "") {
        if (enabled && (pin.length != 4 || !pin.all { it.isDigit() })) {
            _userMessage.value = "PIN must be exactly 4 digits"
            return
        }
        prefs.setPin(pin, enabled)
        if (enabled) {
            val fallbackAnswer = securityAnswer.trim().ifBlank {
                activeBusiness.value?.name ?: cachedBusinessName
            }
            if (fallbackAnswer.isNotBlank()) {
                prefs.setSecurityQuestion(prefs.getSecurityQuestion(), fallbackAnswer)
            }
            _userMessage.value = "4-digit PIN lock enabled"
        } else {
            _isUnlocked.value = true
            _userMessage.value = "PIN lock disabled"
        }
    }

    fun lockApp() {
        if (prefs.isPinEnabled.value) {
            _isUnlocked.value = false
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        return !prefs.isPinEnabled.value || prefs.verifyPin(enteredPin)
    }

    fun completeVerifiedLogin(
        accountLabel: String,
        shopName: String,
        updateBusinessPhone: Boolean,
        startFresh: Boolean = false
    ) {
        val cleanedLabel = accountLabel.trim()
        val cleanShopName = shopName.trim()
        val normalizedKey = PreferencesManager.normalizeAccountKey(cleanedLabel)
        val mappedBusId = if (!startFresh) prefs.getBusinessIdForAccount(cleanedLabel) else -1L
        val currentActiveId = prefs.activeBusinessId.value
        val currentOwner = prefs.getAccountOwnerForBusiness(currentActiveId)

        if (mappedBusId > 0L) {
            prefs.setActiveBusinessId(mappedBusId)
        } else if (startFresh || (currentActiveId > 0L && currentOwner.isNotBlank() && currentOwner != normalizedKey)) {
            prefs.setActiveBusinessId(-1L)
        }

        _selectedCustomerId.value = null
        _selectedStaffId.value = null
        prefs.loginWithPhone(cleanedLabel)
        _isUnlocked.value = true

        viewModelScope.launch {
            val mappedEntity = if (mappedBusId > 0L) repository.getBusinessByIdSync(mappedBusId) else null
            val targetBusinessId: Long = if (mappedEntity != null) {
                val updatedName = if (cleanShopName.isNotBlank()) cleanShopName else mappedEntity.name
                val updatedPhone = if (updateBusinessPhone && cleanedLabel.isNotBlank()) cleanedLabel else mappedEntity.phone
                cachedBusinessName = updatedName
                if (updatedName != mappedEntity.name || updatedPhone != mappedEntity.phone) {
                    repository.updateBusiness(
                        mappedEntity.copy(
                            name = updatedName,
                            phone = updatedPhone
                        )
                    )
                }
                prefs.setActiveBusinessId(mappedEntity.id)
                mappedEntity.id
            } else {
                val firstBusId = repository.ensureDefaultBusiness()
                val firstBus = repository.getBusinessByIdSync(firstBusId)
                val firstOwner = prefs.getAccountOwnerForBusiness(firstBusId)
                val firstHasData = repository.hasBusinessData(firstBusId)
                val canReuseFirst = !startFresh && firstBus != null &&
                    ((firstOwner.isBlank() && !firstHasData) || firstOwner == normalizedKey)

                if (canReuseFirst && firstBus != null) {
                    val updatedName = if (cleanShopName.isNotBlank()) cleanShopName else firstBus.name
                    val updatedPhone = if (updateBusinessPhone && cleanedLabel.isNotBlank()) cleanedLabel else firstBus.phone
                    val updatedOwner = if (!updateBusinessPhone && cleanedLabel != "Offline Ledger" && firstBus.ownerName.isBlank()) {
                        cleanedLabel
                    } else {
                        firstBus.ownerName
                    }
                    cachedBusinessName = updatedName
                    if (updatedName != firstBus.name || updatedPhone != firstBus.phone || updatedOwner != firstBus.ownerName) {
                        repository.updateBusiness(
                            firstBus.copy(
                                name = updatedName,
                                phone = updatedPhone,
                                ownerName = updatedOwner
                            )
                        )
                    }
                    prefs.setBusinessIdForAccount(cleanedLabel, firstBusId)
                    prefs.setActiveBusinessId(firstBusId)
                    firstBusId
                } else {
                    val newName = cleanShopName.ifBlank { "My Business" }
                    val newPhone = if (updateBusinessPhone) cleanedLabel else ""
                    val newOwner = if (!updateBusinessPhone && cleanedLabel != "Offline Ledger") cleanedLabel else ""
                    val newId = repository.createBusiness(
                        name = newName,
                        ownerName = newOwner,
                        phone = newPhone,
                        address = "",
                        gstNumber = ""
                    )
                    cachedBusinessName = newName
                    prefs.setBusinessIdForAccount(cleanedLabel, newId)
                    prefs.setActiveBusinessId(newId)
                    newId
                }
            }
            if (targetBusinessId > 0L) {
                _userMessage.value = "Signed in: $cleanedLabel"
            }
        }
    }

    fun skipLoginOffline(shopName: String, startFresh: Boolean = false): Boolean {
        if (prefs.isPinEnabled.value) return false
        completeVerifiedLogin(
            accountLabel = "Offline Ledger",
            shopName = shopName,
            updateBusinessPhone = false,
            startFresh = startFresh
        )
        return true
    }

    fun loginWithPhone(
        countryCode: String,
        phoneDigits: String,
        shopName: String,
        enteredPin: String,
        startFresh: Boolean = false
    ): Boolean {
        val cleanDigits = FirebaseAuthHelper.normalizePhoneDigits(countryCode, phoneDigits)
        if (!FirebaseAuthHelper.isValidPhoneDigits(cleanDigits)) {
            return false
        }
        if (prefs.isPinEnabled.value && !prefs.verifyPin(enteredPin)) {
            return false
        }
        val fullPhone = FirebaseAuthHelper.formatDisplayPhone(countryCode, cleanDigits)
        completeVerifiedLogin(
            accountLabel = fullPhone,
            shopName = shopName,
            updateBusinessPhone = true,
            startFresh = startFresh
        )
        return true
    }

    fun logout() {
        _selectedCustomerId.value = null
        _selectedStaffId.value = null
        prefs.logout()
        _isUnlocked.value = !prefs.isPinEnabled.value
    }

    // Business Selection
    fun selectBusiness(id: Long) {
        prefs.setActiveBusinessId(id)
        _selectedCustomerId.value = null
        _selectedStaffId.value = null
    }

    fun createBusiness(name: String, owner: String, phone: String, address: String, gst: String) {
        viewModelScope.launch {
            val id = repository.createBusiness(name, owner, phone, address, gst)
            prefs.setActiveBusinessId(id)
            _userMessage.value = "Business created: $name"
        }
    }

    fun updateBusiness(business: BusinessEntity) {
        viewModelScope.launch {
            repository.updateBusiness(business)
            _userMessage.value = "Business updated successfully"
        }
    }

    // Customer Selection
    fun selectCustomer(customerId: Long?) {
        _selectedCustomerId.value = customerId
    }

    // Customer CRUD
    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        openingBalance: Double,
        openingBalanceType: String,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            val id = repository.addCustomer(
                businessId = currentBusId,
                name = name,
                phone = phone,
                address = address,
                openingBalance = openingBalance,
                openingBalanceType = openingBalanceType
            )
            _userMessage.value = "Customer added: $name"
            onSuccess(id)
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            _userMessage.value = "Customer updated"
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            if (_selectedCustomerId.value == customerId) {
                _selectedCustomerId.value = null
            }
            _userMessage.value = "Customer deleted"
        }
    }

    // Transaction CRUD
    fun addTransaction(
        customerId: Long,
        amount: Double,
        type: TransactionType,
        paymentMethod: String,
        description: String,
        billNumber: String,
        transactionDate: Long,
        dueDate: Long? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            repository.addTransaction(
                businessId = currentBusId,
                customerId = customerId,
                amount = amount,
                type = type,
                paymentMethod = paymentMethod,
                description = description,
                billNumber = billNumber,
                transactionDate = transactionDate,
                dueDate = dueDate
            )
            val actionName = if (type == TransactionType.CREDIT) "Credit (Gave) entry added" else "Payment received added"
            _userMessage.value = actionName
            onSuccess()
        }
    }

    fun updateTransaction(transaction: TransactionEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            _userMessage.value = "Transaction updated"
            onSuccess()
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            _userMessage.value = "Transaction deleted"
        }
    }

    // Expenses CRUD
    fun addExpense(
        amount: Double,
        category: String,
        paymentMethod: String,
        description: String,
        expenseDate: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            repository.addExpense(
                businessId = currentBusId,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                description = description,
                expenseDate = expenseDate
            )
            _userMessage.value = "Expense recorded"
            onSuccess()
        }
    }

    fun updateExpense(expense: ExpenseEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            _userMessage.value = "Expense updated"
            onSuccess()
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            _userMessage.value = "Expense deleted"
        }
    }

    // Staff & Attendance Management
    fun selectStaff(staffId: Long?) {
        _selectedStaffId.value = staffId
    }

    fun previousStaffMonth() {
        val (year, month) = _selectedStaffMonth.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, -1)
        }
        _selectedStaffMonth.value = cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun nextStaffMonth() {
        val (year, month) = _selectedStaffMonth.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, 1)
        }
        _selectedStaffMonth.value = cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun addStaff(
        name: String,
        phone: String,
        role: String,
        salaryType: String,
        salaryAmount: Double,
        overtimeRate: Double
    ) {
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            repository.addStaff(
                businessId = currentBusId,
                name = name,
                phone = phone,
                role = role,
                salaryType = salaryType,
                salaryAmount = salaryAmount,
                overtimeRate = overtimeRate
            )
            _userMessage.value = "Staff added: $name"
        }
    }

    fun updateStaff(staff: StaffEntity) {
        viewModelScope.launch {
            repository.updateStaff(staff)
            _userMessage.value = "Staff updated"
        }
    }

    fun deleteStaff(staffId: Long) {
        viewModelScope.launch {
            repository.deleteStaff(staffId)
            if (_selectedStaffId.value == staffId) {
                _selectedStaffId.value = null
            }
            _userMessage.value = "Staff removed"
        }
    }

    fun toggleStaffAttendance(staffId: Long, dateKey: String, targetStatus: AttendanceStatus) {
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            repository.setStaffAttendanceStatus(
                businessId = currentBusId,
                staffId = staffId,
                dateKey = dateKey,
                targetStatus = targetStatus
            )
        }
    }

    fun setStaffAdvance(staffId: Long, dateKey: String, amount: Double, note: String) {
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            repository.setStaffAdvance(
                businessId = currentBusId,
                staffId = staffId,
                dateKey = dateKey,
                amount = amount,
                note = note
            )
            _userMessage.value = if (amount > 0.0) "Advance updated" else "Advance cleared"
        }
    }

    // Demo Data
    fun loadDemoData() {
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            repository.populateDemoData(currentBusId)
            _userMessage.value = "Sample shop demo data loaded!"
        }
    }

    // Backup & Restore
    fun exportBackup(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentBusId = resolveActiveBusinessId()
                val json = repository.exportBackupJson(currentBusId)
                onExportReady(json)
            } catch (e: Exception) {
                _userMessage.value = "Failed to export backup"
            }
        }
    }

    fun restoreBackup(jsonStr: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            val success = repository.importBackupJson(currentBusId, jsonStr)
            if (success) {
                _userMessage.value = "Backup restored successfully!"
            } else {
                _userMessage.value = "Failed to restore backup. Check JSON content."
            }
            onResult(success)
        }
    }

    // Preferences
    fun setLanguage(lang: String) {
        prefs.setLanguage(lang)
    }

    fun setCurrency(currency: String) {
        prefs.setCurrency(currency)
    }

    fun setTheme(theme: String) {
        prefs.setThemeMode(theme)
    }

    fun savePaymentSettings(upiId: String, defaultMode: String) {
        prefs.setPaymentSettings(upiId, defaultMode)
        _userMessage.value = "Payment settings saved"
    }

    fun markCashbookGuideSeen() {
        prefs.markCashbookGuideSeen()
    }

    // Cashbook CRUD
    fun addCashbookEntry(
        amount: Double,
        type: CashbookType,
        paymentMode: CashbookPaymentMode,
        note: String,
        entryDate: Long = System.currentTimeMillis(),
        onSuccess: (Long) -> Unit = {}
    ) {
        if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) {
            _userMessage.value = "Amount must be greater than zero"
            return
        }
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            val id = repository.addCashbookEntry(
                businessId = currentBusId,
                amount = amount,
                type = type,
                paymentMode = paymentMode,
                note = note,
                entryDate = entryDate
            )
            _userMessage.value = "${type.value} entry saved"
            onSuccess(id)
        }
    }

    fun updateCashbookEntry(entry: CashbookEntity, onSuccess: () -> Unit = {}) {
        if (entry.amount <= 0.0 || entry.amount.isNaN() || entry.amount.isInfinite()) {
            _userMessage.value = "Amount must be greater than zero"
            return
        }
        viewModelScope.launch {
            repository.updateCashbookEntry(entry)
            _userMessage.value = "Cashbook entry updated"
            onSuccess()
        }
    }

    fun deleteCashbookEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteCashbookEntry(entryId)
            _userMessage.value = "Entry moved to Recycle Bin"
        }
    }

    // Items CRUD
    fun addItem(
        name: String,
        unit: String,
        salePrice: Double,
        purchasePrice: Double,
        stockQty: Double,
        lowStockAlert: Double
    ) {
        if (name.isBlank()) {
            _userMessage.value = "Item name is required"
            return
        }
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            repository.addItem(
                businessId = currentBusId,
                name = name,
                unit = unit,
                salePrice = salePrice,
                purchasePrice = purchasePrice,
                stockQty = stockQty,
                lowStockAlert = lowStockAlert
            )
            _userMessage.value = "Item added: ${name.trim()}"
        }
    }

    fun updateItem(item: ItemEntity) {
        if (item.name.isBlank()) {
            _userMessage.value = "Item name is required"
            return
        }
        viewModelScope.launch {
            repository.updateItem(item)
            _userMessage.value = "Item updated"
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
            _userMessage.value = "Item deleted"
        }
    }

    // Recycle Bin actions
    fun restoreRecycleBinItem(item: RecycleBinItem) {
        viewModelScope.launch {
            repository.restoreRecycleBinItem(item)
            _userMessage.value = "Restored: ${item.title}"
        }
    }

    fun permanentlyDeleteRecycleBinItem(item: RecycleBinItem) {
        viewModelScope.launch {
            repository.permanentlyDeleteRecycleBinItem(item)
            _userMessage.value = "Permanently deleted"
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            val currentBusId = resolveActiveBusinessId()
            repository.emptyRecycleBin(currentBusId)
            _userMessage.value = "Recycle Bin emptied"
        }
    }

    // Delete Khata (Business)
    fun deleteBusiness(business: BusinessEntity) {
        viewModelScope.launch {
            repository.deleteBusiness(business)
            prefs.clearBusinessOwnerMapping(business.id)
            _selectedCustomerId.value = null
            _selectedStaffId.value = null
            val nextId = repository.ensureDefaultBusiness()
            prefs.setActiveBusinessId(nextId)
            val nextBus = repository.getBusinessByIdSync(nextId)
            cachedBusinessName = nextBus?.name ?: "My Business"
            _userMessage.value = "Khata deleted"
        }
    }
}

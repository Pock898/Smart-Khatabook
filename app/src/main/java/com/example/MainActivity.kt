package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.FirebaseAuthHelper
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.StaffEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.CashbookEntryUi
import com.example.data.model.CashbookPaymentMode
import com.example.data.model.CashbookType
import com.example.data.model.CustomerWithBalance
import com.example.data.model.StaffAttendanceCalculator
import com.example.data.model.StaffDayRecord
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCustomer
import com.example.ui.CustomerViewModel
import com.example.ui.ExpenseViewModel
import com.example.ui.MainViewModel
import com.example.ui.TransactionViewModel
import com.example.ui.components.AddCustomerDialog
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AddStaffDialog
import com.example.ui.components.BusinessProfileDialog
import com.example.ui.components.KhatabookTopBar
import com.example.ui.components.NewTransactionSheet
import com.example.ui.components.PaymentReminderDialog
import com.example.ui.components.SecurityLockScreen
import com.example.ui.components.StaffAdvanceDialog
import com.example.ui.components.StaffReportDialog
import com.example.ui.components.TransactionReceiptDialog
import com.example.ui.localization.AppStrings
import com.example.ui.pdf.ReportExporter
import com.example.ui.screens.CashbookEntryScreen
import com.example.ui.screens.CashbookScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.ItemsScreen
import com.example.ui.screens.PaymentSettingsScreen
import com.example.ui.screens.PhoneLoginScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StaffDetailScreen
import com.example.ui.screens.StaffScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.SmartKhatabookTheme

enum class AppNavTab {
    DASHBOARD,
    CUSTOMERS,
    STAFF,
    ACCOUNTS,
    SETTINGS
}

enum class AccountsSubTab {
    TRANSACTIONS,
    EXPENSES,
    REPORTS
}

enum class OverlayRoute {
    NONE,
    CASHBOOK,
    CASHBOOK_ENTRY,
    ITEMS,
    RECYCLE_BIN,
    PAYMENT_SETTINGS
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val customerViewModel: CustomerViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            SmartKhatabookTheme(darkTheme = isDark) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
                val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
                val currentLang by viewModel.language.collectAsStateWithLifecycle()
                val loggedInPhone by viewModel.loggedInPhone.collectAsStateWithLifecycle()
                val activeBusiness by viewModel.activeBusiness.collectAsStateWithLifecycle()
                val strings = remember(currentLang) { AppStrings.get(currentLang) }

                if (!isLoggedIn) {
                    val phonePrefill = loggedInPhone
                        .takeIf { it.isNotBlank() && !it.contains("@") && !it.any { ch -> ch.isLetter() } }
                        .orEmpty()
                    PhoneLoginScreen(
                        initialPhone = phonePrefill,
                        initialShopName = if (loggedInPhone.isNotBlank()) activeBusiness?.name.orEmpty() else "",
                        isPinEnabled = isPinEnabled,
                        securityQuestion = viewModel.prefs.getSecurityQuestion(),
                        currentLang = currentLang,
                        strings = strings,
                        onToggleLang = {
                            viewModel.setLanguage(if (currentLang == "en") "hi" else "en")
                        },
                        onLoginSubmit = { countryCode, phoneDigits, shopName, pin, startFresh ->
                            viewModel.loginWithPhone(countryCode, phoneDigits, shopName, pin, startFresh)
                        },
                        onGoogleLoginSuccess = { accountLabel, shopName, startFresh ->
                            viewModel.completeVerifiedLogin(
                                accountLabel = accountLabel,
                                shopName = shopName,
                                updateBusinessPhone = false,
                                startFresh = startFresh
                            )
                        },
                        onResetPinViaSecurity = { answer, newPin ->
                            viewModel.resetPinViaSecurity(answer, newPin)
                        },
                        onSkipOffline = { shopName, startFresh ->
                            viewModel.skipLoginOffline(shopName, startFresh)
                        }
                    )
                } else if (!isUnlocked) {
                    SecurityLockScreen(
                        onUnlockSuccess = {},
                        onVerifyPin = { pin -> viewModel.unlockWithPin(pin) },
                        onResetPinViaSecurity = { answer, newPin -> viewModel.resetPinViaSecurity(answer, newPin) },
                        securityQuestion = viewModel.prefs.getSecurityQuestion()
                    )
                } else {
                    KhatabookMainApp(
                        viewModel = viewModel,
                        customerViewModel = customerViewModel,
                        transactionViewModel = transactionViewModel,
                        expenseViewModel = expenseViewModel
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            viewModel.lockApp()
        }
    }
}

@Composable
fun KhatabookMainApp(
    viewModel: MainViewModel,
    customerViewModel: CustomerViewModel,
    transactionViewModel: TransactionViewModel,
    expenseViewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    // State collections
    val currentLang by viewModel.language.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
    val loggedInPhone by viewModel.loggedInPhone.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val activeBusiness by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val allBusinesses by viewModel.allBusinesses.collectAsStateWithLifecycle()
    val customers by customerViewModel.customersWithBalance.collectAsStateWithLifecycle()
    val suppliers by customerViewModel.suppliersWithBalance.collectAsStateWithLifecycle()
    val allTransactions by transactionViewModel.allTransactions.collectAsStateWithLifecycle()
    val allExpenses by expenseViewModel.allExpenses.collectAsStateWithLifecycle()
    val dashboardSummary by transactionViewModel.dashboardSummary.collectAsStateWithLifecycle()
    val categoryExpenses by expenseViewModel.categoryExpenses.collectAsStateWithLifecycle()

    // Cashbook, Items, Recycle Bin & Payment Settings states
    val cashbookSummary by viewModel.cashbookSummary.collectAsStateWithLifecycle()
    val cashbookDayGroups by viewModel.cashbookDayGroups.collectAsStateWithLifecycle()
    val cashbookEntries by viewModel.cashbookEntries.collectAsStateWithLifecycle()
    val itemsList by viewModel.itemsList.collectAsStateWithLifecycle()
    val recycleBinItems by viewModel.recycleBinItems.collectAsStateWithLifecycle()
    val upiId by viewModel.upiId.collectAsStateWithLifecycle()
    val defaultCashbookMode by viewModel.defaultCashbookMode.collectAsStateWithLifecycle()
    val hasSeenCashbookGuide by viewModel.hasSeenCashbookGuide.collectAsStateWithLifecycle()

    val selectedCustomerId by customerViewModel.selectedCustomerId.collectAsStateWithLifecycle()
    val selectedCustomerWithBalance by customerViewModel.selectedCustomer.collectAsStateWithLifecycle()
    val selectedCustomerTransactions by transactionViewModel.customerTransactions.collectAsStateWithLifecycle()

    // Staff states
    val staffList by viewModel.staffWithSummary.collectAsStateWithLifecycle()
    val selectedStaffMonth by viewModel.selectedStaffMonth.collectAsStateWithLifecycle()
    val selectedStaffId by viewModel.selectedStaffId.collectAsStateWithLifecycle()
    val selectedStaffSummary by viewModel.selectedStaffSummary.collectAsStateWithLifecycle()
    val selectedStaffEntity by viewModel.selectedStaffEntity.collectAsStateWithLifecycle()
    val selectedStaffDays by viewModel.selectedStaffDays.collectAsStateWithLifecycle()

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val customerMessage by customerViewModel.userMessage.collectAsStateWithLifecycle()
    val transactionMessage by transactionViewModel.userMessage.collectAsStateWithLifecycle()
    val expenseMessage by expenseViewModel.userMessage.collectAsStateWithLifecycle()

    val strings = remember(currentLang) { AppStrings.get(currentLang) }

    var currentTab by remember { mutableStateOf(AppNavTab.DASHBOARD) }
    var accountsSubTab by remember { mutableStateOf(AccountsSubTab.TRANSACTIONS) }
    var overlayRoute by remember { mutableStateOf(OverlayRoute.NONE) }
    var cashbookEntryType by remember { mutableStateOf(CashbookType.OUT) }
    var editingCashbookEntry by remember { mutableStateOf<CashbookEntryUi?>(null) }

    // Dialog & Sheet States
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var addCustomerPartyType by remember { mutableStateOf("CUSTOMER") }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }

    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var addTxnInitialCustomerId by remember { mutableStateOf<Long?>(null) }
    var addTxnInitialType by remember { mutableStateOf(TransactionType.CREDIT) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }

    var selectedReceiptTxn by remember { mutableStateOf<TransactionWithCustomer?>(null) }
    var reminderCustomer by remember { mutableStateOf<CustomerWithBalance?>(null) }

    var showNewBusinessDialog by remember { mutableStateOf(false) }

    // Staff Dialog States
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var staffToEdit by remember { mutableStateOf<StaffEntity?>(null) }
    var advanceTargetDay by remember { mutableStateOf<StaffDayRecord?>(null) }
    var showStaffReportDialog by remember { mutableStateOf(false) }

    fun selectCustomerEverywhere(id: Long?) {
        customerViewModel.selectCustomer(id)
        transactionViewModel.selectCustomer(id)
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(customerMessage) {
        customerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            customerViewModel.clearUserMessage()
        }
    }

    LaunchedEffect(transactionMessage) {
        transactionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            transactionViewModel.clearUserMessage()
        }
    }

    LaunchedEffect(expenseMessage) {
        expenseMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            expenseViewModel.clearUserMessage()
        }
    }

    BackHandler(
        enabled = overlayRoute != OverlayRoute.NONE ||
            selectedStaffId != null ||
            selectedCustomerId != null ||
            currentTab != AppNavTab.DASHBOARD
    ) {
        when {
            overlayRoute == OverlayRoute.CASHBOOK_ENTRY -> {
                editingCashbookEntry = null
                overlayRoute = OverlayRoute.CASHBOOK
            }
            overlayRoute != OverlayRoute.NONE -> {
                overlayRoute = OverlayRoute.NONE
            }
            selectedStaffId != null -> viewModel.selectStaff(null)
            selectedCustomerId != null -> selectCustomerEverywhere(null)
            currentTab != AppNavTab.DASHBOARD -> currentTab = AppNavTab.DASHBOARD
        }
    }

    if (overlayRoute == OverlayRoute.CASHBOOK_ENTRY) {
        CashbookEntryScreen(
            entryType = cashbookEntryType,
            initialMode = CashbookPaymentMode.from(defaultCashbookMode),
            existingEntry = editingCashbookEntry,
            currency = currency,
            strings = strings,
            onBackClick = {
                editingCashbookEntry = null
                overlayRoute = OverlayRoute.CASHBOOK
            },
            onSaveEntry = { amount, type, mode, note ->
                val editing = editingCashbookEntry
                if (editing == null) {
                    viewModel.addCashbookEntry(amount = amount, type = type, paymentMode = mode, note = note)
                } else {
                    viewModel.updateCashbookEntry(
                        com.example.data.local.CashbookEntity(
                            id = editing.id,
                            businessId = editing.businessId,
                            amount = amount,
                            type = type.value,
                            paymentMode = mode.value,
                            note = note,
                            entryDate = editing.entryDate,
                            createdAt = editing.createdAt
                        )
                    )
                }
                editingCashbookEntry = null
                overlayRoute = OverlayRoute.CASHBOOK
            }
        )
    } else if (overlayRoute == OverlayRoute.CASHBOOK) {
        CashbookScreen(
            business = activeBusiness,
            summary = cashbookSummary,
            dayGroups = cashbookDayGroups,
            rawEntries = cashbookEntries,
            currency = currency,
            strings = strings,
            hasSeenGuide = hasSeenCashbookGuide,
            onDismissGuide = { viewModel.markCashbookGuideSeen() },
            onBackClick = { overlayRoute = OverlayRoute.NONE },
            onAddOutClick = {
                editingCashbookEntry = null
                cashbookEntryType = CashbookType.OUT
                overlayRoute = OverlayRoute.CASHBOOK_ENTRY
            },
            onAddInClick = {
                editingCashbookEntry = null
                cashbookEntryType = CashbookType.IN
                overlayRoute = OverlayRoute.CASHBOOK_ENTRY
            },
            onEditEntry = { entry ->
                editingCashbookEntry = entry
                cashbookEntryType = entry.type
                overlayRoute = OverlayRoute.CASHBOOK_ENTRY
            },
            onDeleteEntry = { entry ->
                viewModel.deleteCashbookEntry(entry.id)
            }
        )
    } else if (overlayRoute == OverlayRoute.ITEMS) {
        ItemsScreen(
            items = itemsList,
            currency = currency,
            strings = strings,
            onBackClick = { overlayRoute = OverlayRoute.NONE },
            onAddItem = { name, unit, salePrice, purchasePrice, stockQty, lowAlert ->
                viewModel.addItem(name, unit, salePrice, purchasePrice, stockQty, lowAlert)
            },
            onUpdateItem = { viewModel.updateItem(it) },
            onDeleteItem = { viewModel.deleteItem(it) }
        )
    } else if (overlayRoute == OverlayRoute.RECYCLE_BIN) {
        RecycleBinScreen(
            items = recycleBinItems,
            currency = currency,
            strings = strings,
            onBackClick = { overlayRoute = OverlayRoute.NONE },
            onRestoreItem = { viewModel.restoreRecycleBinItem(it) },
            onDeleteForever = { viewModel.permanentlyDeleteRecycleBinItem(it) },
            onEmptyBin = { viewModel.emptyRecycleBin() }
        )
    } else if (overlayRoute == OverlayRoute.PAYMENT_SETTINGS) {
        PaymentSettingsScreen(
            business = activeBusiness,
            initialUpiId = upiId,
            initialDefaultMode = defaultCashbookMode,
            strings = strings,
            onBackClick = { overlayRoute = OverlayRoute.NONE },
            onSaveSettings = { newUpi, newMode ->
                viewModel.savePaymentSettings(newUpi, newMode)
            }
        )
    } else if (selectedStaffSummary != null) {
        val staff = selectedStaffSummary!!
        StaffDetailScreen(
            staff = staff,
            days = selectedStaffDays,
            selectedMonth = selectedStaffMonth,
            currency = currency,
            strings = strings,
            onBackClick = { viewModel.selectStaff(null) },
            onEditStaffClick = {
                staffToEdit = selectedStaffEntity ?: StaffEntity(
                    id = staff.id,
                    businessId = staff.businessId,
                    name = staff.name,
                    phone = staff.phone,
                    role = staff.role,
                    salaryType = staff.salaryType,
                    salaryAmount = staff.salaryAmount,
                    overtimeRate = staff.overtimeRate
                )
                showAddStaffDialog = true
            },
            onPreviousMonth = { viewModel.previousStaffMonth() },
            onNextMonth = { viewModel.nextStaffMonth() },
            onOpenReportClick = { showStaffReportDialog = true },
            onToggleAttendance = { dateKey, status ->
                viewModel.toggleStaffAttendance(staff.id, dateKey, status)
            },
            onAdvanceClick = { day ->
                advanceTargetDay = day
            }
        )
    } else if (selectedCustomerWithBalance != null) {
        val selectedCustomer = selectedCustomerWithBalance!!
        CustomerDetailScreen(
            customer = selectedCustomer,
            transactions = selectedCustomerTransactions,
            business = activeBusiness,
            currency = currency,
            strings = strings,
            onBackClick = { selectCustomerEverywhere(null) },
            onAddCreditClick = {
                addTxnInitialCustomerId = selectedCustomer.id
                addTxnInitialType = TransactionType.CREDIT
                transactionToEdit = null
                showAddTransactionDialog = true
            },
            onAddPaymentClick = {
                addTxnInitialCustomerId = selectedCustomer.id
                addTxnInitialType = TransactionType.PAYMENT
                transactionToEdit = null
                showAddTransactionDialog = true
            },
            onEditCustomerClick = {
                addCustomerPartyType = selectedCustomer.partyType
                customerToEdit = CustomerEntity(
                    id = selectedCustomer.id,
                    businessId = selectedCustomer.businessId,
                    name = selectedCustomer.name,
                    phone = selectedCustomer.phone,
                    address = selectedCustomer.address,
                    openingBalance = selectedCustomer.openingBalance,
                    openingBalanceType = selectedCustomer.openingBalanceType,
                    partyType = selectedCustomer.partyType
                )
                showAddCustomerDialog = true
            },
            onDeleteCustomerClick = {
                customerViewModel.deleteCustomer(selectedCustomer.id) {
                    transactionViewModel.selectCustomer(null)
                }
            },
            onEditTransaction = { txn ->
                transactionToEdit = txn
                addTxnInitialCustomerId = selectedCustomer.id
                addTxnInitialType = TransactionType.from(txn.type)
                showAddTransactionDialog = true
            },
            onDeleteTransaction = { txn ->
                transactionViewModel.deleteTransaction(txn.id)
            },
            onReminderClick = {
                reminderCustomer = selectedCustomer
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                KhatabookTopBar(
                    business = activeBusiness,
                    businesses = allBusinesses,
                    onSelectBusiness = {
                        selectCustomerEverywhere(null)
                        viewModel.selectBusiness(it)
                    },
                    onAddNewBusiness = { showNewBusinessDialog = true },
                    currentLang = currentLang,
                    onToggleLang = {
                        viewModel.setLanguage(if (currentLang == "en") "hi" else "en")
                    },
                    isPinEnabled = isPinEnabled,
                    onLockApp = { viewModel.lockApp() },
                    title = when (currentTab) {
                        AppNavTab.DASHBOARD -> null
                        AppNavTab.CUSTOMERS -> strings.parties
                        AppNavTab.STAFF -> strings.staffAttendance
                        AppNavTab.ACCOUNTS -> when (accountsSubTab) {
                            AccountsSubTab.TRANSACTIONS -> strings.transactions
                            AccountsSubTab.EXPENSES -> strings.expenses
                            AccountsSubTab.REPORTS -> strings.reports
                        }
                        AppNavTab.SETTINGS -> strings.more
                    }
                )
            },
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    NavigationBarItem(
                        selected = currentTab == AppNavTab.DASHBOARD,
                        onClick = { currentTab = AppNavTab.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = strings.dashboard) },
                        label = { Text(strings.dashboard, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentTab == AppNavTab.CUSTOMERS,
                        onClick = { currentTab = AppNavTab.CUSTOMERS },
                        icon = { Icon(Icons.Default.People, contentDescription = strings.parties) },
                        label = { Text(strings.parties, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_customers")
                    )
                    NavigationBarItem(
                        selected = currentTab == AppNavTab.STAFF,
                        onClick = { currentTab = AppNavTab.STAFF },
                        icon = { Icon(Icons.Default.Badge, contentDescription = strings.staff) },
                        label = { Text(strings.staff, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_staff")
                    )
                    NavigationBarItem(
                        selected = currentTab == AppNavTab.ACCOUNTS,
                        onClick = { currentTab = AppNavTab.ACCOUNTS },
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = strings.accounts) },
                        label = { Text(strings.accounts, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_accounts")
                    )
                    NavigationBarItem(
                        selected = currentTab == AppNavTab.SETTINGS,
                        onClick = { currentTab = AppNavTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = strings.more) },
                        label = { Text(strings.more, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AppNavTab.DASHBOARD -> {
                        DashboardScreen(
                            summary = dashboardSummary,
                            customers = customers,
                            recentTransactions = allTransactions,
                            staffList = staffList,
                            business = activeBusiness,
                            currency = currency,
                            strings = strings,
                            onNavigateToCustomers = { currentTab = AppNavTab.CUSTOMERS },
                            onNavigateToStaff = { currentTab = AppNavTab.STAFF },
                            onNavigateToTransactions = {
                                accountsSubTab = AccountsSubTab.TRANSACTIONS
                                currentTab = AppNavTab.ACCOUNTS
                            },
                            onNavigateToExpenses = {
                                accountsSubTab = AccountsSubTab.EXPENSES
                                currentTab = AppNavTab.ACCOUNTS
                            },
                            onNavigateToReports = {
                                accountsSubTab = AccountsSubTab.REPORTS
                                currentTab = AppNavTab.ACCOUNTS
                            },
                            onCustomerClick = { cust -> selectCustomerEverywhere(cust.id) },
                            onReminderClick = { cust -> reminderCustomer = cust },
                            onAddCustomerClick = {
                                addCustomerPartyType = "CUSTOMER"
                                customerToEdit = null
                                showAddCustomerDialog = true
                            },
                            onAddCreditClick = {
                                addTxnInitialCustomerId = null
                                addTxnInitialType = TransactionType.CREDIT
                                transactionToEdit = null
                                showAddTransactionDialog = true
                            },
                            onAddPaymentClick = {
                                addTxnInitialCustomerId = null
                                addTxnInitialType = TransactionType.PAYMENT
                                transactionToEdit = null
                                showAddTransactionDialog = true
                            },
                            onAddExpenseClick = {
                                expenseToEdit = null
                                showAddExpenseDialog = true
                            },
                            onTransactionClick = { txn -> selectedReceiptTxn = txn },
                            onEditTransaction = { txn ->
                                transactionToEdit = TransactionEntity(
                                    id = txn.transactionId,
                                    businessId = txn.businessId,
                                    customerId = txn.customerId,
                                    amount = txn.amount,
                                    type = txn.type.value,
                                    paymentMethod = txn.paymentMethod,
                                    description = txn.description,
                                    billNumber = txn.billNumber,
                                    transactionDate = txn.transactionDate,
                                    dueDate = txn.dueDate,
                                    createdAt = txn.createdAt
                                )
                                addTxnInitialCustomerId = txn.customerId
                                addTxnInitialType = txn.type
                                showAddTransactionDialog = true
                            },
                            onDeleteTransaction = { txn ->
                                transactionViewModel.deleteTransaction(txn.transactionId)
                            }
                        )
                    }

                    AppNavTab.CUSTOMERS -> {
                        CustomersScreen(
                            customers = customers,
                            suppliers = suppliers,
                            currency = currency,
                            strings = strings,
                            onCustomerClick = { cust -> selectCustomerEverywhere(cust.id) },
                            onAddCustomerClick = {
                                addCustomerPartyType = "CUSTOMER"
                                customerToEdit = null
                                showAddCustomerDialog = true
                            },
                            onAddSupplierClick = {
                                addCustomerPartyType = "SUPPLIER"
                                customerToEdit = null
                                showAddCustomerDialog = true
                            },
                            onOpenCashbookClick = {
                                overlayRoute = OverlayRoute.CASHBOOK
                            },
                            onViewReportsClick = {
                                accountsSubTab = AccountsSubTab.REPORTS
                                currentTab = AppNavTab.ACCOUNTS
                            },
                            onReminderClick = { cust -> reminderCustomer = cust }
                        )
                    }

                    AppNavTab.STAFF -> {
                        StaffScreen(
                            staffList = staffList,
                            selectedMonth = selectedStaffMonth,
                            currency = currency,
                            strings = strings,
                            onPreviousMonth = { viewModel.previousStaffMonth() },
                            onNextMonth = { viewModel.nextStaffMonth() },
                            onStaffClick = { staff -> viewModel.selectStaff(staff.id) },
                            onAddStaffClick = {
                                staffToEdit = null
                                showAddStaffDialog = true
                            },
                            onQuickMarkToday = { staffId, status ->
                                viewModel.toggleStaffAttendance(
                                    staffId = staffId,
                                    dateKey = StaffAttendanceCalculator.todayDateKey(),
                                    targetStatus = status
                                )
                            }
                        )
                    }

                    AppNavTab.ACCOUNTS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TabRow(
                                selectedTabIndex = accountsSubTab.ordinal,
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Tab(
                                    selected = accountsSubTab == AccountsSubTab.TRANSACTIONS,
                                    onClick = { accountsSubTab = AccountsSubTab.TRANSACTIONS },
                                    text = { Text(strings.transactions, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                                    modifier = Modifier.testTag("accounts_subtab_transactions")
                                )
                                Tab(
                                    selected = accountsSubTab == AccountsSubTab.EXPENSES,
                                    onClick = { accountsSubTab = AccountsSubTab.EXPENSES },
                                    text = { Text(strings.expenses, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                                    modifier = Modifier.testTag("accounts_subtab_expenses")
                                )
                                Tab(
                                    selected = accountsSubTab == AccountsSubTab.REPORTS,
                                    onClick = { accountsSubTab = AccountsSubTab.REPORTS },
                                    text = { Text(strings.reports, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                                    modifier = Modifier.testTag("accounts_subtab_reports")
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                when (accountsSubTab) {
                                    AccountsSubTab.TRANSACTIONS -> {
                                        TransactionsScreen(
                                            transactions = allTransactions,
                                            currency = currency,
                                            strings = strings,
                                            onAddTransactionClick = {
                                                addTxnInitialCustomerId = null
                                                addTxnInitialType = TransactionType.CREDIT
                                                transactionToEdit = null
                                                showAddTransactionDialog = true
                                            },
                                            onEditTransaction = { txn ->
                                                transactionToEdit = TransactionEntity(
                                                    id = txn.transactionId,
                                                    businessId = txn.businessId,
                                                    customerId = txn.customerId,
                                                    amount = txn.amount,
                                                    type = txn.type.value,
                                                    paymentMethod = txn.paymentMethod,
                                                    description = txn.description,
                                                    billNumber = txn.billNumber,
                                                    transactionDate = txn.transactionDate,
                                                    dueDate = txn.dueDate,
                                                    createdAt = txn.createdAt
                                                )
                                                addTxnInitialCustomerId = txn.customerId
                                                addTxnInitialType = txn.type
                                                showAddTransactionDialog = true
                                            },
                                            onDeleteTransaction = { txn ->
                                                transactionViewModel.deleteTransaction(txn.transactionId)
                                            },
                                            onViewReceipt = { txn -> selectedReceiptTxn = txn },
                                            onShareTransaction = { txn -> selectedReceiptTxn = txn }
                                        )
                                    }

                                    AccountsSubTab.EXPENSES -> {
                                        ExpensesScreen(
                                            expenses = allExpenses,
                                            currency = currency,
                                            strings = strings,
                                            onAddExpenseClick = {
                                                expenseToEdit = null
                                                showAddExpenseDialog = true
                                            },
                                            onEditExpense = { exp ->
                                                expenseToEdit = exp
                                                showAddExpenseDialog = true
                                            },
                                            onDeleteExpense = { exp ->
                                                expenseViewModel.deleteExpense(exp.id)
                                            }
                                        )
                                    }

                                    AccountsSubTab.REPORTS -> {
                                        ReportsScreen(
                                            business = activeBusiness,
                                            customers = customers,
                                            suppliers = suppliers,
                                            cashbookSummary = cashbookSummary,
                                            transactions = allTransactions,
                                            expenses = allExpenses,
                                            categoryExpenses = categoryExpenses,
                                            currency = currency,
                                            strings = strings
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AppNavTab.SETTINGS -> {
                        SettingsScreen(
                            activeBusiness = activeBusiness,
                            allBusinesses = allBusinesses,
                            loggedInPhone = loggedInPhone,
                            currentLang = currentLang,
                            currentCurrency = currency,
                            isPinEnabled = isPinEnabled,
                            currentTheme = themeMode,
                            upiId = upiId,
                            recycleBinCount = recycleBinItems.size,
                            strings = strings,
                            onSelectBusiness = {
                                selectCustomerEverywhere(null)
                                viewModel.selectBusiness(it)
                            },
                            onUpdateBusiness = { viewModel.updateBusiness(it) },
                            onCreateBusiness = { name, owner, phone, address, gst ->
                                viewModel.createBusiness(name, owner, phone, address, gst)
                            },
                            onDeleteBusiness = { viewModel.deleteBusiness(it) },
                            onSetLanguage = { viewModel.setLanguage(it) },
                            onSetCurrency = { viewModel.setCurrency(it) },
                            onSetTheme = { viewModel.setTheme(it) },
                            onSetPin = { pin, enabled, answer -> viewModel.setPin(pin, enabled, answer) },
                            onExportBackup = { onReady -> viewModel.exportBackup(onReady) },
                            onRestoreBackup = { json, onResult -> viewModel.restoreBackup(json, onResult) },
                            onLoadDemoData = { viewModel.loadDemoData() },
                            onOpenCashbook = { overlayRoute = OverlayRoute.CASHBOOK },
                            onOpenBills = {
                                accountsSubTab = AccountsSubTab.EXPENSES
                                currentTab = AppNavTab.ACCOUNTS
                            },
                            onOpenItems = { overlayRoute = OverlayRoute.ITEMS },
                            onOpenStaff = { currentTab = AppNavTab.STAFF },
                            onOpenCollection = { currentTab = AppNavTab.CUSTOMERS },
                            onOpenReports = {
                                accountsSubTab = AccountsSubTab.REPORTS
                                currentTab = AppNavTab.ACCOUNTS
                            },
                            onOpenDashboard = { currentTab = AppNavTab.DASHBOARD },
                            onOpenPaymentSettings = { overlayRoute = OverlayRoute.PAYMENT_SETTINGS },
                            onOpenRecycleBin = { overlayRoute = OverlayRoute.RECYCLE_BIN },
                            onLogout = {
                                FirebaseAuthHelper.signOut(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onSignOutComplete = {
                                        selectCustomerEverywhere(null)
                                        customerViewModel.setSearchQuery("")
                                        customerViewModel.setFilterType(null)
                                        expenseViewModel.setCategoryFilter(null)
                                        viewModel.logout()
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Customer or Supplier Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            strings = strings,
            currency = currency,
            existingCustomer = customerToEdit,
            partyType = customerToEdit?.partyType ?: addCustomerPartyType,
            onDismiss = {
                showAddCustomerDialog = false
                customerToEdit = null
            },
            onSave = { name, phone, address, opening, balType ->
                if (customerToEdit == null) {
                    customerViewModel.addCustomer(
                        name = name,
                        phone = phone,
                        address = address,
                        openingBalance = opening,
                        openingBalanceType = balType,
                        partyType = addCustomerPartyType
                    )
                } else {
                    customerViewModel.updateCustomer(
                        customerToEdit!!.copy(
                            name = name,
                            phone = phone,
                            address = address,
                            openingBalance = opening,
                            openingBalanceType = balType
                        )
                    )
                }
                showAddCustomerDialog = false
                customerToEdit = null
            }
        )
    }

    // New / Edit Transaction Bottom Sheet
    if (showAddTransactionDialog) {
        NewTransactionSheet(
            strings = strings,
            currency = currency,
            customers = customers + suppliers,
            initialCustomerId = addTxnInitialCustomerId,
            initialType = addTxnInitialType,
            existingTransaction = transactionToEdit,
            onAddCustomerFirst = {
                showAddTransactionDialog = false
                transactionToEdit = null
                addCustomerPartyType = "CUSTOMER"
                customerToEdit = null
                showAddCustomerDialog = true
            },
            onDismiss = {
                showAddTransactionDialog = false
                transactionToEdit = null
            },
            onSave = { custId, amount, type, method, desc, billNo, txnDate, dueDate ->
                if (transactionToEdit == null) {
                    transactionViewModel.addTransaction(
                        customerId = custId,
                        amount = amount,
                        type = type,
                        paymentMethod = method,
                        description = desc,
                        billNumber = billNo,
                        transactionDate = txnDate,
                        dueDate = dueDate
                    )
                } else {
                    transactionViewModel.updateTransaction(
                        transactionToEdit!!.copy(
                            customerId = custId,
                            amount = amount,
                            type = type.value,
                            paymentMethod = method,
                            description = desc,
                            billNumber = billNo,
                            transactionDate = txnDate,
                            dueDate = dueDate
                        )
                    )
                }
                showAddTransactionDialog = false
                transactionToEdit = null
            }
        )
    }

    // Add / Edit Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            strings = strings,
            currency = currency,
            existingExpense = expenseToEdit,
            onDismiss = {
                showAddExpenseDialog = false
                expenseToEdit = null
            },
            onSave = { amount, category, method, desc, date ->
                if (expenseToEdit == null) {
                    expenseViewModel.addExpense(amount, category, method, desc, date)
                } else {
                    expenseViewModel.updateExpense(
                        expenseToEdit!!.copy(
                            amount = amount,
                            category = category,
                            paymentMethod = method,
                            description = desc
                        )
                    )
                }
                showAddExpenseDialog = false
                expenseToEdit = null
            }
        )
    }

    // Add / Edit Staff Dialog
    if (showAddStaffDialog) {
        AddStaffDialog(
            strings = strings,
            currency = currency,
            existingStaff = staffToEdit,
            onDismiss = {
                showAddStaffDialog = false
                staffToEdit = null
            },
            onDelete = { staffId ->
                viewModel.deleteStaff(staffId)
                showAddStaffDialog = false
                staffToEdit = null
            },
            onSave = { name, phone, role, salaryType, salaryAmt, overtimeRate ->
                if (staffToEdit == null) {
                    viewModel.addStaff(name, phone, role, salaryType, salaryAmt, overtimeRate)
                } else {
                    viewModel.updateStaff(
                        staffToEdit!!.copy(
                            name = name,
                            phone = phone,
                            role = role,
                            salaryType = salaryType,
                            salaryAmount = salaryAmt,
                            overtimeRate = overtimeRate
                        )
                    )
                }
                showAddStaffDialog = false
                staffToEdit = null
            }
        )
    }

    // Staff Advance Dialog
    if (advanceTargetDay != null && selectedStaffSummary != null) {
        val targetStaff = selectedStaffSummary!!
        val targetDay = advanceTargetDay!!
        StaffAdvanceDialog(
            staffName = targetStaff.name,
            dayRecord = targetDay,
            currency = currency,
            strings = strings,
            onDismiss = { advanceTargetDay = null },
            onSave = { amount, note ->
                viewModel.setStaffAdvance(targetStaff.id, targetDay.dateKey, amount, note)
                advanceTargetDay = null
            }
        )
    }

    // Staff Monthly Salary & Attendance Report Dialog
    if (showStaffReportDialog && selectedStaffSummary != null) {
        val targetStaff = selectedStaffSummary!!
        val monthLabel = StaffAttendanceCalculator.formatMonthDisplay(
            year = selectedStaffMonth.first,
            monthZeroIndexed = selectedStaffMonth.second,
            currentMonthLabel = strings.currentMonth
        )
        val salarySummary = remember(targetStaff, selectedStaffDays) {
            StaffAttendanceCalculator.calculateSalarySummary(targetStaff, selectedStaffDays)
        }
        StaffReportDialog(
            staff = targetStaff,
            monthLabel = monthLabel,
            salarySummary = salarySummary,
            currency = currency,
            strings = strings,
            onDismiss = { showStaffReportDialog = false },
            onExportPdf = {
                ReportExporter.generateAndShareStaffReportPdf(
                    context = context,
                    business = activeBusiness,
                    staff = targetStaff,
                    monthLabel = monthLabel,
                    days = selectedStaffDays,
                    salarySummary = salarySummary,
                    currency = currency
                )
            },
            onShareCsv = {
                ReportExporter.shareStaffReportCsv(
                    context = context,
                    business = activeBusiness,
                    staff = targetStaff,
                    monthLabel = monthLabel,
                    days = selectedStaffDays,
                    salarySummary = salarySummary,
                    currency = currency
                )
            }
        )
    }

    // Transaction Digital Receipt Dialog
    if (selectedReceiptTxn != null) {
        TransactionReceiptDialog(
            transaction = selectedReceiptTxn!!,
            business = activeBusiness,
            currency = currency,
            onDismiss = { selectedReceiptTxn = null }
        )
    }

    // Send Payment Reminder Dialog
    if (reminderCustomer != null) {
        PaymentReminderDialog(
            customer = reminderCustomer!!,
            business = activeBusiness,
            currency = currency,
            currentLang = currentLang,
            onDismiss = { reminderCustomer = null }
        )
    }

    // New Business Dialog
    if (showNewBusinessDialog) {
        BusinessProfileDialog(
            business = null,
            isNew = true,
            onDismiss = { showNewBusinessDialog = false },
            onSave = { name, owner, phone, address, gst ->
                viewModel.createBusiness(name, owner, phone, address, gst)
                showNewBusinessDialog = false
            }
        )
    }
}

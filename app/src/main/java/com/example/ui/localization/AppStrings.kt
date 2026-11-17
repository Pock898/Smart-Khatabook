package com.example.ui.localization

object AppStrings {
    fun get(lang: String): Dictionary {
        return if (lang == "hi") HindiDictionary else EnglishDictionary
    }

    interface Dictionary {
        val appName: String
        val dashboard: String
        val customers: String
        val staff: String
        val accounts: String
        val transactions: String
        val expenses: String
        val reports: String
        val settings: String

        // Dashboard
        val totalReceivable: String
        val totalPayable: String
        val totalCredit: String
        val totalExpenses: String
        val youWillGet: String
        val youWillGive: String
        val todayCollection: String
        val todayExpenses: String
        val totalCustomers: String
        val pendingPayments: String
        val quickActions: String
        val addCustomer: String
        val addCredit: String
        val addPayment: String
        val addExpense: String
        val viewReports: String
        val recentTransactions: String
        val monthlySummary: String
        val creditGiven: String
        val paymentsReceived: String
        val netCashFlow: String
        val viewAll: String

        // Customer
        val searchCustomer: String
        val allCustomers: String
        val filterYouWillGet: String
        val filterYouWillGive: String
        val filterSettled: String
        val customerName: String
        val mobileNumber: String
        val address: String
        val openingBalance: String
        val openingBalanceType: String
        val saveCustomer: String
        val editCustomer: String
        val deleteCustomer: String
        val deleteCustomerConfirm: String
        val callCustomer: String
        val sendReminder: String
        val reminderWhatsapp: String
        val reminderSms: String
        val shareLedger: String
        val noCustomersYet: String
        val addFirstCustomer: String
        val customerDetails: String

        // Credit & Payment
        val newTransaction: String
        val givenCreditOption: String
        val receivedPaymentOption: String
        val selectCustomerLabel: String
        val gaveCreditTitle: String // Maine Diya / Gave ₹
        val receivedPaymentTitle: String // Mujhe Mila / Received ₹
        val amount: String
        val description: String
        val billNumber: String
        val selectDate: String
        val dueDate: String
        val paymentMethod: String
        val saveTransaction: String
        val saveGivenCredit: String
        val saveReceivedPayment: String
        val deleteTransactionConfirm: String
        val editTransaction: String
        val deleteTransaction: String
        val receipt: String
        val shareReceipt: String

        // Expenses
        val expenseCategory: String
        val saveExpense: String
        val noExpensesYet: String
        val topCategory: String
        val monthExpenses: String

        // Reports & Analytics
        val dailyReport: String
        val weeklyReport: String
        val monthlyReport: String
        val yearlyReport: String
        val exportPdf: String
        val exportCsv: String
        val businessAnalytics: String
        val incomeVsExpense: String
        val categoryBreakdown: String

        // Settings & Security
        val businessProfile: String
        val businessName: String
        val ownerName: String
        val phone: String
        val gstNumber: String
        val appSecurity: String
        val pinLock: String
        val enterPin: String
        val setPin: String
        val changePin: String
        val unlockWithPin: String
        val pinIncorrect: String
        val forgotPin: String
        val securityQuestion: String
        val securityAnswer: String
        val language: String
        val currency: String
        val backupAndRestore: String
        val createBackup: String
        val restoreBackup: String
        val backupSuccess: String
        val restoreSuccess: String
        val restoreFailed: String
        val loadDemoData: String
        val demoDataLoaded: String
        val theme: String
        val aboutApp: String
        val switchBusiness: String
        val addNewBusiness: String

        // Common
        val cancel: String
        val save: String
        val delete: String
        val confirm: String
        val search: String
        val filter: String
        val settled: String
        val today: String
        val yesterday: String
        val noTransactions: String
        val validationRequired: String
        val validationAmount: String

        // Staff Attendance & Advance
        val addStaff: String
        val editStaff: String
        val deleteStaff: String
        val deleteStaffConfirm: String
        val staffName: String
        val staffRole: String
        val salaryType: String
        val salaryAmount: String
        val overtimeRate: String
        val present: String
        val absent: String
        val overtime: String
        val off: String
        val advance: String
        val dateLabel: String
        val attendanceLabel: String
        val currentMonth: String
        val openReport: String
        val netSalary: String
        val staffAttendance: String
        val noStaffYet: String
        val addFirstStaff: String
        val searchStaff: String
        val recordAdvance: String
        val advanceNote: String
        val clearAdvance: String
        val monthlySalaryReport: String
        val grossEarned: String
        val overtimePay: String
        val totalAdvance: String

        // Phone Login & Account
        val loginTitle: String
        val loginSubtitle: String
        val enterMobileNumber: String
        val invalidPhone: String
        val continueLabel: String
        val logout: String
        val logoutConfirm: String
        val loggedInAs: String
        val accountSession: String
        val shopNameOptional: String
        val safeOfflineBadge: String
        val orDivider: String
        val signInWithGoogle: String
        val continueOffline: String
        val googleSignInCancelledHint: String
        val resetPinTitle: String
        val enterNewPin: String
        val resetPinButton: String
        val invalidNewPinError: String
        val invalidSecurityAnswerError: String
        val existingLoginTab: String
        val newAccountTab: String
        val newAccountTitle: String
        val newAccountZeroHint: String

        // Parties (Customers & Suppliers)
        val parties: String
        val suppliers: String
        val addSupplier: String
        val editSupplier: String
        val supplierName: String
        val searchSupplier: String
        val noSuppliersYet: String
        val addFirstSupplier: String
        val addCustomersPrompt: String
        val addSuppliersPrompt: String

        // Cashbook & Calculator
        val cashbook: String
        val totalBalance: String
        val todaysBalance: String
        val cashInHand: String
        val online: String
        val viewCashbookReport: String
        val cashIn: String
        val cashOut: String
        val cashMode: String
        val addFirstEntry: String
        val cashbookEmptyMsg: String
        val enterAmountPlaceholder: String
        val outEntryTitle: String
        val inEntryTitle: String
        val entryNoteHint: String

        // More Hub, Items, Recycle Bin, Payment Settings
        val more: String
        val bills: String
        val items: String
        val collection: String
        val paymentSettings: String
        val recycleBin: String
        val appLockLabel: String
        val backupInfo: String
        val deleteKhata: String
        val helpAndSupport: String
        val aboutUs: String
        val inviteFriends: String
        val upiIdLabel: String
        val noItemsYet: String
        val addItem: String
        val recycleBinEmpty: String
        val restore: String
    }

    private object EnglishDictionary : Dictionary {
        override val appName = "Smart Khatabook"
        override val dashboard = "Dashboard"
        override val customers = "Customers"
        override val staff = "Staff"
        override val accounts = "Accounts"
        override val transactions = "Transactions"
        override val expenses = "Expenses"
        override val reports = "Reports"
        override val settings = "Settings"

        override val totalReceivable = "You'll Get"
        override val totalPayable = "You'll Give"
        override val totalCredit = "Total Credit"
        override val totalExpenses = "Total Expenses"
        override val youWillGet = "You'll Get"
        override val youWillGive = "You'll Give"
        override val todayCollection = "Today's Collection"
        override val todayExpenses = "Today's Expenses"
        override val totalCustomers = "Total Customers"
        override val pendingPayments = "Pending Dues"
        override val quickActions = "Quick Actions"
        override val addCustomer = "Add Customer"
        override val addCredit = "Gave (Udhar)"
        override val addPayment = "Got (Payment)"
        override val addExpense = "Add Expense"
        override val viewReports = "View Reports"
        override val recentTransactions = "Recent Transactions"
        override val monthlySummary = "Monthly Business Summary"
        override val creditGiven = "Credit Given"
        override val paymentsReceived = "Payments Received"
        override val netCashFlow = "Net Cash Flow"
        override val viewAll = "View All"

        override val searchCustomer = "Search customer by name or phone..."
        override val allCustomers = "All"
        override val filterYouWillGet = "You'll Get"
        override val filterYouWillGive = "You'll Give"
        override val filterSettled = "Settled"
        override val customerName = "Customer Name *"
        override val mobileNumber = "Mobile Number *"
        override val address = "Address"
        override val openingBalance = "Opening Balance"
        override val openingBalanceType = "Balance Type"
        override val saveCustomer = "Save Customer"
        override val editCustomer = "Edit Customer"
        override val deleteCustomer = "Delete Customer"
        override val deleteCustomerConfirm = "Are you sure you want to delete this customer? All their transactions will be deleted."
        override val callCustomer = "Call"
        override val sendReminder = "Send Reminder"
        override val reminderWhatsapp = "WhatsApp Reminder"
        override val reminderSms = "SMS Reminder"
        override val shareLedger = "Share Ledger Statement"
        override val noCustomersYet = "No customers added yet"
        override val addFirstCustomer = "Add your first customer to start your digital ledger"
        override val customerDetails = "Customer Ledger"

        override val newTransaction = "New Transaction"
        override val givenCreditOption = "Given (Credit)"
        override val receivedPaymentOption = "Received (Payment)"
        override val selectCustomerLabel = "Select Customer *"
        override val gaveCreditTitle = "You Gave (Maine Diya)"
        override val receivedPaymentTitle = "You Got (Mujhe Mila)"
        override val amount = "Amount *"
        override val description = "Item / Description"
        override val billNumber = "Bill / Invoice No."
        override val selectDate = "Transaction Date"
        override val dueDate = "Payment Due Date (Optional)"
        override val paymentMethod = "Payment Mode"
        override val saveTransaction = "Save Transaction"
        override val saveGivenCredit = "Save Given (Credit)"
        override val saveReceivedPayment = "Save Received (Payment)"
        override val deleteTransactionConfirm = "Are you sure you want to delete this transaction?"
        override val editTransaction = "Edit Transaction"
        override val deleteTransaction = "Delete Transaction"
        override val receipt = "Receipt"
        override val shareReceipt = "Share Receipt"

        override val expenseCategory = "Category"
        override val saveExpense = "Save Expense"
        override val noExpensesYet = "No expenses recorded yet"
        override val topCategory = "Top Expense Category"
        override val monthExpenses = "This Month's Expenses"

        override val dailyReport = "Daily"
        override val weeklyReport = "Weekly"
        override val monthlyReport = "Monthly"
        override val yearlyReport = "Yearly"
        override val exportPdf = "Export PDF"
        override val exportCsv = "Export CSV / Excel"
        override val businessAnalytics = "Business Analytics"
        override val incomeVsExpense = "Credit vs Collection"
        override val categoryBreakdown = "Expense Breakdown"

        override val businessProfile = "Business Profile"
        override val businessName = "Business Name"
        override val ownerName = "Owner Name"
        override val phone = "Business Phone"
        override val gstNumber = "GSTIN / Tax No."
        override val appSecurity = "Security & PIN Lock"
        override val pinLock = "App PIN Lock"
        override val enterPin = "Enter 4-Digit PIN"
        override val setPin = "Set 4-Digit PIN"
        override val changePin = "Change PIN"
        override val unlockWithPin = "Enter PIN to access Smart Khatabook"
        override val pinIncorrect = "Incorrect PIN. Please try again."
        override val forgotPin = "Forgot PIN?"
        override val securityQuestion = "Security Question"
        override val securityAnswer = "Security Answer"
        override val language = "Language / भाषा"
        override val currency = "Currency Symbol"
        override val backupAndRestore = "Data Backup & Restore"
        override val createBackup = "Create Backup (Export JSON)"
        override val restoreBackup = "Restore Backup (Import JSON)"
        override val backupSuccess = "Backup exported successfully!"
        override val restoreSuccess = "Data restored successfully!"
        override val restoreFailed = "Failed to restore backup. Invalid file format."
        override val loadDemoData = "Load Demo Data (Sample Shop)"
        override val demoDataLoaded = "Demo sample data loaded successfully!"
        override val theme = "Theme"
        override val aboutApp = "About Smart Khatabook"
        override val switchBusiness = "Switch Business"
        override val addNewBusiness = "Add New Business"

        override val cancel = "Cancel"
        override val save = "Save"
        override val delete = "Delete"
        override val confirm = "Confirm"
        override val search = "Search"
        override val filter = "Filter"
        override val settled = "Settled"
        override val today = "Today"
        override val yesterday = "Yesterday"
        override val noTransactions = "No transactions found"
        override val validationRequired = "This field is required"
        override val validationAmount = "Please enter a valid amount greater than 0"

        override val addStaff = "Add Staff"
        override val editStaff = "Edit Staff"
        override val deleteStaff = "Delete Staff"
        override val deleteStaffConfirm = "Are you sure you want to delete this staff member and all attendance records?"
        override val staffName = "Staff Name *"
        override val staffRole = "Role / Designation"
        override val salaryType = "Salary Basis"
        override val salaryAmount = "Salary / Daily Wage"
        override val overtimeRate = "Overtime Rate (per OT day)"
        override val present = "Present"
        override val absent = "Absent"
        override val overtime = "Overtime"
        override val off = "Off"
        override val advance = "Advance"
        override val dateLabel = "Date"
        override val attendanceLabel = "Attendance"
        override val currentMonth = "Current Month"
        override val openReport = "Open Report"
        override val netSalary = "Net Payable Salary"
        override val staffAttendance = "Staff Attendance"
        override val noStaffYet = "No staff members added yet"
        override val addFirstStaff = "Add your shop staff to track daily attendance, overtime, and advances in one tap."
        override val searchStaff = "Search staff by name, phone, or role..."
        override val recordAdvance = "Record Staff Advance"
        override val advanceNote = "Note (Optional)"
        override val clearAdvance = "Clear Advance"
        override val monthlySalaryReport = "Monthly Attendance & Salary Report"
        override val grossEarned = "Gross Earned"
        override val overtimePay = "Overtime Pay"
        override val totalAdvance = "Total Advance"

        override val loginTitle = "Login with Phone Number"
        override val loginSubtitle = "Manage customer credit, payments, expenses, and staff attendance in one place."
        override val enterMobileNumber = "Enter Mobile Number *"
        override val invalidPhone = "Please enter a valid 7 to 15 digit mobile number"
        override val continueLabel = "Continue"
        override val logout = "Logout"
        override val logoutConfirm = "Are you sure you want to logout? Your ledger records will stay safely saved on this device."
        override val loggedInAs = "Logged in Phone"
        override val accountSession = "Account & Phone Login"
        override val shopNameOptional = "Shop / Business Name (Optional)"
        override val safeOfflineBadge = "100% Safe & Offline Ledger • Data stays on your device"
        override val orDivider = "OR"
        override val signInWithGoogle = "Sign in with Google"
        override val continueOffline = "Skip Login & Open Offline Ledger"
        override val googleSignInCancelledHint = "Google Sign-In was cancelled or no Google account is signed in on this device. Tap again, login with your mobile number above, or continue offline."
        override val resetPinTitle = "Reset Security PIN"
        override val enterNewPin = "Enter New 4-Digit PIN"
        override val resetPinButton = "Reset PIN"
        override val invalidNewPinError = "New PIN must be 4 digits"
        override val invalidSecurityAnswerError = "Incorrect security answer or shop name."
        override val existingLoginTab = "Login"
        override val newAccountTab = "New Account"
        override val newAccountTitle = "Create New Account"
        override val newAccountZeroHint = "Starts a fresh zero-balance ledger for your new account."

        override val parties = "Parties"
        override val suppliers = "Suppliers"
        override val addSupplier = "Add Supplier"
        override val editSupplier = "Edit Supplier"
        override val supplierName = "Supplier Name *"
        override val searchSupplier = "Search Supplier"
        override val noSuppliersYet = "No suppliers added yet"
        override val addFirstSupplier = "Add suppliers to track purchases & vendor payments"
        override val addCustomersPrompt = "Add customers & collect payments faster"
        override val addSuppliersPrompt = "Add supplier and manage your purchases"

        override val cashbook = "Cashbook"
        override val totalBalance = "Total Balance"
        override val todaysBalance = "Today's Balance"
        override val cashInHand = "Cash in Hand"
        override val online = "Online"
        override val viewCashbookReport = "VIEW CASHBOOK REPORT"
        override val cashIn = "IN"
        override val cashOut = "OUT"
        override val cashMode = "Cash"
        override val addFirstEntry = "Add Your First Entry"
        override val cashbookEmptyMsg = "Hello! Let's make today's entries"
        override val enterAmountPlaceholder = "Enter amount"
        override val outEntryTitle = "OUT entry of"
        override val inEntryTitle = "IN entry of"
        override val entryNoteHint = "Enter note / item details (optional)"

        override val more = "More"
        override val bills = "Bills"
        override val items = "Items"
        override val collection = "Collection"
        override val paymentSettings = "Payment Settings"
        override val recycleBin = "Recycle Bin"
        override val appLockLabel = "App Lock"
        override val backupInfo = "Backup Information"
        override val deleteKhata = "Delete Khata"
        override val helpAndSupport = "Help & Support"
        override val aboutUs = "About Us"
        override val inviteFriends = "Invite Friends"
        override val upiIdLabel = "UPI ID (e.g. shop@upi)"
        override val noItemsYet = "No items added yet"
        override val addItem = "Add Item"
        override val recycleBinEmpty = "Recycle Bin is empty"
        override val restore = "Restore"
    }

    private object HindiDictionary : Dictionary {
        override val appName = "स्मार्ट खाता-बुक"
        override val dashboard = "डैशबोर्ड"
        override val customers = "ग्राहक"
        override val staff = "स्टाफ"
        override val accounts = "खाते"
        override val transactions = "लेन-देन"
        override val expenses = "खर्च"
        override val reports = "रिपोर्ट्स"
        override val settings = "सेटिंग्स"

        override val totalReceivable = "आपको मिलेंगे"
        override val totalPayable = "आपको देने हैं"
        override val totalCredit = "कुल उधार (क्रेडिट)"
        override val totalExpenses = "कुल खर्च"
        override val youWillGet = "मिलेंगे"
        override val youWillGive = "देने हैं"
        override val todayCollection = "आज की वसूली"
        override val todayExpenses = "आज का खर्चा"
        override val totalCustomers = "कुल ग्राहक"
        override val pendingPayments = "बकाया खाते"
        override val quickActions = "त्वरित कार्य"
        override val addCustomer = "ग्राहक जोड़ें"
        override val addCredit = "मैंने दिया (उधार)"
        override val addPayment = "मुझे मिला (जमा)"
        override val addExpense = "खर्च जोड़ें"
        override val viewReports = "रिपोर्ट देखें"
        override val recentTransactions = "हालिया लेन-देन"
        override val monthlySummary = "मासिक व्यापार सारांश"
        override val creditGiven = "दिया गया उधार"
        override val paymentsReceived = "प्राप्त भुगतान"
        override val netCashFlow = "शुद्ध नकदी प्रवाह"
        override val viewAll = "सभी देखें"

        override val searchCustomer = "नाम या मोबाइल नंबर से खोजें..."
        override val allCustomers = "सभी"
        override val filterYouWillGet = "मिलेंगे"
        override val filterYouWillGive = "देने हैं"
        override val filterSettled = "हिसाब चुकता"
        override val customerName = "ग्राहक का नाम *"
        override val mobileNumber = "मोबाइल नंबर *"
        override val address = "पता"
        override val openingBalance = "प्रारंभिक शेष (Opening Balance)"
        override val openingBalanceType = "शेष प्रकार"
        override val saveCustomer = "ग्राहक सहेजें"
        override val editCustomer = "ग्राहक संपादित करें"
        override val deleteCustomer = "ग्राहक हटाएं"
        override val deleteCustomerConfirm = "क्या आप निश्चित रूप से इस ग्राहक को हटाना चाहते हैं? इनके सभी लेन-देन हटा दिए जाएंगे।"
        override val callCustomer = "कॉल करें"
        override val sendReminder = "तगादा / तकाजा भेजें"
        override val reminderWhatsapp = "व्हाट्सएप तगादा"
        override val reminderSms = "एसएमएस तगादा"
        override val shareLedger = "खाता विवरणी शेयर करें"
        override val noCustomersYet = "अभी तक कोई ग्राहक नहीं है"
        override val addFirstCustomer = "डिजिटल खाता शुरू करने के लिए अपना पहला ग्राहक जोड़ें"
        override val customerDetails = "ग्राहक खाता विवरणी"

        override val newTransaction = "नया लेन-देन"
        override val givenCreditOption = "दिया (उधार / Credit)"
        override val receivedPaymentOption = "मिला (जमा / Payment)"
        override val selectCustomerLabel = "ग्राहक चुनें *"
        override val gaveCreditTitle = "मैंने दिया (उधार)"
        override val receivedPaymentTitle = "मुझे मिला (जमा)"
        override val amount = "राशि *"
        override val description = "सामान / विवरण"
        override val billNumber = "बिल / पर्ची नंबर"
        override val selectDate = "लेन-देन की तारीख"
        override val dueDate = "भुगतान नियत तारीख (वैकल्पिक)"
        override val paymentMethod = "भुगतान का माध्यम"
        override val saveTransaction = "लेन-देन सहेजें"
        override val saveGivenCredit = "दिया (उधार) सहेजें"
        override val saveReceivedPayment = "मिला (भुगतान) सहेजें"
        override val deleteTransactionConfirm = "क्या आप इस लेन-देन को हटाना चाहते हैं?"
        override val editTransaction = "लेन-देन संपादित करें"
        override val deleteTransaction = "लेन-देन हटाएं"
        override val receipt = "रसीद"
        override val shareReceipt = "रसीद शेयर करें"

        override val expenseCategory = "खर्च श्रेणी"
        override val saveExpense = "खर्च सहेजें"
        override val noExpensesYet = "अभी तक कोई खर्च दर्ज नहीं है"
        override val topCategory = "मुख्य खर्च श्रेणी"
        override val monthExpenses = "इस महीने का कुल खर्च"

        override val dailyReport = "दैनिक"
        override val weeklyReport = "साप्ताहिक"
        override val monthlyReport = "मासिक"
        override val yearlyReport = "वार्षिक"
        override val exportPdf = "PDF निकालें"
        override val exportCsv = "CSV / Excel निकालें"
        override val businessAnalytics = "व्यापार विश्लेषण"
        override val incomeVsExpense = "उधार बनाम वसूली"
        override val categoryBreakdown = "खर्च का विश्लेषण"

        override val businessProfile = "व्यापार प्रोफाइल"
        override val businessName = "दुकान / व्यापार का नाम"
        override val ownerName = "मालिक का नाम"
        override val phone = "व्यापार फोन नंबर"
        override val gstNumber = "जीएसटी / टैक्स नंबर"
        override val appSecurity = "सुरक्षा और पिन लॉक"
        override val pinLock = "ऐप पिन लॉक"
        override val enterPin = "4 अंकों का पिन दर्ज करें"
        override val setPin = "4 अंकों का पिन सेट करें"
        override val changePin = "पिन बदलें"
        override val unlockWithPin = "स्मार्ट खाता-बुक खोलने के लिए पिन दर्ज करें"
        override val pinIncorrect = "गलत पिन! कृपया पुनः प्रयास करें।"
        override val forgotPin = "पिन भूल गए?"
        override val securityQuestion = "सुरक्षा प्रश्न"
        override val securityAnswer = "सुरक्षा उत्तर"
        override val language = "भाषा (Language)"
        override val currency = "मुद्रा चिह्न"
        override val backupAndRestore = "डेटा बैकअप और पुनर्स्थापना"
        override val createBackup = "बैकअप बनाएं (JSON निर्यात)"
        override val restoreBackup = "बैकअप बहाल करें (JSON आयात)"
        override val backupSuccess = "बैकअप सफलतापूर्वक सुरक्षित हो गया!"
        override val restoreSuccess = "डेटा सफलतापूर्वक बहाल हो गया!"
        override val restoreFailed = "बैकअप बहाल करने में विफल। अमान्य फ़ाइल।"
        override val loadDemoData = "डेमो डेटा लोड करें (नमूना दुकान)"
        override val demoDataLoaded = "नमूना दुकान डेटा सफलतापूर्वक लोड किया गया!"
        override val theme = "थीम (रंग)"
        override val aboutApp = "स्मार्ट खाता-बुक के बारे में"
        override val switchBusiness = "व्यापार बदलें"
        override val addNewBusiness = "नया व्यापार जोड़ें"

        override val cancel = "रद्द करें"
        override val save = "सहेजें"
        override val delete = "हटाएं"
        override val confirm = "पुष्टि करें"
        override val search = "खोजें"
        override val filter = "फ़िल्टर"
        override val settled = "चुकता"
        override val today = "आज"
        override val yesterday = "कल"
        override val noTransactions = "कोई लेन-देन नहीं मिला"
        override val validationRequired = "यह जानकारी अनिवार्य है"
        override val validationAmount = "कृपया 0 से अधिक वैध राशि दर्ज करें"

        override val addStaff = "स्टाफ जोड़ें"
        override val editStaff = "स्टाफ संपादित करें"
        override val deleteStaff = "स्टाफ हटाएं"
        override val deleteStaffConfirm = "क्या आप निश्चित रूप से इस स्टाफ और उनकी सभी हाजिरी हटाना चाहते हैं?"
        override val staffName = "स्टाफ का नाम *"
        override val staffRole = "पद / कार्य"
        override val salaryType = "वेतन प्रकार"
        override val salaryAmount = "वेतन / दैनिक मजदूरी"
        override val overtimeRate = "ओवरटाइम दर (प्रति दिन)"
        override val present = "उपस्थित"
        override val absent = "अनुपस्थित"
        override val overtime = "ओवरटाइम"
        override val off = "छुट्टी"
        override val advance = "एडवांस"
        override val dateLabel = "तारीख"
        override val attendanceLabel = "हाजिरी"
        override val currentMonth = "वर्तमान महीना"
        override val openReport = "रिपोर्ट खोलें"
        override val netSalary = "शुद्ध देय वेतन"
        override val staffAttendance = "स्टाफ हाजिरी"
        override val noStaffYet = "अभी तक कोई स्टाफ नहीं जुड़ा है"
        override val addFirstStaff = "एक टैप में दैनिक हाजिरी, ओवरटाइम और एडवांस ट्रैक करने के लिए स्टाफ जोड़ें।"
        override val searchStaff = "नाम, मोबाइल या पद से स्टाफ खोजें..."
        override val recordAdvance = "स्टाफ एडवांस दर्ज करें"
        override val advanceNote = "विवरण (वैकल्पिक)"
        override val clearAdvance = "एडवांस हटाएं"
        override val monthlySalaryReport = "मासिक हाजिरी और वेतन रिपोर्ट"
        override val grossEarned = "कुल अर्जित वेतन"
        override val overtimePay = "ओवरटाइम भुगतान"
        override val totalAdvance = "कुल एडवांस"

        override val loginTitle = "मोबाइल नंबर से लॉगिन करें"
        override val loginSubtitle = "ग्राहक उधार, भुगतान, खर्च और स्टाफ हाजिरी एक ही जगह संभालें।"
        override val enterMobileNumber = "मोबाइल नंबर दर्ज करें *"
        override val invalidPhone = "कृपया 7 से 15 अंकों का वैध मोबाइल नंबर दर्ज करें"
        override val continueLabel = "आगे बढ़ें"
        override val logout = "लॉग आउट"
        override val logoutConfirm = "क्या आप लॉग आउट करना चाहते हैं? आपका पूरा खाता डेटा इस डिवाइस पर सुरक्षित रहेगा।"
        override val loggedInAs = "लॉगिन मोबाइल नंबर"
        override val accountSession = "खाता और मोबाइल लॉगिन"
        override val shopNameOptional = "दुकान / व्यापार का नाम (वैकल्पिक)"
        override val safeOfflineBadge = "100% सुरक्षित और ऑफलाइन खाता • आपका डेटा डिवाइस पर सुरक्षित"
        override val orDivider = "या"
        override val signInWithGoogle = "Google से साइन इन करें"
        override val continueOffline = "लॉगिन छोड़ें और ऑफलाइन खाता खोलें"
        override val googleSignInCancelledHint = "Google साइन-इन रद्द हो गया या इस डिवाइस पर कोई Google खाता सक्रिय नहीं है। फिर से प्रयास करें, ऊपर मोबाइल नंबर से लॉगिन करें, या ऑफलाइन जारी रखें।"
        override val resetPinTitle = "सुरक्षा पिन रीसेट करें"
        override val enterNewPin = "नया 4 अंकों का पिन दर्ज करें"
        override val resetPinButton = "पिन रीसेट करें"
        override val invalidNewPinError = "नया पिन 4 अंकों का होना चाहिए"
        override val invalidSecurityAnswerError = "गलत सुरक्षा उत्तर या दुकान का नाम।"
        override val existingLoginTab = "लॉगिन"
        override val newAccountTab = "नया खाता"
        override val newAccountTitle = "नया खाता बनाएं"
        override val newAccountZeroHint = "नए खाते के लिए 0 से बिल्कुल नया बही-खाता शुरू होगा।"

        override val parties = "पार्टियां"
        override val suppliers = "सप्लायर"
        override val addSupplier = "सप्लायर जोड़ें"
        override val editSupplier = "सप्लायर संपादित करें"
        override val supplierName = "सप्लायर का नाम *"
        override val searchSupplier = "सप्लायर खोजें"
        override val noSuppliersYet = "अभी तक कोई सप्लायर नहीं है"
        override val addFirstSupplier = "खरीदारी और भुगतान ट्रैक करने के लिए सप्लायर जोड़ें"
        override val addCustomersPrompt = "ग्राहक जोड़ें और तेजी से भुगतान पाएं"
        override val addSuppliersPrompt = "सप्लायर जोड़ें और अपनी खरीदारी प्रबंधित करें"

        override val cashbook = "कैशबुक"
        override val totalBalance = "कुल शेष"
        override val todaysBalance = "आज का शेष"
        override val cashInHand = "नकद (गल्ला)"
        override val online = "ऑनलाइन"
        override val viewCashbookReport = "कैशबुक रिपोर्ट देखें"
        override val cashIn = "IN (आया)"
        override val cashOut = "OUT (गया)"
        override val cashMode = "नकद"
        override val addFirstEntry = "अपनी पहली एंट्री जोड़ें"
        override val cashbookEmptyMsg = "नमस्ते! आइए आज की एंट्री दर्ज करें"
        override val enterAmountPlaceholder = "राशि दर्ज करें"
        override val outEntryTitle = "OUT एंट्री:"
        override val inEntryTitle = "IN एंट्री:"
        override val entryNoteHint = "विवरण / नोट लिखें (वैकल्पिक)"

        override val more = "अधिक"
        override val bills = "बिल / खर्च"
        override val items = "सामान (Items)"
        override val collection = "वसूली"
        override val paymentSettings = "भुगतान सेटिंग्स"
        override val recycleBin = "रीसाइकल बिन"
        override val appLockLabel = "ऐप लॉक"
        override val backupInfo = "बैकअप जानकारी"
        override val deleteKhata = "खाता हटाएं"
        override val helpAndSupport = "सहायता और समर्थन"
        override val aboutUs = "हमारे बारे में"
        override val inviteFriends = "दोस्तों को आमंत्रित करें"
        override val upiIdLabel = "यूपीआई आईडी (जैसे shop@upi)"
        override val noItemsYet = "अभी तक कोई सामान नहीं जोड़ा गया"
        override val addItem = "सामान जोड़ें"
        override val recycleBinEmpty = "रीसाइकल बिन खाली है"
        override val restore = "वापस लाएं"
    }
}

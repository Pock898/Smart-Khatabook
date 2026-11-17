package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.BusinessEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.StaffAttendanceEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.BalanceType
import com.example.data.model.CustomerWithBalance
import com.example.data.model.StaffAttendanceCalculator
import com.example.data.model.StaffWithSummary
import com.example.data.auth.FirebaseAuthHelper
import com.example.data.repository.KhatabookRepository
import com.example.data.repository.PreferencesManager
import com.example.ui.CustomerViewModel
import com.example.ui.ExpenseViewModel
import com.example.ui.MainViewModel
import com.example.ui.TransactionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Khatabook", appName)
  }

  @Test
  fun `test customer balance calculations`() {
    // Opening Udhar 1000 + Credit 2500 - Payment 1500 = 2000 (You'll Get)
    val opening = 1000.0
    val credit = 2500.0
    val payment = 1500.0
    val netBalance = opening + credit - payment

    val customer = CustomerWithBalance(
      id = 1L,
      businessId = 1L,
      name = "Ramesh Kumar",
      phone = "9876543210",
      address = "Market Yard",
      openingBalance = opening,
      openingBalanceType = "GET",
      totalCredit = credit,
      totalPayment = payment,
      netBalance = netBalance,
      lastTransactionDate = System.currentTimeMillis(),
      transactionCount = 2
    )

    assertEquals(2000.0, customer.netBalance, 0.001)
    assertEquals(BalanceType.YOU_WILL_GET, customer.balanceType)
  }

  @Test
  fun `test attendance status mapping and monthly salary calculation`() {
    assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.from("P"))
    assertEquals(AttendanceStatus.ABSENT, AttendanceStatus.from("A"))
    assertEquals(AttendanceStatus.OVERTIME, AttendanceStatus.from("OT"))
    assertEquals(AttendanceStatus.OFF, AttendanceStatus.from("OFF"))
    assertEquals(AttendanceStatus.NONE, AttendanceStatus.from("UNKNOWN"))

    // 30-day month (April 2026 -> monthZeroIndexed = 3)
    val entries = listOf(
      StaffAttendanceEntity(id = 1, businessId = 1, staffId = 10, dateKey = "2026-04-01", status = "P", advanceAmount = 0.0),
      StaffAttendanceEntity(id = 2, businessId = 1, staffId = 10, dateKey = "2026-04-02", status = "OT", advanceAmount = 500.0),
      StaffAttendanceEntity(id = 3, businessId = 1, staffId = 10, dateKey = "2026-04-03", status = "OFF", advanceAmount = 1000.0),
      StaffAttendanceEntity(id = 4, businessId = 1, staffId = 10, dateKey = "2026-04-04", status = "A", advanceAmount = 0.0)
    )

    val days = StaffAttendanceCalculator.buildMonthDayRecords(2026, Calendar.APRIL, entries)
    assertEquals(30, days.size)
    assertEquals("01", days[0].dayOfMonth)
    assertEquals(AttendanceStatus.PRESENT, days[0].status)
    assertEquals(AttendanceStatus.OVERTIME, days[1].status)
    assertEquals(500.0, days[1].advanceAmount, 0.001)

    val staff = StaffWithSummary(
      id = 10L,
      businessId = 1L,
      name = "Harish",
      phone = "9988776433",
      role = "Shop Assistant",
      salaryType = "MONTHLY",
      salaryAmount = 15000.0,
      overtimeRate = 250.0,
      presentCount = 1,
      absentCount = 1,
      overtimeCount = 1,
      offCount = 1,
      totalAdvance = 1500.0,
      todayStatus = AttendanceStatus.NONE
    )

    val summary = StaffAttendanceCalculator.calculateSalarySummary(staff, days)
    // Paid days = 1 (P) + 1 (OT) + 1 (OFF) = 3 out of 30 => 3/30 * 15000 = 1500 base
    // Overtime = 1 * 250 = 250; Gross = 1750; Advance = 1500; Net = 250
    assertEquals(1500.0, summary.earnedBasePay, 0.001)
    assertEquals(250.0, summary.overtimePay, 0.001)
    assertEquals(1750.0, summary.grossEarned, 0.001)
    assertEquals(1500.0, summary.totalAdvance, 0.001)
    assertEquals(250.0, summary.netPayable, 0.001)
  }

  @Test
  fun `test phone number login and logout session state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = PreferencesManager(context)
    assertFalse(prefs.isLoggedIn.value)

    prefs.loginWithPhone("+91 9876543210")
    assertTrue(prefs.isLoggedIn.value)
    assertEquals("+91 9876543210", prefs.loggedInPhone.value)

    prefs.logout()
    assertFalse(prefs.isLoggedIn.value)
    assertEquals("+91 9876543210", prefs.loggedInPhone.value)
  }

  @Test
  fun `test e164 phone formatting, normalization, email rejection, and otp validation`() {
    assertEquals("+919876543210", FirebaseAuthHelper.formatE164Phone("+91", "98765 43210"))
    assertEquals("+91 9876543210", FirebaseAuthHelper.formatDisplayPhone("+91", "98765-43210"))
    assertTrue(FirebaseAuthHelper.isValidPhoneDigits("9876543210"))
    assertFalse(FirebaseAuthHelper.isValidPhoneDigits("12345"))
    assertTrue(FirebaseAuthHelper.isValidOtpCode("123456"))
    assertFalse(FirebaseAuthHelper.isValidOtpCode("12345"))

    // Pasted numbers with country code or leading zero normalize cleanly, while 10-digit 91xxx stays intact
    assertEquals("9876543210", FirebaseAuthHelper.normalizePhoneDigits("+91", "+91 98765 43210"))
    assertEquals("9876543210", FirebaseAuthHelper.normalizePhoneDigits("+91", "919876543210"))
    assertEquals("9876543210", FirebaseAuthHelper.normalizePhoneDigits("+91", "09876543210"))
    assertEquals("9123456789", FirebaseAuthHelper.normalizePhoneDigits("+91", "9123456789"))

    // Email or Offline label must NEVER leak digits into phone field
    val supported = listOf("+91", "+1", "+44", "+971", "+977", "+880")
    assertEquals("+91" to "", FirebaseAuthHelper.extractPhoneParts("rajeshjani908@gmail.com", supported))
    assertEquals("+91" to "", FirebaseAuthHelper.extractPhoneParts("Offline Ledger", supported))
    assertEquals("+971" to "501234567", FirebaseAuthHelper.extractPhoneParts("+971 501234567", supported))

    val billingMsg = FirebaseAuthHelper.mapPhoneAuthError(
      Exception("An internal error has occurred. [BILLING_NOT_ENABLED]")
    )
    assertTrue(billingMsg.contains("Blaze", ignoreCase = true))
  }

  @Test
  fun `test viewmodel phone login validation and logout unlock reset`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(app)

    // Invalid short phone must be rejected
    assertFalse(viewModel.loginWithPhone("+91", "123", "Kiran Store", ""))
    assertFalse(viewModel.isLoggedIn.value)

    // Valid normalized phone logs in cleanly
    assertTrue(viewModel.loginWithPhone("+91", "+91 98765 43210", "Kiran Store", ""))
    assertTrue(viewModel.isLoggedIn.value)
    assertEquals("+91 9876543210", viewModel.loggedInPhone.value)
    assertTrue(viewModel.isUnlocked.value)

    // With PIN lock enabled, logout locks session and requires PIN on re-login
    viewModel.setPin("4321", true, "Kiran Store")
    viewModel.logout()
    assertFalse(viewModel.isLoggedIn.value)
    assertFalse(viewModel.isUnlocked.value)

    assertFalse(viewModel.loginWithPhone("+91", "9876543210", "Kiran Store", "0000"))
    assertFalse(viewModel.isLoggedIn.value)

    assertTrue(viewModel.loginWithPhone("+91", "9876543210", "Kiran Store", "4321"))
    assertTrue(viewModel.isLoggedIn.value)
    assertTrue(viewModel.isUnlocked.value)
  }

  @Test
  fun `test pin verification reset and offline login skip`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(app)

    // Unset PIN must never verify as true
    assertFalse(viewModel.prefs.verifyPin(""))

    // Offline skip works when PIN is disabled
    assertTrue(viewModel.skipLoginOffline("Gupta Traders"))
    assertTrue(viewModel.isLoggedIn.value)

    // Non-digit PIN must be rejected
    viewModel.setPin("ab12", true, "Gupta Traders")
    assertFalse(viewModel.isPinEnabled.value)

    // Valid 4-digit PIN locks and resets via security answer
    viewModel.setPin("1234", true, "Gupta Traders")
    assertTrue(viewModel.isPinEnabled.value)
    assertFalse(viewModel.unlockWithPin("9999"))
    assertTrue(viewModel.unlockWithPin("1234"))

    viewModel.lockApp()
    assertFalse(viewModel.isUnlocked.value)

    // Reset PIN via security answer
    assertFalse(viewModel.resetPinViaSecurity("Wrong Shop", "5678"))
    assertTrue(viewModel.resetPinViaSecurity("gupta traders", "5678"))
    assertTrue(viewModel.isUnlocked.value)
    assertTrue(viewModel.unlockWithPin("5678"))

    // Offline skip must be blocked while PIN lock is active
    viewModel.logout()
    assertFalse(viewModel.skipLoginOffline("Gupta Traders"))
  }

  @Test
  fun `test backup export and strict transactional import validation`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = KhatabookRepository(db)
      val busId = repo.ensureDefaultBusiness()
      val custId = repo.addCustomer(busId, "Suresh Store", "9876543210", "Main Road", 100.0, "GET")
      repo.addTransaction(busId, custId, 500.0, com.example.data.model.TransactionType.CREDIT, "CASH", "Sugar", "B-1", System.currentTimeMillis(), null)
      repo.addExpense(busId, 200.0, "RENT", "CASH", "Shop rent", System.currentTimeMillis())

      val summary = repo.getDashboardSummary(busId).first()
      assertEquals(500.0, summary.totalCredit, 0.001)
      assertEquals(200.0, summary.totalExpenses, 0.001)
      assertEquals(600.0, summary.totalReceivable, 0.001)
      assertEquals(1, summary.totalCustomers)

      val exported = repo.exportBackupJson(busId)
      assertFalse(repo.importBackupJson(busId, "{}"))
      assertFalse(repo.importBackupJson(busId, "not-json"))
      assertTrue(repo.importBackupJson(busId, exported))
    } finally {
      db.close()
    }
  }

  @Test
  fun `test AppDatabase DAOs full CRUD and cascade deletes`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val businessId = db.businessDao().insertBusiness(BusinessEntity(name = "Test Mart"))

      // CustomerDao CRUD
      val customerDao = db.customerDao()
      val custId = customerDao.insertCustomer(
        CustomerEntity(businessId = businessId, name = "Asha", phone = "9876543210", openingBalance = 100.0)
      )
      assertEquals("Asha", customerDao.getCustomerByIdSync(custId)?.name)
      assertEquals(1, customerDao.getAllCustomers().first().size)

      customerDao.updateCustomer(
        customerDao.getCustomerByIdSync(custId)!!.copy(name = "Asha Kirana")
      )
      assertEquals("Asha Kirana", customerDao.getCustomerById(custId).first()?.name)
      assertEquals(1, customerDao.searchCustomers(businessId, "Kirana").first().size)

      // TransactionDao CRUD & aggregate flows
      val transactionDao = db.transactionDao()
      val now = System.currentTimeMillis()
      val txnId = transactionDao.insertTransaction(
        TransactionEntity(
          businessId = businessId,
          customerId = custId,
          amount = 750.0,
          type = "CREDIT",
          transactionDate = now
        )
      )
      assertEquals(750.0, transactionDao.getTotalCreditByBusiness(businessId).first(), 0.001)
      transactionDao.updateTransaction(
        transactionDao.getTransactionByIdSync(txnId)!!.copy(amount = 900.0)
      )
      assertEquals(900.0, transactionDao.getTransactionById(txnId).first()?.amount ?: 0.0, 0.001)
      assertEquals(900.0, transactionDao.getTotalCreditByBusiness(businessId).first(), 0.001)

      // ExpenseDao CRUD & aggregate flows
      val expenseDao = db.expenseDao()
      val expId = expenseDao.insertExpense(
        ExpenseEntity(
          businessId = businessId,
          amount = 300.0,
          category = "RENT",
          expenseDate = now
        )
      )
      assertEquals(300.0, expenseDao.getTotalExpensesByBusiness(businessId).first(), 0.001)
      expenseDao.updateExpense(
        expenseDao.getExpenseByIdSync(expId)!!.copy(amount = 450.0)
      )
      assertEquals(450.0, expenseDao.getExpensesByCategory(businessId, "RENT").first().first().amount, 0.001)
      expenseDao.deleteExpenseById(expId)
      assertTrue(expenseDao.getAllExpenses().first().isEmpty())

      // Deleting customer cascades to transactions
      customerDao.deleteCustomerById(custId)
      assertTrue(customerDao.getAllCustomers().first().isEmpty())
      assertTrue(transactionDao.getAllTransactions().first().isEmpty())
    } finally {
      db.close()
    }
  }

  @Test
  fun `test CustomerViewModel and TransactionViewModel CRUD and dashboard summary`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = KhatabookRepository(db)
      val prefs = PreferencesManager(app)
      val busId = repo.ensureDefaultBusiness()
      prefs.setActiveBusinessId(busId)

      val customerVm = CustomerViewModel(app, repo, prefs)
      val transactionVm = TransactionViewModel(app, repo, prefs)

      suspend fun awaitAsync(condition: suspend () -> Boolean) {
        val deadline = System.currentTimeMillis() + 3000L
        while (System.currentTimeMillis() < deadline) {
          org.robolectric.shadows.ShadowLooper.idleMainLooper()
          if (condition()) return
          kotlinx.coroutines.delay(20)
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(condition())
      }

      // Validation rejects blank customer name
      customerVm.addCustomer(name = "   ")
      assertEquals("Customer name is required", customerVm.userMessage.value)

      // Add customer via CustomerViewModel
      var createdCustomerId = -1L
      customerVm.addCustomer(
        name = "Mohan Traders",
        phone = "9876501234",
        address = "Station Road",
        openingBalance = 200.0,
        openingBalanceType = "GET"
      ) { id -> createdCustomerId = id }
      awaitAsync { createdCustomerId > 0L }

      val customers = repo.getCustomersWithBalance(busId).first()
      assertEquals(1, customers.size)

      // Search & filter in CustomerViewModel
      customerVm.selectCustomer(createdCustomerId)
      transactionVm.selectCustomer(createdCustomerId)
      customerVm.setSearchQuery("Mohan")
      assertEquals("Mohan", customerVm.searchQuery.value)
      customerVm.setFilterType(BalanceType.YOU_WILL_GET)
      assertEquals(BalanceType.YOU_WILL_GET, customerVm.filterType.value)

      // Validation rejects zero/negative transaction amount
      transactionVm.addTransaction(
        customerId = createdCustomerId,
        amount = 0.0,
        type = com.example.data.model.TransactionType.CREDIT
      )
      assertEquals("Amount must be greater than zero", transactionVm.userMessage.value)

      // Add credit transaction via TransactionViewModel
      var creditTxnId = -1L
      transactionVm.addTransaction(
        customerId = createdCustomerId,
        amount = 800.0,
        type = com.example.data.model.TransactionType.CREDIT,
        paymentMethod = "CASH",
        description = "Rice bags"
      ) { id -> creditTxnId = id }
      awaitAsync { creditTxnId > 0L }

      val summaryAfterCredit = repo.getDashboardSummary(busId).first()
      assertEquals(800.0, summaryAfterCredit.totalCredit, 0.001)
      assertEquals(1000.0, summaryAfterCredit.totalReceivable, 0.001)

      // Update transaction amount
      var txnUpdated = false
      val savedTxn = db.transactionDao().getTransactionByIdSync(creditTxnId)!!
      transactionVm.updateTransaction(savedTxn.copy(amount = 650.0)) { txnUpdated = true }
      awaitAsync { txnUpdated }
      assertEquals(650.0, db.transactionDao().getTransactionByIdSync(creditTxnId)!!.amount, 0.001)

      // Delete transaction
      var txnDeleted = false
      transactionVm.deleteTransaction(creditTxnId) { txnDeleted = true }
      awaitAsync { txnDeleted }
      assertEquals(0, repo.getCustomerTransactions(createdCustomerId).first().size)

      // Update & delete customer
      var custUpdated = false
      val savedCust = db.customerDao().getCustomerByIdSync(createdCustomerId)!!
      customerVm.updateCustomer(savedCust.copy(name = "Mohan General Store")) { custUpdated = true }
      awaitAsync { custUpdated }
      assertEquals("Mohan General Store", db.customerDao().getCustomerByIdSync(createdCustomerId)?.name)

      var custDeleted = false
      customerVm.deleteCustomer(createdCustomerId) { custDeleted = true }
      awaitAsync { custDeleted }
      assertTrue(repo.getCustomersWithBalance(busId).first().isEmpty())

      // ExpenseViewModel validation & CRUD
      val expenseVm = ExpenseViewModel(app, repo, prefs)
      expenseVm.addExpense(amount = -10.0, category = "RENT")
      assertEquals("Expense amount must be greater than zero", expenseVm.userMessage.value)

      var expenseId = -1L
      expenseVm.addExpense(
        amount = 1250.0,
        category = "ELECTRICITY",
        paymentMethod = "UPI",
        description = "Monthly power bill"
      ) { id -> expenseId = id }
      awaitAsync { expenseId > 0L }
      assertEquals(1250.0, repo.getDashboardSummary(busId).first().totalExpenses, 0.001)

      expenseVm.setCategoryFilter("ELECTRICITY")
      assertEquals("ELECTRICITY", expenseVm.selectedCategory.value)

      var expUpdated = false
      val savedExp = db.expenseDao().getExpenseByIdSync(expenseId)!!
      expenseVm.updateExpense(savedExp.copy(amount = 1500.0)) { expUpdated = true }
      awaitAsync { expUpdated }
      assertEquals(1500.0, db.expenseDao().getExpenseByIdSync(expenseId)!!.amount, 0.001)

      var expDeleted = false
      expenseVm.deleteExpense(expenseId) { expDeleted = true }
      awaitAsync { expDeleted }
      assertEquals(0.0, repo.getDashboardSummary(busId).first().totalExpenses, 0.001)
    } finally {
      db.close()
    }
  }

  @Test
  fun `test shared PreferencesManager syncs ViewModels and new account starts at zero`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val mainVm = MainViewModel(app)
    val customerVm = CustomerViewModel(app)
    val transactionVm = TransactionViewModel(app)
    val expenseVm = ExpenseViewModel(app)

    suspend fun awaitAsync(condition: suspend () -> Boolean) {
      val deadline = System.currentTimeMillis() + 3000L
      while (System.currentTimeMillis() < deadline) {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        if (condition()) return
        kotlinx.coroutines.delay(20)
      }
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      assertTrue(condition())
    }

    // Wait for MainViewModel init to create default business and ensure ALL ViewModels share it
    awaitAsync { mainVm.prefs.activeBusinessId.value > 0L }
    assertEquals(mainVm.prefs.activeBusinessId.value, PreferencesManager.getInstance(app).activeBusinessId.value)

    // Login Account A and add a customer + expense
    assertTrue(mainVm.loginWithPhone("+91", "9999988888", "Shop A", ""))
    awaitAsync {
      val id = mainVm.prefs.activeBusinessId.value
      id > 0L && mainVm.repository.getBusinessByIdSync(id)?.name == "Shop A"
    }
    val shopAId = mainVm.prefs.activeBusinessId.value

    var custId = -1L
    customerVm.addCustomer(name = "Raju", phone = "9111122222") { custId = it }
    awaitAsync { custId > 0L }
    awaitAsync { mainVm.repository.getCustomersWithBalance(shopAId).first().size == 1 }

    var txnId = -1L
    transactionVm.addTransaction(custId, 500.0, com.example.data.model.TransactionType.CREDIT) { txnId = it }
    awaitAsync { txnId > 0L }

    var expId = -1L
    expenseVm.addExpense(150.0, "RENT") { expId = it }
    awaitAsync { expId > 0L }

    val summaryA = mainVm.repository.getDashboardSummary(shopAId).first()
    assertEquals(500.0, summaryA.totalCredit, 0.001)
    assertEquals(150.0, summaryA.totalExpenses, 0.001)

    // Logout and login with NEW Account B -> must get a clean zero-balance ledger
    mainVm.logout()
    assertTrue(mainVm.loginWithPhone("+91", "7777766666", "Shop B", "", startFresh = true))
    awaitAsync {
      val id = mainVm.prefs.activeBusinessId.value
      id > 0L && id != shopAId && mainVm.repository.getBusinessByIdSync(id)?.name == "Shop B"
    }
    val shopBId = mainVm.prefs.activeBusinessId.value
    val summaryB = mainVm.repository.getDashboardSummary(shopBId).first()
    assertEquals(0, summaryB.totalCustomers)
    assertEquals(0.0, summaryB.totalCredit, 0.001)
    assertEquals(0.0, summaryB.totalExpenses, 0.001)

    // Returning to Account A restores Shop A ledger
    mainVm.logout()
    assertTrue(mainVm.loginWithPhone("+91", "9999988888", "", ""))
    awaitAsync { mainVm.prefs.activeBusinessId.value == shopAId }
    assertEquals(1, mainVm.repository.getCustomersWithBalance(shopAId).first().size)
  }

  @Test
  fun `test MainActivity launches cleanly without crash`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)
    assertFalse(activity.isFinishing)
    controller.pause().stop().destroy()
  }

  @Test
  fun `test CashbookCalculator arithmetic percent and edge cases`() {
    assertEquals(280.0, com.example.data.model.CashbookCalculator.evaluate("120+80×2") ?: -1.0, 0.001)
    assertEquals("280", com.example.data.model.CashbookCalculator.evaluateOrKeep("120+80×2"))
    assertEquals("110", com.example.data.model.CashbookCalculator.applyPercent("100+10"))
    assertEquals("90", com.example.data.model.CashbookCalculator.applyPercent("100-10"))
    assertEquals("25", com.example.data.model.CashbookCalculator.evaluateOrKeep("100÷4"))
    assertEquals("5÷0", com.example.data.model.CashbookCalculator.evaluateOrKeep("5÷0"))
    assertEquals(0.0, com.example.data.model.CashbookCalculator.evaluateToAmount("5÷0"), 0.001)
    assertEquals("12.5", com.example.data.model.CashbookCalculator.appendDecimal("12.5"))
  }

  @Test
  fun `test Suppliers Cashbook Items and Recycle Bin soft delete and restore`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = KhatabookRepository(db)
      val busId = repo.createBusiness("Wholesale Mart", "Rajesh", "9876543210", "", "")

      // Customer vs Supplier separation
      val custId = repo.addCustomer(busId, "Aman Customer", "9000011111", "", 200.0, "GET", "CUSTOMER")
      val supId = repo.addCustomer(busId, "Metro Wholesaler", "9000022222", "", 500.0, "GIVE", "SUPPLIER")

      val customers = repo.getCustomersWithBalance(busId).first()
      val suppliers = repo.getSuppliersWithBalance(busId).first()
      assertEquals(1, customers.size)
      assertEquals("Aman Customer", customers[0].name)
      assertEquals(1, suppliers.size)
      assertEquals("Metro Wholesaler", suppliers[0].name)
      assertEquals(-500.0, suppliers[0].netBalance, 0.001)

      // Cashbook Cash vs Online balance calculation
      repo.addCashbookEntry(
        businessId = busId,
        amount = 1000.0,
        type = com.example.data.model.CashbookType.IN,
        paymentMode = com.example.data.model.CashbookPaymentMode.CASH,
        note = "Morning sales"
      )
      val outEntryId = repo.addCashbookEntry(
        businessId = busId,
        amount = 300.0,
        type = com.example.data.model.CashbookType.OUT,
        paymentMode = com.example.data.model.CashbookPaymentMode.CASH,
        note = "Milk purchase"
      )
      repo.addCashbookEntry(
        businessId = busId,
        amount = 450.0,
        type = com.example.data.model.CashbookType.IN,
        paymentMode = com.example.data.model.CashbookPaymentMode.ONLINE,
        note = "UPI payment"
      )

      val cbSummary = repo.getCashbookSummary(busId).first()
      assertEquals(700.0, cbSummary.totalCashInHand, 0.001)
      assertEquals(450.0, cbSummary.totalOnline, 0.001)
      assertEquals(1150.0, cbSummary.totalBalance, 0.001)
      assertEquals(3, cbSummary.todayEntryCount)

      // Items CRUD
      val itemId = repo.addItem(busId, "Sugar 1kg", "kg", 48.0, 40.0, 25.0, 5.0)
      val items = repo.getItems(busId).first()
      assertEquals(1, items.size)
      assertEquals("Sugar 1kg", items[0].name)
      repo.updateItem(items[0].copy(stockQty = 30.0))
      assertEquals(30.0, repo.getItems(busId).first()[0].stockQty, 0.001)
      repo.deleteItem(itemId)
      assertTrue(repo.getItems(busId).first().isEmpty())

      // Soft-delete party and cashbook entry -> appears in Recycle Bin -> Restore restores both
      repo.deleteCustomer(custId)
      repo.deleteCashbookEntry(outEntryId)

      assertTrue(repo.getCustomersWithBalance(busId).first().isEmpty())
      val binItems = repo.getRecycleBinItems(busId).first()
      assertEquals(2, binItems.size)

      binItems.forEach { repo.restoreRecycleBinItem(it) }
      assertEquals(1, repo.getCustomersWithBalance(busId).first().size)
      assertTrue(repo.getRecycleBinItems(busId).first().isEmpty())
      assertEquals(700.0, repo.getCashbookSummary(busId).first().totalCashInHand, 0.001)
    } finally {
      db.close()
    }
  }

  @Test
  fun `test AppDatabase MIGRATION_2_3 upgrades v2 schema cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbName = "migration_test_v2_v3_${System.currentTimeMillis()}.db"
    val sqliteDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
      context.getDatabasePath(dbName).apply { parentFile?.mkdirs() },
      null
    )
    try {
      sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS businesses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, ownerName TEXT NOT NULL, phone TEXT NOT NULL, address TEXT NOT NULL, gstNumber TEXT NOT NULL, createdAt INTEGER NOT NULL)")
      sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS customers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, businessId INTEGER NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL, address TEXT NOT NULL, openingBalance REAL NOT NULL, openingBalanceType TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
      sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, businessId INTEGER NOT NULL, customerId INTEGER NOT NULL, amount REAL NOT NULL, type TEXT NOT NULL, paymentMethod TEXT NOT NULL, description TEXT NOT NULL, billNumber TEXT NOT NULL, transactionDate INTEGER NOT NULL, dueDate INTEGER, createdAt INTEGER NOT NULL)")

      val supportDb = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
        .create(
          androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
              override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {}
              override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        ).writableDatabase
      AppDatabase.MIGRATION_2_3.migrate(supportDb)

      val tablesCursor = supportDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('cashbook_entries', 'items')")
      val tableNames = mutableListOf<String>()
      while (tablesCursor.moveToNext()) tableNames.add(tablesCursor.getString(0))
      tablesCursor.close()
      assertTrue(tableNames.contains("cashbook_entries"))
      assertTrue(tableNames.contains("items"))
      supportDb.close()
    } finally {
      sqliteDb.close()
      context.deleteDatabase(dbName)
    }
  }

  @Test
  fun `test New Transaction Given and Received balance projection and Room update`() = runBlocking {
    val customer = CustomerWithBalance(
      id = 5L,
      businessId = 1L,
      name = "Mohan Kirana",
      phone = "9876501234",
      address = "",
      openingBalance = 0.0,
      openingBalanceType = "GET",
      totalCredit = 1000.0,
      totalPayment = 250.0,
      netBalance = 750.0,
      lastTransactionDate = 1000L,
      transactionCount = 2
    )

    // Given (Credit) increases receivable balance
    assertEquals(
      1250.0,
      com.example.ui.components.calculateProjectedCustomerBalance(
        selectedCustomer = customer,
        newAmount = 500.0,
        newType = com.example.data.model.TransactionType.CREDIT
      ),
      0.001
    )

    // Received (Payment) decreases receivable balance
    assertEquals(
      550.0,
      com.example.ui.components.calculateProjectedCustomerBalance(
        selectedCustomer = customer,
        newAmount = 200.0,
        newType = com.example.data.model.TransactionType.PAYMENT
      ),
      0.001
    )

    // Editing an existing Given (Credit) entry reverses prior 400 and applies Received 300 => 750 - 400 - 300 = 50
    val existing = TransactionEntity(
      id = 99L,
      businessId = 1L,
      customerId = 5L,
      amount = 400.0,
      type = "CREDIT",
      paymentMethod = "CASH",
      transactionDate = 1000L
    )
    assertEquals(
      50.0,
      com.example.ui.components.calculateProjectedCustomerBalance(
        selectedCustomer = customer,
        newAmount = 300.0,
        newType = com.example.data.model.TransactionType.PAYMENT,
        existingTransaction = existing
      ),
      0.001
    )

    // Verify end-to-end Room persistence through TransactionViewModel
    val app = ApplicationProvider.getApplicationContext<Application>()
    val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = KhatabookRepository(db)
      val prefs = PreferencesManager(app)
      val busId = repo.ensureDefaultBusiness()
      prefs.setActiveBusinessId(busId)
      val custId = repo.addCustomer(busId, "Mohan Kirana", "9876501234", "", 0.0, "GET")
      val txnVm = TransactionViewModel(app, repo, prefs)

      var creditSaved = false
      txnVm.addTransaction(
        customerId = custId,
        amount = 800.0,
        type = com.example.data.model.TransactionType.CREDIT,
        paymentMethod = "CASH",
        description = "Rice bags"
      ) { creditSaved = true }

      val deadline = System.currentTimeMillis() + 3000L
      while (!creditSaved && System.currentTimeMillis() < deadline) {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        kotlinx.coroutines.delay(20)
      }
      assertTrue(creditSaved)

      var paymentSaved = false
      txnVm.addTransaction(
        customerId = custId,
        amount = 300.0,
        type = com.example.data.model.TransactionType.PAYMENT,
        paymentMethod = "UPI",
        description = "Part payment"
      ) { paymentSaved = true }

      val deadline2 = System.currentTimeMillis() + 3000L
      while (!paymentSaved && System.currentTimeMillis() < deadline2) {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        kotlinx.coroutines.delay(20)
      }
      assertTrue(paymentSaved)

      val updatedCustomer = repo.getCustomerWithBalance(custId).first()
      assertNotNull(updatedCustomer)
      assertEquals(800.0, updatedCustomer!!.totalCredit, 0.001)
      assertEquals(300.0, updatedCustomer.totalPayment, 0.001)
      assertEquals(500.0, updatedCustomer.netBalance, 0.001)
    } finally {
      db.close()
    }
  }

  @Test
  fun `test calculateCurrentMonthExpenseSummary filters strictly by current month`() {
    val refCal = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 16, 14, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val refTime = refCal.timeInMillis

    val todayExpense = (refCal.clone() as Calendar).apply {
      set(Calendar.HOUR_OF_DAY, 9)
    }.timeInMillis
    val earlierThisMonth = (refCal.clone() as Calendar).apply {
      set(Calendar.DAY_OF_MONTH, 2)
    }.timeInMillis
    val previousMonth = (refCal.clone() as Calendar).apply {
      add(Calendar.MONTH, -1)
    }.timeInMillis
    val nextMonth = (refCal.clone() as Calendar).apply {
      add(Calendar.MONTH, 1)
    }.timeInMillis

    val expenses = listOf(
      ExpenseEntity(id = 1, businessId = 1, amount = 600.0, category = "RENT", expenseDate = earlierThisMonth),
      ExpenseEntity(id = 2, businessId = 1, amount = 400.0, category = "INVENTORY", expenseDate = todayExpense),
      ExpenseEntity(id = 3, businessId = 1, amount = 500.0, category = "ELECTRICITY", expenseDate = previousMonth),
      ExpenseEntity(id = 4, businessId = 1, amount = 900.0, category = "SALARY", expenseDate = nextMonth)
    )

    val summary = com.example.ui.screens.calculateCurrentMonthExpenseSummary(
      expenses = expenses,
      referenceTimeMillis = refTime
    )

    assertEquals(1000.0, summary.currentMonthTotal, 0.001)
    assertEquals(500.0, summary.previousMonthTotal, 0.001)
    assertEquals(400.0, summary.todayTotal, 0.001)
    assertEquals(2, summary.entryCount)
    assertEquals(2, summary.categoryShares.size)
    assertEquals("RENT", summary.categoryShares[0].categoryKey)
    assertEquals(0.6f, summary.categoryShares[0].percentage, 0.001f)
    assertEquals("INVENTORY", summary.categoryShares[1].categoryKey)
    assertEquals(0.4f, summary.categoryShares[1].percentage, 0.001f)
  }
}

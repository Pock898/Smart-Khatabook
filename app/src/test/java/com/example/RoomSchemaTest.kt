package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.BusinessEntity
import com.example.data.local.Customer
import com.example.data.local.CustomerEntity
import com.example.data.local.Expense
import com.example.data.local.ExpenseEntity
import com.example.data.local.Transaction
import com.example.data.local.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.KhatabookRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomSchemaTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: KhatabookRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = KhatabookRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `customer transaction and expense entities persist and compute balances`() = runBlocking {
        val businessId = database.businessDao().insertBusiness(
            BusinessEntity(name = "Test Kirana Store", ownerName = "Rajesh")
        )

        val customerId = database.customerDao().insertCustomer(
            Customer(
                businessId = businessId,
                name = "Anil Traders",
                phone = "+91 9876543210",
                address = "Main Market",
                openingBalance = 500.0,
                openingBalanceType = "GET"
            )
        )

        val now = System.currentTimeMillis()
        database.transactionDao().insertTransaction(
            Transaction(
                businessId = businessId,
                customerId = customerId,
                amount = 1500.0,
                type = TransactionType.CREDIT.value,
                paymentMethod = "CASH",
                description = "Grocery credit",
                transactionDate = now
            )
        )
        database.transactionDao().insertTransaction(
            TransactionEntity(
                businessId = businessId,
                customerId = customerId,
                amount = 600.0,
                type = TransactionType.PAYMENT.value,
                paymentMethod = "UPI",
                description = "Partial UPI payment",
                transactionDate = now + 1000L
            )
        )

        database.expenseDao().insertExpense(
            Expense(
                businessId = businessId,
                amount = 250.0,
                category = "ELECTRICITY",
                paymentMethod = "UPI",
                description = "Shop bill",
                expenseDate = now
            )
        )

        val customersWithBalance = repository.getCustomersWithBalance(businessId).first()
        assertEquals(1, customersWithBalance.size)
        // Opening 500 + Credit 1500 - Payment 600 = 1400
        assertEquals(1400.0, customersWithBalance.first().netBalance, 0.001)

        val expenses = repository.getExpenses(businessId).first()
        assertEquals(1, expenses.size)
        assertEquals(250.0, expenses.first().amount, 0.001)

        // ForeignKey.CASCADE removes transactions when customer is deleted
        database.customerDao().deleteCustomerById(customerId)
        val remainingTxns = database.transactionDao().getTransactionsByCustomer(customerId).first()
        assertTrue(remainingTxns.isEmpty())
    }

    @Test
    fun `customer transaction and expense dao crud operations succeed`() = runBlocking {
        val businessId = database.businessDao().insertBusiness(
            BusinessEntity(name = "CRUD Store")
        )
        val now = System.currentTimeMillis()

        // Customer CRUD
        val customerId = database.customerDao().insertCustomer(
            Customer(businessId = businessId, name = "Suresh", phone = "9876500001")
        )
        val insertedCustomer = database.customerDao().getCustomerByIdSync(customerId)!!
        assertEquals("Suresh", insertedCustomer.name)

        database.customerDao().updateCustomer(insertedCustomer.copy(name = "Suresh Sharma"))
        assertEquals("Suresh Sharma", database.customerDao().getCustomerByIdSync(customerId)?.name)

        val searched = database.customerDao().searchCustomers(businessId, "Sharma").first()
        assertEquals(1, searched.size)

        // Transaction CRUD
        val txnId = database.transactionDao().insertTransaction(
            Transaction(
                businessId = businessId,
                customerId = customerId,
                amount = 800.0,
                type = TransactionType.CREDIT.value,
                transactionDate = now
            )
        )
        val insertedTxn = database.transactionDao().getTransactionByIdSync(txnId)!!
        assertEquals(800.0, insertedTxn.amount, 0.001)

        database.transactionDao().updateTransaction(insertedTxn.copy(amount = 950.0))
        assertEquals(950.0, database.transactionDao().getTransactionByIdSync(txnId)!!.amount, 0.001)

        database.transactionDao().deleteTransactionById(txnId)
        assertTrue(database.transactionDao().getTransactionsByCustomer(customerId).first().isEmpty())

        // Expense CRUD
        val expenseId = database.expenseDao().insertExpense(
            Expense(
                businessId = businessId,
                amount = 300.0,
                category = "RENT",
                expenseDate = now
            )
        )
        val insertedExpense = database.expenseDao().getExpenseById(expenseId).first()!!
        assertEquals(300.0, insertedExpense.amount, 0.001)

        database.expenseDao().updateExpense(insertedExpense.copy(amount = 450.0))
        assertEquals(450.0, database.expenseDao().getExpenseById(expenseId).first()?.amount ?: 0.0, 0.001)

        database.expenseDao().deleteExpenseById(expenseId)
        assertTrue(database.expenseDao().getExpensesByBusiness(businessId).first().isEmpty())

        // Delete customer
        database.customerDao().deleteCustomerById(customerId)
        assertTrue(database.customerDao().getCustomersByBusiness(businessId).first().isEmpty())
    }
}

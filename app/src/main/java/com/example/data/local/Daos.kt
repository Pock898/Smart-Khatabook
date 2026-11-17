package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses ORDER BY createdAt ASC")
    fun getAllBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    fun getBusinessById(id: Long): Flow<BusinessEntity?>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessByIdSync(id: Long): BusinessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity): Long

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Delete
    suspend fun deleteBusiness(business: BusinessEntity)

    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun getBusinessCount(): Int

    @Query("SELECT * FROM businesses LIMIT 1")
    suspend fun getFirstBusiness(): BusinessEntity?
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getCustomersByBusiness(businessId: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND partyType = :partyType AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getPartiesByBusinessAndType(businessId: Long, partyType: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedCustomersByBusiness(businessId: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdSync(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND isDeleted = 0")
    suspend fun getAllCustomersSync(businessId: Long): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>): List<Long>

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :customerId")
    suspend fun softDeleteCustomer(customerId: Long, deletedAt: Long)

    @Query("UPDATE customers SET isDeleted = 0, deletedAt = NULL WHERE id = :customerId")
    suspend fun restoreCustomer(customerId: Long)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomerById(customerId: Long)

    @Query("DELETE FROM customers WHERE businessId = :businessId")
    suspend fun deleteCustomersByBusiness(businessId: Long)

    @Query("DELETE FROM customers WHERE businessId = :businessId AND isDeleted = 1")
    suspend fun purgeDeletedCustomers(businessId: Long)

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND isDeleted = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchCustomers(businessId: Long, query: String): Flow<List<CustomerEntity>>

    @Query("UPDATE customers SET updatedAt = :timestamp WHERE id = :customerId")
    suspend fun updateCustomerTimestamp(customerId: Long, timestamp: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY transactionDate DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY transactionDate DESC, id DESC")
    fun getTransactionsByCustomer(customerId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY transactionDate ASC, id ASC")
    suspend fun getTransactionsByCustomerAscSync(customerId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE businessId = :businessId AND isDeleted = 0 ORDER BY transactionDate DESC, id DESC")
    fun getTransactionsByBusiness(businessId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE businessId = :businessId AND isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedTransactionsByBusiness(businessId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE businessId = :businessId AND isDeleted = 0 ORDER BY transactionDate DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(businessId: Long, limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE businessId = :businessId AND isDeleted = 0 AND transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC, id DESC")
    fun getTransactionsBetween(businessId: Long, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionByIdSync(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE businessId = :businessId AND isDeleted = 0")
    suspend fun getAllTransactionsByBusinessSync(businessId: Long): List<TransactionEntity>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE businessId = :businessId AND isDeleted = 0 AND type = 'CREDIT'")
    fun getTotalCreditByBusiness(businessId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE businessId = :businessId AND isDeleted = 0 AND type = 'PAYMENT'")
    fun getTotalPaymentsByBusiness(businessId: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, deletedAt: Long)

    @Query("UPDATE transactions SET isDeleted = 1, deletedAt = :deletedAt WHERE customerId = :customerId AND isDeleted = 0")
    suspend fun softDeleteTransactionsByCustomer(customerId: Long, deletedAt: Long)

    @Query("UPDATE transactions SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreTransaction(id: Long)

    @Query("UPDATE transactions SET isDeleted = 0, deletedAt = NULL WHERE customerId = :customerId")
    suspend fun restoreTransactionsByCustomer(customerId: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsByCustomer(customerId: Long)

    @Query("DELETE FROM transactions WHERE businessId = :businessId AND isDeleted = 1")
    suspend fun purgeDeletedTransactions(businessId: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE businessId = :businessId ORDER BY expenseDate DESC, id DESC")
    fun getExpensesByBusiness(businessId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND category = :category ORDER BY expenseDate DESC, id DESC")
    fun getExpensesByCategory(businessId: Long, category: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND expenseDate BETWEEN :startDate AND :endDate ORDER BY expenseDate DESC, id DESC")
    fun getExpensesBetween(businessId: Long, startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseById(id: Long): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseByIdSync(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE businessId = :businessId")
    suspend fun getAllExpensesSync(businessId: Long): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE businessId = :businessId")
    fun getTotalExpensesByBusiness(businessId: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>): List<Long>

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses WHERE businessId = :businessId")
    suspend fun deleteExpensesByBusiness(businessId: Long)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE businessId = :businessId ORDER BY name ASC")
    fun getStaffByBusiness(businessId: Long): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE businessId = :businessId")
    suspend fun getAllStaffSync(businessId: Long): List<StaffEntity>

    @Query("SELECT * FROM staff WHERE id = :staffId LIMIT 1")
    fun getStaffById(staffId: Long): Flow<StaffEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Query("DELETE FROM staff WHERE id = :staffId")
    suspend fun deleteStaffById(staffId: Long)

    @Query("SELECT * FROM staff_attendance WHERE businessId = :businessId AND dateKey LIKE :monthPrefix || '%'")
    fun getAttendanceByBusinessMonth(businessId: Long, monthPrefix: String): Flow<List<StaffAttendanceEntity>>

    @Query("SELECT * FROM staff_attendance WHERE businessId = :businessId")
    suspend fun getAllAttendanceSync(businessId: Long): List<StaffAttendanceEntity>

    @Query("SELECT * FROM staff_attendance WHERE staffId = :staffId AND dateKey LIKE :monthPrefix || '%'")
    fun getAttendanceForStaffMonth(staffId: Long, monthPrefix: String): Flow<List<StaffAttendanceEntity>>

    @Query("SELECT * FROM staff_attendance WHERE staffId = :staffId AND dateKey = :dateKey LIMIT 1")
    suspend fun getAttendanceByStaffAndDateSync(staffId: Long, dateKey: String): StaffAttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(entry: StaffAttendanceEntity): Long

    @Query("DELETE FROM staff_attendance WHERE staffId = :staffId")
    suspend fun deleteAttendanceByStaff(staffId: Long)
}

@Dao
interface CashbookDao {
    @Query("SELECT * FROM cashbook_entries WHERE businessId = :businessId AND isDeleted = 0 ORDER BY entryDate DESC, id DESC")
    fun getEntriesByBusiness(businessId: Long): Flow<List<CashbookEntity>>

    @Query("SELECT * FROM cashbook_entries WHERE businessId = :businessId AND isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedEntriesByBusiness(businessId: Long): Flow<List<CashbookEntity>>

    @Query("SELECT * FROM cashbook_entries WHERE businessId = :businessId AND isDeleted = 0")
    suspend fun getAllEntriesSync(businessId: Long): List<CashbookEntity>

    @Query("SELECT * FROM cashbook_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryByIdSync(id: Long): CashbookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CashbookEntity): Long

    @Update
    suspend fun updateEntry(entry: CashbookEntity)

    @Query("UPDATE cashbook_entries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteEntry(id: Long, deletedAt: Long)

    @Query("UPDATE cashbook_entries SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreEntry(id: Long)

    @Query("DELETE FROM cashbook_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM cashbook_entries WHERE businessId = :businessId AND isDeleted = 1")
    suspend fun purgeDeletedEntries(businessId: Long)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE businessId = :businessId ORDER BY name ASC")
    fun getItemsByBusiness(businessId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE businessId = :businessId")
    suspend fun getAllItemsSync(businessId: Long): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: Long)
}


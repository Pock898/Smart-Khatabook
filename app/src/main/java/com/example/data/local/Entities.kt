package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val gstNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["businessId"]), Index(value = ["phone"])]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val businessId: Long,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val openingBalance: Double = 0.0,
    val openingBalanceType: String = "GET", // "GET" or "GIVE"
    val partyType: String = "CUSTOMER", // "CUSTOMER" or "SUPPLIER"
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["businessId"]),
        Index(value = ["transactionDate"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val businessId: Long,
    val customerId: Long,
    val amount: Double,
    val type: String, // "CREDIT" or "PAYMENT"
    val paymentMethod: String = "CASH",
    val description: String = "",
    val billNumber: String = "",
    val transactionDate: Long,
    val dueDate: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["businessId"]), Index(value = ["expenseDate"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val businessId: Long,
    val amount: Double,
    val category: String,
    val paymentMethod: String = "CASH",
    val description: String = "",
    val expenseDate: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cashbook_entries",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["businessId"]), Index(value = ["entryDate"])]
)
data class CashbookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val businessId: Long,
    val amount: Double,
    val type: String, // "IN" or "OUT"
    val paymentMode: String = "CASH", // "CASH" or "ONLINE"
    val note: String = "",
    val entryDate: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["businessId"])]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val businessId: Long,
    val name: String,
    val unit: String = "pcs",
    val salePrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val stockQty: Double = 0.0,
    val lowStockAlert: Double = 5.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "staff",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["businessId"])]
)
data class StaffEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val businessId: Long,
    val name: String,
    val phone: String = "",
    val role: String = "Staff",
    val salaryType: String = "MONTHLY", // "MONTHLY" or "DAILY"
    val salaryAmount: Double = 0.0,
    val overtimeRate: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "staff_attendance",
    foreignKeys = [
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["staffId", "dateKey"], unique = true),
        Index(value = ["businessId"]),
        Index(value = ["dateKey"])
    ]
)
data class StaffAttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val businessId: Long,
    val staffId: Long,
    val dateKey: String, // "yyyy-MM-dd"
    val status: String = "NONE", // "NONE", "P", "A", "OT", "OFF"
    val advanceAmount: Double = 0.0,
    val advanceNote: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

typealias Customer = CustomerEntity
typealias Transaction = TransactionEntity
typealias Expense = ExpenseEntity




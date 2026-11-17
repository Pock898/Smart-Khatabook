package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BusinessEntity::class,
        CustomerEntity::class,
        TransactionEntity::class,
        ExpenseEntity::class,
        CashbookEntity::class,
        ItemEntity::class,
        StaffEntity::class,
        StaffAttendanceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun cashbookDao(): CashbookDao
    abstract fun itemDao(): ItemDao
    abstract fun staffDao(): StaffDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN partyType TEXT NOT NULL DEFAULT 'CUSTOMER'")
                db.execSQL("ALTER TABLE customers ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE customers ADD COLUMN deletedAt INTEGER")

                db.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN deletedAt INTEGER")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cashbook_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        businessId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        paymentMode TEXT NOT NULL,
                        note TEXT NOT NULL,
                        entryDate INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(businessId) REFERENCES businesses(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cashbook_entries_businessId ON cashbook_entries(businessId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cashbook_entries_entryDate ON cashbook_entries(entryDate)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        businessId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        salePrice REAL NOT NULL,
                        purchasePrice REAL NOT NULL,
                        stockQty REAL NOT NULL,
                        lowStockAlert REAL NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(businessId) REFERENCES businesses(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_businessId ON items(businessId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "khatabook_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

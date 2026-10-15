package com.salimshivani.inventoryscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mobile_number TEXT,
                name TEXT NOT NULL,
                address TEXT,
                created_at TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_mobile_number ON accounts(mobile_number)")
        db.execSQL("ALTER TABLE transactions ADD COLUMN account_id INTEGER REFERENCES accounts(id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_account_id ON transactions(account_id)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE items ADD COLUMN price REAL NOT NULL DEFAULT 0")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
                direction TEXT NOT NULL,
                amount REAL NOT NULL,
                payment_date TEXT NOT NULL,
                note TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_account_id ON payments(account_id)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bills (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
                direction TEXT NOT NULL,
                total_amount REAL NOT NULL,
                status TEXT NOT NULL DEFAULT 'unpaid',
                bill_date TEXT NOT NULL,
                payment_id INTEGER REFERENCES payments(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_account_id ON bills(account_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_status ON bills(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_payment_id ON bills(payment_id)")
    }
}

@Database(
    entities = [Item::class, Transaction::class, Account::class, Bill::class, Payment::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun billDao(): BillDao
    abstract fun paymentDao(): PaymentDao

    suspend fun replaceAllData(
        items: List<Item>,
        transactions: List<Transaction>,
        accounts: List<Account>,
        bills: List<Bill>,
        payments: List<Payment>
    ) {
        withTransaction {
            billDao().deleteAll()
            transactionDao().deleteAll()
            paymentDao().deleteAll()
            itemDao().deleteAll()
            accountDao().deleteAll()
            accountDao().insertAll(accounts)
            itemDao().insertAll(items)
            paymentDao().insertAll(payments)
            transactionDao().insertAll(transactions)
            billDao().insertAll(bills)
        }
    }

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventory.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}

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
        db.execSQL("ALTER TABLE transactions ADD COLUMN account_id INTEGER REFERENCES accounts(id) ON DELETE SET NULL")
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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE items ADD COLUMN purchase_price REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE items ADD COLUMN mrp REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE items ADD COLUMN gst_percent REAL NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE bills ADD COLUMN subtotal REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE bills ADD COLUMN tax_amount REAL NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE transactions ADD COLUMN bill_id INTEGER REFERENCES bills(id) ON DELETE SET NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN rate REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN gst_percent REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN tax_amount REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN line_total REAL NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_bill_id ON transactions(bill_id)")
    }
}

// Rebuilds `transactions` with foreign-key ON DELETE actions that actually match the
// Transaction entity (account_id/bill_id -> SET NULL). Both were originally added via
// ALTER TABLE ... REFERENCES ... with no ON DELETE clause (SQLite defaults to NO ACTION),
// which made Room's post-migration schema validation fail and crash on every app start
// right after this table was first queried. SQLite can't alter a column's FK action in
// place, so this copies every row into a correctly-defined table instead — no data lost.
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transactions_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
                direction TEXT NOT NULL,
                quantity REAL NOT NULL,
                timestamp TEXT NOT NULL,
                note TEXT,
                account_id INTEGER REFERENCES accounts(id) ON DELETE SET NULL,
                bill_id INTEGER REFERENCES bills(id) ON DELETE SET NULL,
                rate REAL NOT NULL DEFAULT 0,
                gst_percent REAL NOT NULL DEFAULT 0,
                tax_amount REAL NOT NULL DEFAULT 0,
                line_total REAL NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO transactions_new
                (id, item_id, direction, quantity, timestamp, note, account_id, bill_id, rate, gst_percent, tax_amount, line_total)
            SELECT id, item_id, direction, quantity, timestamp, note, account_id, bill_id, rate, gst_percent, tax_amount, line_total
            FROM transactions
            """.trimIndent()
        )
        db.execSQL("DROP TABLE transactions")
        db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_item_id ON transactions(item_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions(timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_account_id ON transactions(account_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_bill_id ON transactions(bill_id)")
    }
}

@Database(
    entities = [Item::class, Transaction::class, Account::class, Bill::class, Payment::class],
    version = 5,
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
            billDao().insertAll(bills)
            transactionDao().insertAll(transactions)
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { instance = it }
            }
    }
}
